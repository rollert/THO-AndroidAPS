# Thomas' personal original AAPS

Follow the parent AAPS workspace instructions. Keep this variant close to official AAPS and changes small.

- Repository: `AAPS-ORIG/AAPS-ORIG-THO`. Check root, branch and dirty state before edits/builds.
- `origin`: `rollert/THO-AndroidAPS`, Thomas' fork. `upstream`: `nightscout/AndroidAPS`.
- `master` is the personal integration branch. Initial unmodified baseline: official `4.0.0`, commit `f77abd9b82`.
- Use short `feature/<topic>` branches for behavior changes. Keep related tests and implementation together; avoid unrelated formatting/refactoring.
- For updates: fetch upstream, select an official release tag and record its full commit SHA; create `update/<version>` from clean personal master; merge that release without rewriting published history; review every retained customization, run relevant local tests/build, then integrate into master. Recheck if master changed during validation.
- Record retained customizations and their production-path tests in one concise `docs/tho/CUSTOMIZATIONS.md` when porting begins. Start from official code; selectively port personal behavior, never merge the whole Boost branch.
- Exclude Boost parameter optimization, its daily scheduler, ownership controls and supporting Boost-only history logic. Assess TDD weighting, applicable DynISF guards and DST changes against the actual official implementation before porting. These are candidates, not already validated changes.
- Tests and builds run locally only. Do not add or trigger GitHub builds. Do not run IDE and CLI builds concurrently. Use a dedicated local build profile when created; do not assume Boost task names/settings match this repo.
- Runtime acceptance uses real Android/Bluetooth hardware, no emulators. A successful build/test is distinct from hardware acceptance.
- Tag built candidates and personally accepted versions with distinct names and record source SHA and artifact hash. Preserve the last accepted artifact until a replacement is accepted.
- No force-push, destructive cleanup, release signing, therapy-device installation or overwriting sensitive configuration without explicit authorization under workspace policy. Keep secrets and health data out of Git.

Initial setup changes only remotes/local push configuration and this instruction file; application code remains official AAPS 4.0.0.
