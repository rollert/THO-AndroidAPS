package app.aaps.plugins.aps.openAPSSMB

import androidx.collection.LongSparseArray
import app.aaps.core.data.aps.AverageTDD
import app.aaps.core.data.model.TDD
import app.aaps.core.data.plugin.PluginType
import app.aaps.core.interfaces.aps.APSResult
import app.aaps.core.interfaces.aps.GlucoseStatusSMB
import app.aaps.core.interfaces.bgQualityCheck.BgQualityCheck
import app.aaps.core.interfaces.constraints.ConstraintsChecker
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.iob.GlucoseStatusProvider
import app.aaps.core.interfaces.profiling.Profiler
import app.aaps.core.interfaces.stats.TddCalculator
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.IntKey
import app.aaps.core.objects.profile.ProfileSealed
import app.aaps.plugins.aps.ApsStrings
import app.aaps.shared.tests.TestBaseWithProfile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class OpenAPSSMBPluginTest : TestBaseWithProfile() {

    @Mock lateinit var constraintChecker: ConstraintsChecker
    @Mock lateinit var persistenceLayer: PersistenceLayer
    @Mock lateinit var glucoseStatusProvider: GlucoseStatusProvider
    @Mock lateinit var determineBasalSMB: DetermineBasalSMB
    @Mock lateinit var bgQualityCheck: BgQualityCheck
    @Mock lateinit var tddCalculator: TddCalculator
    @Mock lateinit var profiler: Profiler
    @Mock lateinit var glucoseCalculator: GlucoseStatusCalculatorSMB
    @Mock lateinit var previousResult: APSResult
    @Mock lateinit var calculationClock: DateUtil
    private lateinit var openAPSSMBPlugin: OpenAPSSMBPlugin

    @BeforeEach fun prepare() {
        var tick = 1_794_000_000_000L
        whenever(calculationClock.now()).thenAnswer { tick++ }
        openAPSSMBPlugin = OpenAPSSMBPlugin(
            aapsLogger, rxBus, constraintChecker, rh, profileFunction, profileUtil, config, activePlugin,
            iobCobCalculator, hardLimits, preferences, calculationClock, processedTbrEbData, persistenceLayer, glucoseStatusProvider,
            tddCalculator, bgQualityCheck, notificationManager, determineBasalSMB, profiler, glucoseCalculator, { apsResultProvider() }, ch,
            fabricPrivacy
        )
    }

    /** See the note in `OpenAPSAMAPluginTest`: construction is the coverage this class still carries. */
    @Test
    fun `the plugin constructs and is an APS`() {
        assertThat(openAPSSMBPlugin.getType()).isEqualTo(PluginType.APS)
    }

    // Exercise the public calculation, including history eligibility and database reuse.
    // At BG 100 and insulin peak 60 minutes the divisor is 65; expected ISFs are hand calculated.
    private fun history(recentFourHours: Double = 8.0, sevenDay: Double = 48.0, oneDay: Double = 48.0) = runBlocking {
        whenever(preferences.get(BooleanKey.ApsUseDynamicSensitivity)).thenReturn(true)
        whenever(preferences.get(IntKey.ApsDynIsfAdjustmentFactor)).thenReturn(100)
        val glucose = GlucoseStatusSMB(glucose = 100.0)
        whenever(glucoseStatusProvider.glucoseStatusData).thenReturn(glucose)
        whenever(glucoseCalculator.getGlucoseStatusData(false)).thenReturn(glucose)
        val day = TDD(timestamp = 0, totalAmount = oneDay)
        val week = TDD(timestamp = 0, totalAmount = sevenDay)
        val dayData = LongSparseArray<TDD>().apply { put(0, day) }
        val weekData = LongSparseArray<TDD>().apply { put(0, week) }
        whenever(tddCalculator.calculate(1, allowMissingDays = false)).thenReturn(dayData)
        whenever(tddCalculator.calculate(7, allowMissingDays = false)).thenReturn(weekData)
        whenever(tddCalculator.averageTDD(dayData)).thenReturn(AverageTDD(day, false))
        whenever(tddCalculator.averageTDD(weekData)).thenReturn(AverageTDD(week, false))
        whenever(tddCalculator.calculateDaily(-24, 0)).thenReturn(day)
        whenever(tddCalculator.calculateDaily(-4, 0)).thenReturn(TDD(timestamp = 0, totalAmount = recentFourHours))
        whenever(tddCalculator.calculateDaily(-8, -4)).thenReturn(TDD(timestamp = 0, totalAmount = recentFourHours))
    }

    @Test fun `stable delivery keeps the same dynamic ISF`() {
        history()
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isEqualTo(40.3)
    }

    @Test fun `recent delivery decrease raises ISF despite the higher long term history`() {
        history(recentFourHours = 2.0)
        // Weighted recent TDD 12, week/day 48 => blended TDD 19.2, ISF 100.6.
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isEqualTo(100.6)
    }

    @Test fun `recent delivery increase lowers ISF despite the lower long term history`() {
        history(recentFourHours = 16.0)
        // Weighted recent TDD 96, week/day 48 => blended TDD 86.4, ISF 22.4.
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isEqualTo(22.4)
    }

    @Test fun `different weekly and daily history each retain ten percent of the blend`() {
        history(recentFourHours = 2.0, sevenDay = 60.0, oneDay = 24.0)
        // Weighted recent TDD 12, week 60, day 24 => TDD 18, ISF 107.3.
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isEqualTo(107.3)
    }

    @Test fun `zero recent delivery still uses the blend without a profile implied floor`() {
        history(recentFourHours = 0.0)
        // The two historical components still provide TDD 9.6.
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isEqualTo(201.3)
    }

    @Test fun `adjustment factor scales the blend`() {
        history(recentFourHours = 2.0)
        whenever(preferences.get(IntKey.ApsDynIsfAdjustmentFactor)).thenReturn(200)
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isEqualTo(50.3)
    }

    @Test fun `profile percentage scales the blend`() {
        history(recentFourHours = 2.0)
        val doubledProfile = ProfileSealed.EPS(effectiveProfileSwitch.copy(originalPercentage = 200), activePlugin)
        assertThat(openAPSSMBPlugin.getIsfMgdl(doubledProfile, "test")).isEqualTo(50.3)
    }

    @Test fun `missing complete week returns null for the caller to use its fallback`() = runBlocking {
        history()
        whenever(tddCalculator.calculate(7, allowMissingDays = false)).thenReturn(null)
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isNull()
    }

    @Test fun `missing current glucose prevents a new dynamic calculation`() {
        history()
        whenever(glucoseStatusProvider.glucoseStatusData).thenReturn(null)
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isNull()
    }

    @Test fun `disabled DynISF returns null even with complete history`() {
        history()
        whenever(preferences.get(BooleanKey.ApsUseDynamicSensitivity)).thenReturn(false)
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isNull()
    }

    @Test fun `stored recent APS sensitivity is reused before calculating a fresh blend`() = runBlocking {
        history(recentFourHours = 2.0)
        whenever(previousResult.variableSens).thenReturn(77.0)
        whenever(persistenceLayer.getApsResultCloseTo(any())).thenReturn(previousResult)
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isEqualTo(77.0)
    }

    @Test fun `same glucose and time bucket reuses cached ISF despite changed recent delivery`() {
        history(recentFourHours = 2.0)
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isEqualTo(100.6)
        history(recentFourHours = 16.0)
        assertThat(openAPSSMBPlugin.getIsfMgdl(effectiveProfile, "test")).isEqualTo(100.6)
    }

    @Test fun `stored out of range ISF stops the loop before fresh TDD calculation`() = runBlocking {
        history()
        doReturn("OpenAPS SMB").whenever(rh).gs(ApsStrings.openapssmb)
        openAPSSMBPlugin.setPluginEnabled(PluginType.APS, true)
        whenever(activePlugin.activeAPS).thenReturn(openAPSSMBPlugin)
        val runningProfile = ProfileSealed.EPS(effectiveProfileSwitch, activePlugin)
        whenever(profileFunction.getProfile()).thenReturn(runningProfile)
        whenever(previousResult.variableSens).thenReturn(1001.0)
        whenever(persistenceLayer.getApsResultCloseTo(any())).thenReturn(previousResult)

        openAPSSMBPlugin.invoke("test", tempBasalFallback = false)

        // The real public lookup reaches the stored value; the real hard-limit check aborts
        // before the loop reads fresh history. This is an existing upstream limit, not a new one.
        verify(persistenceLayer).getApsResultCloseTo(any())
        verify(tddCalculator, never()).calculate(7, allowMissingDays = false)
        assertThat(openAPSSMBPlugin.lastAPSResult).isNull()
    }
}
