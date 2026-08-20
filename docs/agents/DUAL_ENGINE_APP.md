# Dual-engine Terraforming Mars app

**Status: the Gate 1 development runtime boundary is proven; Gate 2 covers its first paid project,
turn boundary, standard project, and map target; and the first Solarnet-side Gate 3 projection is
verified.** This records the smallest promising design, verified runtime facts, and the proof gates
that should precede broad integration.

## Goal

Modify the open-source `terraforming-mars/terraforming-mars` app so that every supported game input
is also executed by Solarnet, then compare the two resulting game states at every stable input
boundary. The existing app remains the playable UI and owns persistence, decks, chance, and hidden
information. Solarnet is a second rules engine, not a source of UI state.

The inspected baselines were:

- Solarnet `7597bbc264d44077f774b99f6e79dcdf2a3986fc` (2026-08-19); and
- `terraforming-mars/terraforming-mars`
  `7dfdbb353d362f38e6f77e50096c7463e431404c` (2026-08-17).

Pin both revisions for the first proof. Upgrading either side is a deliberate compatibility change.

## Settled principles

### Initial milestone and repository

The first complete-game target is the smallest ordinary multiplayer profile: two players, Tharsis,
Corporate Era, no drafts, no beginner corporations, no undo, and no custom content. Further
restriction of the admitted card set is acceptable when generated from the reviewed compatibility
manifest rather than a hidden hand-maintained exception list.

The prototype will be private in the `MartianZoo` GitHub organization. GitHub requires a fork of a
public repository to remain public, so this cannot be a GitHub fork in the technical sense. Create a
new private repository from the app's full Git history, retain the public repository as an
`upstream` remote, and record the imported upstream revision. This preserves the useful fork
workflow without joining the public fork network.

For the stated private personal experiment, the GPL permits running and modifying the app without
publishing the changes. Keep the app's GPL license and notices and Solarnet's Apache-2.0 license and
notices in the private repository. Revisit the combined work's distribution obligations before
giving a copy to anyone outside the organization or making it public.

### The app drives; Solarnet follows

The app already owns shuffled decks, deals, drafts, private hands, and saved games. Solarnet's real-
card mode is only a proposal, while its committed follow mode is specifically able to accept card
outcomes supplied by another game. Therefore:

- do not implement real-card mode for this integration;
- feed app-selected card identities to Solarnet as they become relevant;
- compare card identities only where Solarnet represents them, such as played cards and played
  events; and
- compare private hands by count, not by exact contents or deck order.

This is an intentional parity boundary, not an ignored mismatch.

### Translate intent, never copy mutations

The bridge must execute ordinary Solarnet tasks. It must not calculate the app's before/after delta
and reproduce it with raw changes. In particular, parity execution must never call `sneak`. A
Solarnet rejection or divergent result is the experiment's output.

The app's localized titles and button labels are not an API. Each accepted `PlayerInput` needs a
stable semantic description from which the bridge can select or narrow Solarnet tasks. Existing
`InputResponse` values supply useful payloads—card, space, player, amount, payment, resource, and
option path—but many `SelectOption` callbacks need explicit semantic metadata.

### Compare projections, not object graphs

Neither engine's internal representation is canonical for the other. Each side produces the same
small parity snapshot, and the comparator compares those snapshots. Move translation and state
comparison remain independent: agreement in one cannot manufacture agreement in the other.

### Stop at unsupported content

Game creation uses a generated compatibility manifest. It admits only the intersection of selected
Solarnet content and corresponding app content, minus Solarnet's documented gaps. Unsupported
options are rejected before setup; they are not silently omitted after a game begins.

## Existing seams

The app's `Player.process(InputResponse)` is the central accepted-input boundary. It clears the
current `waitingFor`, processes the selected input, drains synchronous deferred actions, and leaves
the next `waitingFor`. `Game.serialize()` persists public game state but deliberately writes no
deferred actions; `Game.deserialize()` reconstructs the next input from the phase and state.

At the pinned app revision, the sole production HTTP call to that boundary is in
`src/server/routes/PlayerInput.ts`: `processInput` parses and validates the response, handles undo
separately, calls `player.process(entity)`, and immediately writes the player model. A route-local
prototype must capture the old `waitingFor` before that call because successful processing replaces
it, then invoke the shadow bridge only after the call returns. Rejected app inputs and undo must not
reach Solarnet. Direct test callers of `Player.process` are intentionally outside this first hook.

Solarnet exposes actor-scoped `Gameplay`, pending tasks, an event-backed `Timeline`, and
`TfmWorkflow.Auto`. Commands are failure-atomic and automatic effects are drained before the outer
command completes. The new JavaScript-only `parity` module exports the first deliberately narrow
external snapshot and command facade; it is a probe, not yet a stable public API. Its pull-based
`eventsSince(cursor)` diagnostic returns a new cursor and the full Pets-rendered event lines in that
range. Consumers poll only after a completed command; the engine has no parallel observer path.

Canon and Pets still use browser XHR by default. Their JS loaders now accept one optional host
resource-reader function, allowing the parity module to load the same packaged resources in Node.
The existing browser smoke test proves the fallback remains intact. The existing Rego socket server
has one mutable session and a human text protocol, so it is not the production bridge.

## Common parity snapshot

Start with state both engines represent faithfully:

- profile identity: map, enabled expansions/variants, player seats;
- workflow: generation, phase, first player, passed players, and game end;
- global parameters: temperature, oxygen, oceans, and Venus when enabled;
- per player: TR, six resources, six production values, hand count, played card IDs, card-resource
  counts, used actions, colonies, fleets, claimed milestones, and funded awards;
- board: tile kind, owner, and normalized row/column area;
- shared public objects: colony tracks and supported milestone/award state; and
- scoring at explicit scoring checkpoints, with a category-by-category diff.

Explicitly exclude timers, logs and wording, database IDs, RNG cursor, deck/discard order, exact
private hands, UI hints, and every state belonging only to an unsupported option.

Later add **continuation parity**: both engines must be waiting on the same actors for semantically
equivalent choices. Do not require their task trees or menu nesting to be structurally identical.

Card translation should use printed card number (`CardMetadata.cardNumber` to Solarnet `Card{id}`),
with a reviewed exception table only where published identifiers genuinely differ. Map spaces use
the app's row-major space ID and Solarnet's existing row/slant-column conversion.

## Staged proof gates

### Gate 0: Freeze the selected compatibility profile

Begin with two players, Tharsis, Corporate Era, no draft, no beginner corporations, no undo, and no
custom or fan content. Generate the card intersection from app metadata and Solarnet definitions;
ban every other card at app setup. Reject every unselected app variant.

**Pass condition:** both engines construct the same profile, and a report explains every excluded
option and card.

### Gate 1: Prove the runtime boundary

Prefer one in-process Node-facing Kotlin/JS facade with string-in/string-out JSON methods. Its
surface is deliberately tiny: create session, apply semantic move, get snapshot, read diagnostic
events, and close session. First prove resource loading, one exported facade, one game, and one
command in Node.

If browser/Node resource sharing makes that proof disproportionate, fall back to one managed JVM
child process with the same JSON protocol and multiple session IDs. Do not expand the human Rego
protocol into an application API.

**Pass condition:** a TypeScript test creates Solarnet, performs one ordinary task, reads a snapshot,
and tears down cleanly with no app server involved.

**Verified:** `SolarnetSession` is emitted to TypeScript with `apply(moveJson)`, `snapshot()`,
`eventsSince(cursor)`, and `close()`. Its Node integration test creates a two-player Corporate Era
game, uses semantic `selectCorporation` messages for both players, advances from corporation
selection to the action phase through ordinary `TfmGameplay` operations, reads JSON snapshots and
incremental event batches, and shuts down. The checked-in external TypeScript consumer compiles
against the generated declarations, imports and runs the standalone development package, reads its
packaged resources, applies both setup moves, and prints each move's new events without replaying
old ones. It now also applies the first Gate 2 move described below. The optimized production
package currently misparses a valid Canon effect; resolving that is deliberately deferred, and the
development package is the Gate 1 artifact.

### Gate 2: Define the semantic move protocol

Define a closed protocol for the shared concepts actually selected by app inputs: initial-card
selection, pass, play card plus payment, use card action, standard project, milestone, award, trade,
tile/colony/player/card/resource targets, quantities, optional branches, and research purchases.

Add optional parity metadata to `PlayerInput`; composite `or` and `and` inputs preserve and recurse
through the chosen child. Attach metadata first at systemic constructors such as project-card play,
standard projects, payments, spaces, and targets. Add component-specific metadata only when the
component really introduces distinct intent.

The Solarnet interpreter converts the protocol to ordinary task revisions and executions using the
generic `Gameplay` surface. Do not add card-specific methods to Solarnet.

**Verified so far:** `playProject` carries a player seat, printed card ID, and explicit M€/steel/
titanium payment composition. For this first proof, the facade delegates to the existing
transitional `TfmGameplay.playProject` convenience path, which specializes one generic project card
already in that Player's follow-mode hand and executes the ordinary project-play and payment tasks.
This is evidence for the message shape and underlying task path, not yet the settled generic
interpreter. The Node and external TypeScript scenarios buy one generic card during setup, play card
105 (Earth Office) for 1 M€, and observe both the play and payment in the diagnostic event feed;
the first normalized Solarnet state slice is described under Gate 3. Other target kinds, follow-up
choices, and other payment media remain unimplemented protocol families.

`endTurn` and `pass` are separate semantic moves. `endTurn` declines only the optional second-action
offer, leaving that Player active in the generation; `pass` executes the ordinary offered `Pass`
task and removes that Player from the action rotation. One scenario ends Player 1's turn after Earth
Office and observes the workflow rotate to Player 2. The main scenario passes Player 2 after Player
1's two actions and observes the workflow return to Player 1. Negative tests prove that `pass` is
rejected during a second-action offer and `endTurn` is rejected during a first-action offer.

The `standardProject` proof performs Aquifer as Player 1's second action at the app's actual input
granularity. The first message selects Aquifer, pays 18 M€, and leaves Solarnet waiting on its real
ocean-placement task. A following `placeTile` message carries the app's row-major space ID `04`, not
a Pets instruction or Solarnet coordinate. The facade translates that ID through the active map
definition, places an ocean on `Tharsis_1_2`, applies the intrinsic TR and the area's two-steel
bonus, and rotates to Player 2. Each accepted app input is one failure-atomic Solarnet command;
together the two commands continue and finish one resumable operation.

**Pass condition:** a short scenario covers setup, a paid project, a target choice, a tile placement,
an automatic effect, a second action, and pass without parsing display text.

### Gate 3: Build independent projectors and diagnostics

Implement one snapshot projector in each runtime. The comparator reports paths and both values,
plus the accepted semantic move, app input tree, Solarnet pending tasks, Solarnet events for the
command, profile, and both pinned revisions.

Classify every disagreement as bridge defect, projector defect, upstream rule difference, Solarnet
defect, documented Solarnet variant, or unsupported content. A classification never suppresses a
diff unless the compatibility contract is updated explicitly.

**Verified so far:** Solarnet projects generation, normalized phase, first and passed seats, TR, all
six resources and production values, hand count, public played-card IDs, temperature, oxygen,
oceans, and normalized ocean tiles. Set-like arrays are sorted, ocean ownership is explicit `null`,
and all card IDs come from Canon rather than class-name text. The short Gate 2 scenario asserts the
complete projected result after Player 2 passes; a separate event-card scenario verifies that a
played event is mapped through its typed card dependency. This slice deliberately omits active or
waiting-player continuation state and rejects non-ocean or off-map tiles rather than flattening
unmodeled state into a false equality. The app projector, comparator, and disagreement diagnostic
remain unimplemented.

**Pass condition:** injected resource, production, card-resource, and tile mismatches each yield a
small intelligible diagnostic.

### Gate 4: Attach a shadow session to the server

Create one parity session beside each app `Game`. After `Player.process` accepts an input and both
engines reach their next stable waiting boundary, execute the semantic move in Solarnet and compare
snapshots. The first version is diagnostic: on rejection or mismatch it freezes further inputs and
preserves a reproducible bundle rather than attempting to repair either state.

Persist the semantic transcript and bridge/schema revisions with the app game. On load or sidecar
restart, create a fresh Solarnet game and replay the transcript. Performance is secondary, and
replay avoids inventing Solarnet serialization before its state model calls for it.

Undo restores the transcript cursor associated with the app save point and rebuilds Solarnet. Game
cloning creates a new parity session by replaying the cloned transcript.

**Next implementation boundary (verified, not implemented):** keep a server-only registry keyed by
app game ID, initially containing a Solarnet session and event cursor. Make the local prototype
opt-in through an explicit path to Solarnet's standalone development package; absence of that path
leaves the ordinary app unchanged. Initialize the cursor from `eventsSince(0)` after session
creation so construction noise is not replayed. After each successfully translated app input,
apply the corresponding Solarnet command, compare the available snapshot slice, call
`eventsSince(cursor)`, advance the cursor, and print every line as
`[solarnet <game-id>] <event>`. Never add these events to the HTTP model or browser because they may
contain hidden information. A shadow rejection or mismatch occurs after the app has already
accepted its input, so it needs its own diagnostic/freeze path and must not fall through the route's
existing 400 response as though the app rejected the move.

**Pass condition:** create, play, save, reload, undo, and continue one short game with parity checked
after every accepted input.

### Gate 5: Complete base-game action families

Cover systemic moves before individual cards: initial selection, research, both action slots, all
standard projects/actions, payment composition, tiles and adjacency bonuses, milestones/awards,
production, final greenery, scoring, and solo only after multiplayer is sound.

Require a coverage counter proving that every reachable `PlayerInput` kind and semantic operation in
the profile was classified. Unknown input metadata is a hard failure, not a skipped comparison.

**Pass condition:** at least one complete two-player game and a seeded corpus of generated legal app
inputs remain equal at every boundary.

### Gate 6: Expand by supported product

Expand one product at a time: Hellas/Elysium, Prelude, Venus Next and World Government Terraforming,
Colonies, supported promos and Milestones & Awards, then Utopia/Cimmeria. Regenerate the intersection
manifest at each step. Keep the missing items in `docs/what-is-supported.md` excluded even when the
app implements them.

Each newly admitted operation family needs a focused dual-engine scenario before its cards enter
the generated full-game corpus. Fan expansions, Turmoil, Prelude 2, Automa, Moon, Ares, Pathfinders,
Underworld, CEOs, and other unsupported variants stay unavailable.

**Pass condition:** every admitted card and shared option is reachable in at least one focused or
generated parity run, and each selected product has a complete-game run.

### Gate 7: Make the experiment usable

Add a Spartan compatibility notice at game creation and a parity status/diff view for development
games. Export a sanitized replay bundle containing configuration, semantic moves, normalized
snapshots, and version pins. Never expose hidden app state through diagnostics visible to players.

The first live event view belongs in the development app server's terminal: after each translated
input completes, poll `eventsSince(cursor)` and print the returned lines. The full task and change
history can expose hidden information, so do not send this feed to an ordinary player's browser.

Only after the diagnostic system is trustworthy should we consider speculative dual execution that
blocks a move before either live state changes. That requires disposable Solarnet worlds or another
honest two-copy transaction; it is not needed to demonstrate the Frankenstein app.

## Verification strategy

- Keep both projects' existing suites green.
- Add protocol contract tests independent of either engine.
- Add paired scenario tests for each systemic operation family.
- Replay selected existing Solarnet whole-game timelines through both engines where the evidence can
  be translated without inventing hidden draws.
- Generate legal inputs from the app's current `waitingFor` tree and run seeded complete games.
- Compare after every stable boundary and again at production, generation, and scoring boundaries.
- Review every final diff bundle; a green build alone does not prove that unsupported state was not
  accidentally omitted from the projector.

## Principal risks

1. `SelectOption` often contains only a callback and human title. Stable semantic metadata is the
   largest app-side instrumentation cost.
2. The engines may expose choices at different granularities. The bridge must preserve one game
   intention without requiring identical menu nesting.
3. App save/undo boundaries and Solarnet workflow commit floors differ. Transcript replay is the
   initial honest reconciliation.
4. Some apparent mismatches will be genuine rule disagreements or Solarnet's documented variant.
   Preserve them as explicit results until the governing rule is settled.
5. A broad content rollout can turn the bridge into hundreds of exceptions. Stop a product gate if
   systemic metadata plus generic task narrowing cannot account for its moves cleanly.
6. The app is GPLv3 and Solarnet is Apache-2.0. Any distributed combined work needs an explicit
   licensing/notice review and will retain the app's GPL obligations.

## Remaining decision

The private prototype freezes further player input immediately on an unexplained mismatch unless a
later decision selects report-only behavior. Public distribution is not currently planned; if that
changes, packaging and notice work must be reviewed without changing the engine design.
