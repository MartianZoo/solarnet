# Mars Playground

Agreed product direction for a new Solarnet-backed web app: an interactive rules laboratory.
Construct a scenario, enter gameplay mode, and try a move to see how the rules resolve it. The user
is omniscient and controls every player. Solarnet supplies the game rules.

## Technology and experience

- Use **Compose Multiplatform**, with Kotlin as the UI implementation language. This choice is
  settled; it means the Compose Multiplatform UI toolkit, not Compose HTML or Kobweb.
- Target desktop and tablet browsers. Phone layouts are out of scope. An installable desktop
  version might be useful later, but is not a current requirement.
- Run Solarnet locally in the browser. Offline use is desirable; application and asset caching
  will need attention. Server-side game execution is not needed for the initial app.
- Aim for a freely zoomable tabletop with tool windows the user can move and resize. Physicality
  matters: the app should feel like manipulating the contents of an ever-changing Game World as
  tangible objects, including continuity as pieces move between places.
- Ordinary browser conventions are not a design priority. A shareable URL that restores the same
  view is a later feature; its precise contents remain to be decided.
- The code itself is part of the product. Clear ownership, expressive Kotlin, and the pleasure of
  understanding and changing the implementation matter even when an agent writes most of it.
  Reusable additions should be useful to other Kotlin developers; reaching a JavaScript audience
  is not a goal. An active, welcoming community and a credible maintenance history matter.

## Object-centric UI vocabulary

The goal is a clean correspondence between Pets component types in Game World and their visual
representations. Ordinary product changes should be expressed in the language of cards, resource
piles, tiles, hex spaces, and tool windows. They should not routinely require editing pointer
handling, coordinate transforms, subscription cleanup, or rendering machinery. This is the same
design preference behind Pets: a change belongs at the abstraction level of the concept changing.

The proposed approach is to compose a small vocabulary of reusable UI elements and, where useful,
ordinary Kotlin objects holding interaction state. Validate this through real interactions before
settling class names or a general API. Compose supports this approach; adopting it does not require
building a second widget system or a new UI language.

The intended ownership boundary is:

- **Game World owns game facts:** components, resource counts, pending tasks, and history. Visual
  representations observe these facts rather than maintaining another authoritative game model.
- **Presentation objects own interaction state where needed:** what is being held or inspected,
  the tabletop camera, window placement, and animation continuity. Persistent visual identity need
  not imply a new game-component identity.
- **Reusable composables own presentation mechanics:** drawing, layout, animation, and input for
  the physical vocabulary. Compose retains responsibility for its widget tree and rendering
  lifecycle; do not duplicate those with another layout or invalidation system.

Correspondence does not require one new Kotlin class per Pets type or one independently identified
object per resource unit. Game World stores component types with counts. A pile can visualize a
count, and a supply piece can represent an available component type or pending choice rather than
an existing piece in the world. The same card or component may also appear in an inspector.

Interaction meaning depends on mode. In sandbox, dragging a tile may request a gain, removal, or
transmutation while preserving structural invariants. In gameplay, dropping a supply greenery onto a
hex can specify a narrowing and execution of an existing task. The UI must use the game rules and
task machinery rather than duplicate placement legality or card behavior in visual components.

Game-to-UI observation should be routine and reusable. For example, a counter watches
`Plant<Kevin>` and receives its new count through `ComponentGraph.listenToCount`. A small Compose
binding should manage initial display, subscription lifetime, and cancellation so each counter
does not repeat that machinery. The UI should not poll. Selective notification inside Game World
is still an improvement tracked in [TODO.md](TODO.md); the current implementation rechecks all
subscribed counts after each component change and calls only listeners whose counts changed.

Examples of the desired change boundaries:

| Requested change | Owning concept |
| --- | --- |
| Correct how Hired Raiders resolves | Its Pets rule, or the owning general engine semantics if those are wrong |
| Show resources as a pile of cubes | Resource-pile presentation |
| Make held pieces lift and cast a shadow | Shared piece interaction and appearance |
| Interpret a greenery drop differently in sandbox and gameplay | Mode-dependent interaction using existing game operations |
| Start the task window beside the board | Workspace arrangement |

A promising first design exercise is one complete tile interaction, one resource pile, and one
card. Judge their application code by whether human-level changes stay above the mechanics. New
kinds of interaction can require lower-level work, but implement those mechanics once and expose
them through a meaningful vocabulary. Stable visual identity across transmutation, movement, and
history navigation still needs design; do not assume Compose provides that mapping automatically.

## Configuration and sandbox

- Start with a dialog for expansions, player count, and other game options. Label a configuration
  **Unofficial** when no rulebook explicitly permits that combination (for example, solo without
  Corporate Era), but allow it. This label concerns configuration, not scenario plausibility.
- Sandbox mode allows direct manipulation: move tiles freely, adjust resources and production with
  up/down controls, assign the starting-player token, and select the phase. Support dragging and
  click-to-pick-up/click-to-place.
- Find cards by name with autocomplete and place them in a player's hand, event pile, or in play,
  provided the card is not already assigned elsewhere.
- Sandbox does not pay costs or fire normal direct effects. Structural invariants must still hold:
  for example, putting a card into play must provide its tags. Some engine work is still needed.
- This is a playground: manipulation is not cheating. Prevent patently bogus states, without
  requiring that the scenario could have arisen through normal play.

## Cards and gameplay

- Drawn cards are generic card backs until their identities matter. Require identity declaration
  when an operation needs it, including playing a card or moving it to Revealed. Other cards may
  remain unidentified. Earlier declaration is optional and low priority.
- Gameplay mode applies normal game rules and exposes the Solarnet task queue. Selecting a task shows
  the available ways to narrow it.
- Task execution is manual, including concrete tasks; avoid autoexecution so the user can follow
  resolution step by step. Effects accumulate in a log viewer panel.
- Separate explanations of why a rule applies are not required. Watching the task queue resolve
  is the intended way to understand the result.

## History and restarting

- In gameplay mode, freely browse backward and forward through history at appropriate boundaries,
  never stopping inside indivisible work such as double-colon effects.
- Browsing does not change the live game. Explicitly roll back to the viewed point to discard the
  subsequent history and try another move.
- Return to sandbox by rolling all the way back, or reset to start over.
- Saving and reloading scenarios is desirable later, but not required initially.

## Open questions

- Minimal bootstrapping may be enough for the initial state. Determine which core invariants cards
  depend on; a normally initialized, realistic game is not necessarily needed.
- Define precisely what selecting a phase establishes, including which phase-entry procedures
  have already occurred and which tasks remain.
