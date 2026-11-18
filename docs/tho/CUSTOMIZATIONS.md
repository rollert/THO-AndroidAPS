# Original AAPS — personal changes

## Dana-RS pairing recovery — 2026-11-18

An empty or unknown pump-check reply ends the current Bluetooth attempt and preserves the saved pairing. The normal reconnect path still requires the pump handshake to succeed. Recognized pump alarms and manual pairing removal retain their existing behavior. The failure and follow-up patch relate to [upstream issue #5571](https://github.com/nightscout/AndroidAPS/issues/5571).

The regression test reproduced automatic key deletion before the fix. It now verifies three failed attempts, transport disconnects and preservation of both the app pairing and Android bond. All 277 Dana-RS JVM tests passed. The local fullDebug APK and tag `tho-aaps-4.0.0-dana-rs-rc1` identify built commit `049b2e2f8a3af89218145b981b140bc105efad40`. Exported APK SHA256 `D67CD8F5F8D04D5F8526C06AEA872956DFF5FE3283F227F11098B6207035C4C9`. Bluetooth recovery on physical hardware remains unverified.

Base: official 4.0.0 (`f77abd9b82b308e8fc330da0d7a31370fb4a0f49`). No Boost merge.

## Complete feature mapping

Thomas authorized completion of all remaining applicable features on 2026-11-14. The Boost register THO01..09 and complete personal net diff against Boost4.0.0-6.3.2 were reviewed. Decisions for original AAPS:

| ID | Original AAPS decision |
|---|---|
| THO-01 | Retain 80/10/10 in actual SMB DynISF and align the legacy device-test JS comparator. No Boost-only pull-down. |
| THO-02 | Do not import Boost's profile-implied 35%/15% fallback thresholds, per the DynISF investigation. Keep original numerical/absolute-limit behavior unchanged. |
| THO-03 | Daily Boost auto-config and its sample/evidence schedule are excluded. No corresponding feature is needed in original AAPS. |
| THO-04 | Boost parameter Auto/manual ownership, persisted ledger and settings switches are excluded with auto-config. |
| THO-05 | Retain one-hour DST detection and a 60-minute suspension from detection. Spring/autumn 59/60/61-minute boundaries and actual running-mode command are tested; DST-capable pumps remain exempt. Notification uses a personal EN/DE string so other locales fall back to correct English instead of upstream three-hour wording. This is not a guarantee that suspension ends exactly one hour after the transition when detection happens late. |
| THO-06 | Exclude Boost history sync and its four-day window. Original DynISF still requires complete one-/seven-day TDD plus the recent windows; no implicit relaxation of data requirements. |
| THO-07 | Reuse the local workspace build helper with a dedicated original-AAPS profile and artifact folder. Retain upstream build settings and UI; no obsolete plugin or historical patch is revived. |
| THO-08 | Boost daily tightening/confirmation changes are excluded with auto-config. |
| THO-09 | Cap-band replay investigation changed no production bands and supplies no feature for original AAPS. |

The older 26-file personal register is accounted for by these IDs, including tests, resources, keys, history logic and documentation. Inactive historical patches, intermediate blends/cadences and unimplemented adaptive/MAX-ISF ideas are not additional missing features. Release signing and real Android/Bluetooth acceptance remain separate from source integration; no emulators or hosted builds.

## When 80/10/10 actually applies

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

Tests exercise the real public `OpenAPSSMBPlugin.getIsfMgdl` with controlled history/glucose inputs, rather than a copied blend helper. Stable history, recent changes in both directions, unequal day/week history, zero recent delivery, adjustment factor, profile percentage, disabled mode, missing week/glucose, database/cache reuse and the actual loop's early stop on stored ISF1001 are covered. Six expected formula failures preceded the production change. Initial DynISF stage: APS359 + Constraints123 =482 tests, zero failures/errors/skips. Independent source review completed; its coverage/documentation findings were addressed.

The initial DynISF-only fullDebug APK built successfully using JDK21, one worker, 3 GiB Gradle and 2 GiB Kotlin heaps (2 GiB Gradle failed at D8). Historical built commit: `2eb46ee717c6a7fb3a8ba309445210a7a4be7c75`; SHA256 `3473229C21CE325EC361CBE3AF26948789B045ED47A24B145FB173FF4C2B010B`. This predates the completed DST port. No upstream build settings were edited.

Final combined candidate: `6720b7e29d55ae39d9b9d9ec3874061ee603f228`, local tag `tho-aaps-4.0.0-rc1`. APS359 + Constraints127 = **486 tests**, zero failures/errors/skips, including all four new DST tests. Final tests after warning-resource changes succeeded; instrumented test sources (including the aligned comparator) compiled earlier in the same completion branch, without device execution. Independent final review found no open code issue. The comparator is aligned for TDD weights; its pre-existing omission of the original-profile percentage multiplier is not changed or represented as full parity.

The actual helper fullDebug build/export succeeded in39s and restored all five environment variables. APK `C:\Users\linus\Desktop\HEALTH\LOOP\AAPS\ORIG\AAPS-ORIG_AAPS-ORIG-THO_full_debug_2026-11-14_6720b7e29d.apk`, SHA256 `F2611E798D9140B5361BCCFF4A20361B6C1ACCA840CE15EB2CF54E19DC97D9CF`. Version4.0.0, package `info.nightscout.androidaps`; sidecar JSON and checksum identify the build. Final documentation-only commits do not alter application source. This completes the applicable source features; release signing, personal settings/import and real Android/Bluetooth acceptance remain separate. Nothing was pushed or installed during this integration.

Default build/export from the AAPS workspace root:

```powershell
& .\_codex\tooling\Build-AapsArtifact.ps1 -ZoneName AAPS-ORIG -RepoName AAPS-ORIG-THO
```

The existing helper selects this repo, restores process environment, builds fullDebug by default and exports date+commit filenames to `C:\Users\linus\Desktop\HEALTH\LOOP\AAPS\ORIG`. Release builds remain blocked. Tests can use the explicit command below from this repo.

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

## Signed release and session closeout — 2026-11-14

Thomas built and signed `app/full/release/app-full-release.apk` from `8143ef4bd053aaf50879374863326a747370de5d` (embedded DEX build marker independently checked). Local reference tag: `tho-aaps-4.0.0-signed1`. This source has the same application behavior as the tested debug candidate; later differences were documentation/tests. Codex performed no release signing or phone operation.

The signed release is149424147bytes, SHA256 `79FFDD91E463DC561C224A0CF4A44CAD77FD391DD3DE4B04472C38E4ABE22737`. Signature verification passed; its certificate matches Thomas' working signed Boost reference. Original release is smaller than Boost release153660107bytes; the earlier approximately250MB original artifact was a debug build. These packaging checks do not prove runtime behavior.

Thomas reports successful startup after independently deleting installation data and reinstalling. He considers the prior Boost installation the likely cause of the earlier first-start crash. No crash trace proves the precise cause; an in-place Boost-to-original migration is not validated by a clean-data start. Do not turn data deletion into a routine update step. This records initial user-reported operation, not comprehensive Bluetooth/pump or clinical acceptance.

The unchanged signed APK, JSON provenance and SHA256 sidecar are preserved under `C:\Users\linus\Desktop\HEALTH\LOOP\AAPS\ORIG\AAPS-ORIG-THO_4.0.0_signed_2026-11-14_8143ef4bd0.apk`. Original APK and signing material remain untouched. Tests/builds remain exclusively local; repository-wide GitHub Actions was explicitly disabled before the closeout push to preserve that policy. Source publication targets `rollert/THO-AndroidAPS`, branch `master`, with candidate and signed-reference tags; remote refs are verified at session closeout. No APK or private configuration is committed to Git.
