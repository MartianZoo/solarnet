# Mars Playground

Agreed product direction for a new Solarnet-backed web app: an interactive rules laboratory.
Construct a scenario, enter play mode, and try a move to see how the rules resolve it. The user
is omniscient and controls every player. Solarnet supplies the game rules.

## Configuration and setup

- Start with a dialog for expansions, player count, and other game options. Label a configuration
  **Unofficial** when no rulebook explicitly permits that combination (for example, solo without
  Corporate Era), but allow it. This label concerns configuration, not scenario plausibility.
- Setup mode allows direct manipulation: move tiles freely, adjust resources and production with
  up/down controls, assign the starting-player token, and select the phase. Support dragging and
  click-to-pick-up/click-to-place.
- Find cards by name with autocomplete and place them in a player's hand, event pile, or in play,
  provided the card is not already assigned elsewhere.
- Setup does not pay costs or fire normal direct effects. Structural invariants must still hold:
  for example, putting a card into play must provide its tags. Some engine work is still needed.
- This is a playground: manipulation is not cheating. Prevent patently bogus states, without
  requiring that the scenario could have arisen through normal play.

## Cards and play

- Drawn cards are generic card backs until their identities matter. Require identity declaration
  when an operation needs it, including playing a card or moving it to Revealed. Other cards may
  remain unidentified. Earlier declaration is optional and low priority.
- Play mode applies normal game rules and exposes the Solarnet task queue. Selecting a task shows
  the available ways to narrow it.
- Task execution is manual, including concrete tasks; avoid autoexecution so the user can follow
  resolution step by step. Effects accumulate in a log viewer panel.
- Separate explanations of why a rule applies are not required. Watching the task queue resolve
  is the intended way to understand the result.

## History and restarting

- In play mode, freely browse backward and forward through history at appropriate boundaries,
  never stopping inside indivisible work such as double-colon effects.
- Browsing does not change the live game. Explicitly roll back to the viewed point to discard the
  subsequent history and try another move.
- Return to setup by rolling all the way back, or reset to start over.
- Saving and reloading scenarios is desirable later, but not required initially.

## Open questions

- Minimal bootstrapping may be enough for the initial state. Determine which core invariants cards
  depend on; a normally initialized, realistic game is not necessarily needed.
- Define precisely what selecting a phase establishes, including which phase-entry procedures
  have already occurred and which tasks remain.
