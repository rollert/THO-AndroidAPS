# Personal original AAPS 4.0 — implementation

Approved direction: Thomas said to proceed on 2026-11-14, keeping setup simple. His clarification concerns DynISF being displaced by long-term values, not Boost parameter automation.

Base: official 4.0.0, f77abd9b82b308e8fc330da0d7a31370fb4a0f49. Work in the supplied Android Studio checkout on feature/original-tho; no additional clone/worktree. Origin is Thomas' fork; upstream is official AAPS. Master remains the integrated line. Local builds/tests only; no emulator, signing or phone installation.

## Tasks

- [x] Establish unmodified host-test baseline; diagnose the unmodified APK build's 2 GiB heap failure. Candidate APK succeeds with process-local 3 GiB, no upstream build-file change.
- [x] Characterize the public SMB DynISF path and actual loop pre-check: stable TDD, recent decrease/increase, unequal day/week history, zero recent delivery, missing history/glucose, adjustment/profile scaling, disabled mode, DB/cache reuse and absolute ISF rejection. Observe the six expected formula failures before changing 33/34/33 to 80/10/10. Do not import Boost guards or pull-down.
- [x] Run affected full host suites and fullDebug build; record commands and source/artifact identity. No Boost optimizer, ownership ledger, history sync or new UI.
- [x] Independently review the production diff, fix coverage/documentation findings and verify the resulting tests. Changes remain local; no hosted workflow triggered.
- [ ] Deferred by Thomas' narrowed focus: assess/port one-hour DST behavior separately; do not silently count it as completed. Comprehensive pre-existing non-finite/zero-total handling and real device acceptance are also outside this step.

## Review focus

Recent rising/falling TDD must reach the public DynISF calculation without a newly imported profile/history floor. Complete seven-day history remains required by official AAPS; changing evidence availability is separate from changing weights. Preserve existing numerical guards without claiming comprehensive coverage of upstream invalid-input behavior. Adjustment factor and profile percentage must remain effective. Trace pre-calculation absolute limits as well as later dynamic sensitivity calculations.

## Evidence and rulings

- Scope correction from Thomas: first see when 80/10/10 does not actually apply in original 4.0. Defer DST and speculative numerical-policy changes; finish the trace, targeted formula port and behavior tests first. Keep remaining migration candidates explicitly open.
- Baseline started on 2026-11-14 before product edits; log: workspace _codex/orig-baseline-2026-11-14.log.
- Early baseline report snapshot: 346 APS tests, zero failures/errors/skips; Constraints also completed before packaging, but the early aggregate count was incomplete. FullDebug failed in mergeExtDexFullDebug with Java heap space at 2 GiB; no product code had changed. Retry APK packaging with a larger process-local heap, without editing upstream build files.
- The first targeted test attempt was blocked before compilation by upstream's allCommitted() check. Commit test specifications before running them; preserve the check rather than patching it out. This attempt is NOT a behavioral RED result.
- Committed test run: 12 tests executed, 6 expected failures against 33/34/33, including lower/higher recent delivery and adjustment/profile scaling. Stable history and existing fallbacks passed. Log: _codex/orig-dynisf-red-committed-2026-11-14.log. Changed only the production weights to 80/10/10; no guard/cache/history/dose-limit policy changed.
- First GREEN: APS 357 + Constraints 123 = 480 tests, zero failures/errors/skips; log _codex/orig-dynisf-green-2026-11-14.log. Source commit 2eb46ee717. APK retry uses 3 GiB Gradle heap and otherwise the same local command.
- Independent read-only review approved the production expression, with coverage/documentation findings. Re-graded the omitted pre-calculation absolute ISF check and blanket numerical-validation wording as material to this investigation; corrected both. Added unequal-week/day coverage to distinguish 80/10/10 from 80/20/0, plus characterization of the actual invoke early return on stored ISF 1001. These characterize existing behavior; no production fix or synthetic RED claim is needed for the additional tests. No numerical-limit policy changed. Clinical/patient benefit, hardware acceptance and DST remain outside this narrowed step.
- The added invoke characterization initially failed before reaching the guard because enabling the plugin needs its display-name resource in the test fixture. Stubbed that resource only; no production change. The unequal-history test passed. FullDebug succeeded at 3 GiB in 4m43s, build log _codex/orig-dynisf-build-2026-11-14.log; APK product source is 2eb46ee717, subsequent changes only tests/docs.
- Final verification: APS359 + Constraints123 = 482 tests, zero failures/errors/skips. APS executed successfully in the final fixed run; unchanged Constraints reports remain from the earlier successful full run, final task up-to-date. The stored ISF1001 test reaches the actual public lookup in invoke and proves no fresh seven-day query happens afterward. Log _codex/orig-dynisf-final-tests-fixed-2026-11-14.log. No unresolved review finding in this scoped change; hardware/clinical claims declined and not asserted.
- Ruling: use one short plan/ledger in docs/tho rather than additional process folders and task-runner scaffolding, following Thomas' simplicity requirement.
- Ruling: no speculative guard loosening. Official SMB does not have Boost's profile-implied TDD thresholds; importing them would introduce additional fallbacks Thomas specifically wants investigated. Characterize the existing public path first.

## Completion — authorized 2026-11-14

Thomas requested all remaining applicable features. Base dcd573773c; branch feature/complete-original-port in the supplied checkout. Full Boost net diff against 4.0.0-boost-6.3.2 and the nine customization IDs were checked; no additional personal behavior beyond the register was found.

- [ ] Add spring/autumn 59/60/61-minute tests against actual DstHelperPlugin.wasDST; commit and run before changing production. Expected: fail at 60 minutes with upstream's three-hour window.
- [ ] Change only DISABLE_TIME_FRAME_HOURS from -3 to -1 and matching documentation/legacy expectation. The same constant feeds running-mode suspension duration. Verify new boundary tests, then full Constraints suite.
- [ ] Map THO01..09 explicitly for original AAPS. Keep 80/10/10 without Boost pull-down, omit relative profile floors per prior decision, omit Boost auto-config/ownership/history/replay behavior. No new dosing policies or UI.
- [ ] Add the original repo's local build profile using verified JDK21/3 GiB Gradle/2 GiB Kotlin/one-worker settings, and register its existing path in workspace metadata. Export debug artifacts under Desktop/HEALTH/LOOP/AAPS/ORIG. Reuse the existing helper, no new build framework.
- [ ] Run full affected local tests and helper build; independently review the final diff. Resolve material findings, pin the built source and hash, then integrate locally without rewriting history. No hosted build, release signing, emulator or phone installation.

Review focus: spring/autumn elapsed-time boundaries; suspension duration and detection must use the same constant; pumps that handle DST remain exempt; old warning behavior retained. Existing DynISF tests must stay green and no Boost-specific policy should enter original AAPS. Build profile must select the original repo and restore process environment.

- RED evidence: both boundary tests failed at sixty elapsed minutes; full test class ran 4 tests, 3 failed as expected (spring/autumn plus actual command duration180 instead of60), pump exemption passed. Logs _codex/orig-dst-red-2026-11-14.log and orig-dst-full-red-2026-11-14.log. Intermediate fixture attempts used the old Pump type and initially returned Boolean from a JUnit method; both corrected before counting RED. No product changes preceded the valid RED results.
- Completion audit also found the legacy device-test JS comparator still using 33/34/33, an already-listed THO01 support file. Align its weights with production; compile the instrumented test source if its upstream task supports it, without launching a device/emulator.
- GREEN: combined APS/Constraints task succeeded at 27ca0ec5c1; APS359 + Constraints127 = 486 tests, zero failures/errors/skips. New DST class has all four tests passing. Log _codex/orig-complete-tests-2026-11-14.log. Local build profile and managed-zone/repo inventory entries added only for this existing original checkout; all three JSON files parse successfully.
- Instrumented test source compilation succeeded at bef0ce105a in29s; log _codex/orig-device-test-compile-2026-11-14.log. No device test was executed. Additional resource correction: keep upstream translated three-hour key intact and add a personal EN/DE one-hour/60-minute warning key; other locales receive correct English fallback. This limits merge surface and avoids shipping a knowingly incorrect duration message.
