# Thomas' personal original AAPS

Follow the parent AAPS workspace instructions. Keep this variant close to official AAPS and changes small.

- Repository: `AAPS-ORIG/AAPS-ORIG-THO`. Check root, branch and dirty state before edits/builds.
- `origin`: `rollert/THO-AndroidAPS`, Thomas' fork. `upstream`: `nightscout/AndroidAPS`.
- `master` is the personal integration branch. Initial unmodified baseline: official `4.0.0`, commit `f77abd9b82`.
- Use short `feature/<topic>` branches for behavior changes. Keep related tests and implementation together; avoid unrelated formatting/refactoring.
- For updates: fetch upstream, select an official release tag and record its full commit SHA; create `update/<version>` from clean personal master; merge that release without rewriting published history; review every retained customization, run relevant local tests/build, then integrate into master. Recheck if master changed during validation.
- Record retained customizations and their production-path tests in one concise `docs/tho/CUSTOMIZATIONS.md` when porting begins. Start from official code; selectively port personal behavior, never merge the whole Boost branch.
- Retained personal behavior: 80/10/10 SMB DynISF (legacy device-test comparator aligned) and one-hour DST handling. Exclude Boost parameter optimization, daily scheduler, ownership controls and Boost-only history sync. Do not import the Boost profile-implied 35%/15% fallback thresholds. See the complete THO01..09 mapping in CUSTOMIZATIONS.md before adding or removing behavior.
- Tests and builds run locally only. Do not add or trigger GitHub builds. Do not run IDE and CLI builds concurrently. Use the workspace helper with -ZoneName AAPS-ORIG -RepoName AAPS-ORIG-THO (JDK21, one worker, 3 GiB Gradle/2 GiB Kotlin heaps); exports go to Desktop/HEALTH/LOOP/AAPS/ORIG. Do not assume Boost task names/settings match this repo.
- Runtime acceptance uses real Android/Bluetooth hardware, no emulators. A successful build/test is distinct from hardware acceptance.
- Thomas builds the signed release and performs the phone and Dana-RS test. Use `C:\Users\linus\Desktop\SYNC\AAPS` for handoff of that signed APK. The workspace helper exports debug builds to the ORIG artifact directory.
- Tag built candidates and personally accepted versions with distinct names and record source SHA and artifact hash. Preserve the last accepted artifact until a replacement is accepted.
- GitHub Actions is disabled at repository level to enforce local-only builds. Keep it disabled unless Thomas explicitly changes this choice. Signed reference tho-aaps-4.0.0-signed1 records Thomas' initial successful clean-data startup; it does not prove a safe in-place Boost-to-original migration. Never make app-data deletion part of routine updates.
- No force-push, destructive cleanup, release signing, therapy-device installation or overwriting sensitive configuration without explicit authorization under workspace policy. Keep secrets and health data out of Git.

Read `docs/tho/CUSTOMIZATIONS.md` for the personal behavior diff against official AAPS 4.0.0 and remaining migration work.
