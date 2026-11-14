# Personal original AAPS 4.0 — implementation

Approved direction: Thomas said to proceed on 2026-11-14, keeping setup simple. His clarification concerns DynISF being displaced by long-term values, not Boost parameter automation.

Base: official 4.0.0, f77abd9b82b308e8fc330da0d7a31370fb4a0f49. Work in the supplied Android Studio checkout on feature/original-tho; no additional clone/worktree. Origin is Thomas' fork; upstream is official AAPS. Master remains the integrated line. Local builds/tests only; no emulator, signing or phone installation.

## Tasks

- [ ] Establish unmodified APS/Constraints host-test and fullDebug baseline with JDK21, one worker, 2 GiB heaps; preserve log outside source.
- [ ] Characterize the actual public SMB DynISF path with complete recent/history fixtures: stable TDD, recent decrease, recent increase, missing history, invalid numeric input. Expected 80/10/10 TDD for weighted-eight-hour/seven-day/one-day values. Observe failures before changing the existing 33/34/33 formula. Preserve upstream adjustment factor/profile multiplier and numeric rejection. Do not import Boost's profile-implied 35%/15% fallback thresholds or Boost-only pull-down. Record remaining upstream history requirements and distinguish confirmed code paths from unverified field symptoms.
- [ ] Port one-hour DST detection/suspension with spring and autumn 59/60/61-minute boundary tests; observe failure before changing production. Retain upstream running-mode API and warning behavior.
- [ ] Run affected full host suites and fullDebug build; add a concise customization/validation record and simple local build command. No daily Boost optimizer, ownership ledger, history sync or new UI.
- [ ] Review entire diff independently, resolve material findings, record local source/artifact identity and keep hardware acceptance separate. Commit focused changes; integrate locally after verification. Do not trigger hosted workflows.

## Review focus

Recent rising/falling TDD must reach the public DynISF calculation without replacement by a profile/history floor. Complete seven-day history remains required by official AAPS; changing evidence availability is separate from changing weights. Non-finite/zero dosing inputs must not become usable ISF. Adjustment factor and profile percentage must remain effective. DST spring/autumn boundaries must use elapsed time; suspension duration must match the detection window.

## Evidence and rulings

- Scope correction from Thomas: first see when 80/10/10 does not actually apply in original 4.0. Defer DST and speculative numerical-policy changes; finish the trace, targeted formula port and behavior tests first. Keep remaining migration candidates explicitly open.
- Baseline started on 2026-11-14 before product edits; log: workspace _codex/orig-baseline-2026-11-14.log.
- Baseline APS + Constraints: 346 tests, zero failures/errors/skips. FullDebug failed in mergeExtDexFullDebug with Java heap space at 2 GiB; no product code had changed. Retry APK packaging with a larger process-local heap, without editing upstream build files.
- The first targeted test attempt was blocked before compilation by upstream's allCommitted() check. Commit test specifications before running them; preserve the check rather than patching it out. This attempt is NOT a behavioral RED result.
- Ruling: use one short plan/ledger in docs/tho rather than additional process folders and task-runner scaffolding, following Thomas' simplicity requirement.
- Ruling: no speculative guard loosening. Official SMB does not have Boost's profile-implied TDD thresholds; importing them would introduce additional fallbacks Thomas specifically wants investigated. Characterize the existing public path first.
