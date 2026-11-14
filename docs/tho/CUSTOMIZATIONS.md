# Original AAPS — personal changes

Base: official 4.0.0 (`f77abd9b82b308e8fc330da0d7a31370fb4a0f49`). No Boost merge.

## Current focus: when 80/10/10 actually applies

Thomas narrowed the first step on 2026-11-14 to tracing and testing the 80/10/10 calculation in original AAPS. DST and other migration candidates are deferred, not abandoned. No changes to plausibility limits are authorized by assumption.

The official formula in `OpenAPSSMBPlugin.calculateRawDynIsf` uses weighted-eight-hour / seven-day / one-day TDD at 33/34/33. The personal change is 80/10/10 at that single production expression; adjustment factor and original profile percentage still multiply the result. No Boost pull-down or profile-implied 35%/15% guard is imported.

### Paths where a fresh blend does not become the returned ISF

1. Dynamic sensitivity is OFF: `calculateVariableIsf` returns null; `ProfileSealed.getIsfMgdl` uses the profile ISF.
2. A saved APS result already has nonzero variable sensitivity: the public ISF accessor returns it first. The database query selects the latest result in the preceding five minutes. It reuses a prior dynamic result, not a seven-day ISF average. This path does not prove a newly changed formula ran.
3. No saved result, but an eligible cache hit: public ISF lookup can return a cached dynamic value. The key combines a 30-minute time bucket with integer glucose; the timestamp must precede the current clock. This also reuses a prior dynamic value, not a long-term average.
4. Missing current glucose: public ISF lookup returns null.
5. Missing any TDD component: one-day and seven-day calculations require `allowMissingDays=false`; rolling 24h and both recent four-hour components must also exist. Public lookup returns null and its profile caller falls back. In the loop, missing TDD components disable dynamic mode for that run and select the ordinary SMB/autosens path.
6. No fresh glucose status from the SMB glucose calculator: raw calculation cannot produce variable sensitivity even when all TDD parts exist. The loop's existing invalid-input checks can reject the run. Do not confuse missing evidence with a plausible large ISF change.
7. Before gathering fresh loop data, `invoke` checks `profile.getIsfMgdl` against `HardLimits.LIMIT_ISF`, 2..1000 mg/dL/U. A saved/cached or newly calculated public ISF outside that absolute range stops the run before the direct raw calculation. This is an existing upstream absolute limit, not a check of the percentage difference from profile ISF.
8. The loop also explicitly rejects non-finite/non-positive inputs once its final input-validation block is reached. This does not cover every earlier numerical failure: a NaN raw calculation can throw during rounding before that block. Zero-total TDD and non-finite input behavior have not been changed or comprehensively tested here; zero **recent** delivery in the test still has positive day/week components. No general numerical-safety claim is made.

### Paths that change the result while retaining the blend

- Adjustment factor and profile percentage scale the blended TDD.
- After passing preliminary profile/ISF/pump checks, the main loop calls `calculateRawDynIsf` directly. The public ISF accessor's database/cache shortcut does not replace that fresh `TDD`/`variable_sens` pair, but an out-of-range public ISF can prevent the loop from reaching it.
- `DetermineBasalSMB` uses `variable_sens` for dynamic sensitivity and uses the same `profile.TDD` in its predictions and `future_sens`. Future dosing sensitivity can use current/predicted glucose, so its numeric ISF can differ while 80/10/10 still applies.
- Optional TDD/carbohydrate sensitivity adjustment and temporary targets affect basal/targets. Ordinary dose constraints can limit an action even with dynamic mode active. They do not establish that the TDD mixture was overwritten.

### Plausibility distinction

The separate Boost engine can reject seven-day TDD below 35% or adjusted blended TDD below 15% of `1800/profileISF` and keep profile sensitivity. Original AAPS SMB has no such relative thresholds. We are not introducing them. No patient log was supplied that establishes which branch caused Thomas' past observations; code-path findings are not a retrospective diagnosis.

### Verification

Tests exercise the real public `OpenAPSSMBPlugin.getIsfMgdl` with controlled history/glucose inputs, rather than a copied blend helper. Stable history, recent changes in both directions, unequal day/week history, zero recent delivery, adjustment factor, profile percentage, disabled mode, missing week/glucose, database/cache reuse and the actual loop's early stop on stored ISF1001 are covered. Six expected formula failures preceded the production change. Final affected suites: APS359 + Constraints123 = 482 tests, zero failures/errors/skips; unchanged Constraints task was up-to-date on the final run. Independent source review completed; its coverage/documentation findings were addressed.

Local fullDebug APK built successfully using JDK21, one worker, 3 GiB Gradle and 2 GiB Kotlin heaps (2 GiB Gradle failed at D8). No upstream build settings were edited. APK: `app/build/outputs/apk/full/debug/app-full-debug.apk`, version4.0.0, package `info.nightscout.androidaps`. Built commit: `2eb46ee717c6a7fb3a8ba309445210a7a4be7c75`; subsequent commits only add tests/documentation. SHA256: `3473229C21CE325EC361CBE3AF26948789B045ED47A24B145FB173FF4C2B010B`. Debug candidate only; no signing-key migration, installation or hardware acceptance performed.

Reproduce from a clean, committed checkout in PowerShell (run tests and build sequentially; do not run Android Studio builds alongside):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot'
$env:ANDROID_HOME = 'C:\Users\linus\AppData\Local\Android\Sdk'
$env:ANDROID_USER_HOME = 'C:\Users\linus\.android'
Remove-Item Env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue
$thoArgs = @('--no-parallel', '--max-workers=1', '-Dorg.gradle.jvmargs=-Xmx3g -XX:+UseParallelGC -Xss4m', '-Pkotlin.daemon.jvmargs=-Xmx2g', '--console=plain')
.\gradlew.bat :plugins:aps:testAndroidHostTest :plugins:constraints:testAndroidHostTest @thoArgs
if ($LASTEXITCODE -ne 0) { throw 'Host tests failed' }
.\gradlew.bat :app:assembleFullDebug @thoArgs
if ($LASTEXITCODE -ne 0) { throw 'Debug build failed' }
```
