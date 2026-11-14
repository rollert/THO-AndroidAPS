package app.aaps.plugins.constraints.dstHelper

import app.aaps.core.data.model.RM
import app.aaps.core.data.ue.Action
import app.aaps.core.data.ue.Sources
import app.aaps.core.interfaces.aps.Loop
import app.aaps.core.interfaces.pump.Pump
import app.aaps.shared.tests.TestBaseWithProfile
import com.google.common.truth.Truth.assertWithMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.Calendar
import java.util.TimeZone

class ThoDstWindowTest : TestBaseWithProfile() {
    @Mock lateinit var loop: Loop
    @Mock lateinit var pump: Pump
    private lateinit var plugin: DstHelperPlugin

    @BeforeEach fun prepare() {
        plugin = DstHelperPlugin(aapsLogger, rh, preferences, activePlugin, notificationManager, loop, profileFunction)
    }

    private fun check(timestamp: String, expected: Boolean) {
        val now = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))
        now.timeInMillis = Instant.parse(timestamp).toEpochMilli()
        assertWithMessage(timestamp).that(plugin.wasDST(now)).isEqualTo(expected)
    }

    @Test fun `spring detection ends at sixty elapsed minutes`() {
        check("2026-03-29T00:59:59Z", false)
        check("2026-03-29T01:59:00Z", true)
        check("2026-03-29T01:59:59Z", true)
        check("2026-03-29T02:00:00Z", false)
        check("2026-03-29T02:01:00Z", false)
    }

    @Test fun `autumn detection ends at sixty elapsed minutes across the repeated hour`() {
        check("2026-10-25T00:59:59Z", false)
        check("2026-10-25T01:59:00Z", true)
        check("2026-10-25T01:59:59Z", true)
        check("2026-10-25T02:00:00Z", false)
        check("2026-10-25T02:01:00Z", false)
    }

    @Test fun `DST check requests sixty minutes of suspension from detection`() = runBlocking {
        whenever(activePlugin.activePump).thenReturn(pump)
        whenever(pump.canHandleDST()).thenReturn(false)
        whenever(loop.runningMode()).thenReturn(RM.Mode.CLOSED_LOOP)
        whenever(profileFunction.getProfile()).thenReturn(effectiveProfile)
        val duringDstWindow = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).apply {
            timeInMillis = Instant.parse("2026-03-29T01:30:00Z").toEpochMilli()
        }
        mockStatic(Calendar::class.java).use { clock ->
            clock.`when`<Calendar> { Calendar.getInstance() }.thenAnswer { duringDstWindow.clone() as Calendar }
            plugin.dstCheck()
        }
        verify(loop).handleRunningModeChange(
            newRM = RM.Mode.SUSPENDED_BY_DST, durationInMinutes = 60,
            action = Action.SUSPEND, source = Sources.Aaps, profile = effectiveProfile
        )
    }

    @Test fun `pumps handling DST are exempt from suspension`() {
        whenever(activePlugin.activePump).thenReturn(pump)
        whenever(pump.canHandleDST()).thenReturn(true)
        plugin.dstCheck()
        verifyNoInteractions(loop)
    }
}
