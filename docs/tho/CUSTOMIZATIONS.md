# Original AAPS — personal changes

Base: official 4.0.0 (`f77abd9b82b308e8fc330da0d7a31370fb4a0f49`). No Boost merge.

## Current focus: when 80/10/10 actually applies

Thomas narrowed the first step on 2026-11-14 to tracing and testing the 80/10/10 calculation in original AAPS. DST and other migration candidates are deferred, not abandoned. No changes to plausibility limits are authorized by assumption.

The official formula in `OpenAPSSMBPlugin.calculateRawDynIsf` uses weighted-eight-hour / seven-day / one-day TDD at 33/34/33. The proposed personal change is 80/10/10 at that single production expression; adjustment factor and original profile percentage still multiply the result. No Boost pull-down or profile-implied 35%/15% guard is imported.

### Paths where a fresh blend does not become the returned ISF

1. Dynamic sensitivity is OFF: `calculateVariableIsf` returns null; `ProfileSealed.getIsfMgdl` uses the profile ISF.
2. A saved APS result already has nonzero variable sensitivity: the public ISF accessor returns it first. The database query selects the latest result in the preceding five minutes. It reuses a prior dynamic result, not a seven-day ISF average. This path does not prove a newly changed formula ran.
3. No saved result, but an eligible cache hit: public ISF lookup can return a cached dynamic value. The key combines a 30-minute time bucket with integer glucose; the timestamp must precede the current clock. This also reuses a prior dynamic value, not a long-term average.
4. Missing current glucose: public ISF lookup returns null.
5. Missing any TDD component: one-day and seven-day calculations require `allowMissingDays=false`; rolling 24h and both recent four-hour components must also exist. Public lookup returns null and its profile caller falls back. In the loop, missing TDD components disable dynamic mode for that run and select the ordinary SMB/autosens path.
6. No fresh glucose status from the SMB glucose calculator: raw calculation cannot produce variable sensitivity even when all TDD parts exist. The loop's existing invalid-input checks can reject the run. Do not confuse missing evidence with a plausible large ISF change.
7. Non-finite or non-positive algorithm inputs: the existing loop validation aborts; it does not silently replace the mixture with a long-term average.

### Paths that change the result while retaining the blend

- Adjustment factor and profile percentage scale the blended TDD.
- The main loop calls `calculateRawDynIsf` directly for each invocation. The public ISF accessor's database/cache shortcut does not replace that fresh `TDD`/`variable_sens` pair.
- `DetermineBasalSMB` uses `variable_sens` for dynamic sensitivity and uses the same `profile.TDD` in its predictions and `future_sens`. Future dosing sensitivity can use current/predicted glucose, so its numeric ISF can differ while 80/10/10 still applies.
- Optional TDD/carbohydrate sensitivity adjustment and temporary targets affect basal/targets. Ordinary dose constraints can limit an action even with dynamic mode active. They do not establish that the TDD mixture was overwritten.

### Plausibility distinction

The separate Boost engine can reject seven-day TDD below 35% or adjusted blended TDD below 15% of `1800/profileISF` and keep profile sensitivity. Original AAPS SMB has no such relative thresholds. We are not introducing them. No patient log was supplied that establishes which branch caused Thomas' past observations; code-path findings are not a retrospective diagnosis.

### Verification

Tests exercise the real public `OpenAPSSMBPlugin.getIsfMgdl` with controlled history/glucose inputs, rather than a copied blend helper. Stable history, recent changes in both directions, zero recent delivery, adjustment factor, profile percentage, disabled mode, missing week/glucose and database reuse are covered. Execution results and final source/build identity will be recorded after verification.
