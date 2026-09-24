# Real cards mode

> **Read when:** changing card locations, exact draws, printed card queries, or card identity.

## Current representation

`CardBack` is abstract. An unplayed card has a concrete back family, a Player owner, a represented
`Class<CardFront>`, and a `CardLocation`. A front inherits the same `Card<Player, Class<This>>`
identity. `PlayedEvent` also inherits it, so a face keeps its identity after event completion.
The class-scoped `HAS MAX 1 Card<Player, Class<This>>` rule enforces one live location per face
across players, backs, fronts, and played events.

`Hand`, `Selecting`, and `Revealed` are unowned locations. Moving a back is a transmutation, not a
remove and gain. The existing `CardLocationCleanup` removes unchosen backs from `Selecting` and
`Revealed` when work becomes idle. Setup offers ten exact project backs and two exact standard
corporation backs per player. The player chooses a corporation and may keep each offered project.

## Dealer

Catalog Pets authors draws explicitly as `DrawCard<Class<Back>, Location>`. The custom instruction
takes the first eligible face in Class-name order from the active catalog. `DeckSpent` records every
traversed face in the World. `SearchForTag`, `SearchForReference`, and `SearchForUntaggedCard` skip
nonmatching faces by recording them as spent without creating backs. Search exhaustion consumes the
remaining deck and produces no back. The dealer also skips a face already present as a back, front,
or played event. The spent marker makes these draws reproducible through World rollback.

Counted Atomized gains are split before type narrowing so each draw can choose a different face.
Counted transmutations are split at runtime so each existing back retains its own face. A `THEN`
whose first stage expands to several instructions waits for all of them before continuing.

Printed-tag reveal and offer rules use `EACH` to bind each exact back and `PrintedTagOf` to query
its represented front's immutable tags. Pets branches on the metric; `MoveSelectedCard` transmutates
the selected back to hand without changing its face. `RecoverPlayedEvent` transmutates a selected
event record back to its own exact project back. These are custom instructions because ordinary
Pets movement cannot yet carry a selected back's represented Class into a new location Type.

## Current limits

- Deck order is stable Class-name order, with no shuffle or reshuffle. A spent face is unavailable
  for the rest of the game, including after an unplayed back is discarded.
- A manually supplied exact card does not mark its face spent. The dealer skips it while it is in
  the World, but if it is later removed before any draw traverses it, that face can return to the
  ordered deck. Use `DrawCard` for ordinary entries.
- Older follow-mode replay tests and helpers assume fungible backs and do not establish real-card
  behavior. Use `RealCardDrawTest` for current exact-card scenarios.
- Hidden information and DraftVariant are outside this mode's present scope.
