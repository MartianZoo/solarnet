# Admin routing of below-surface tasks — stashed experiment

> **Status:** Research and a recoverable prototype, not current committed behavior. The source and
> test changes are in a work3 Git stash; this note records the selected requirements, observations,
> and remaining design work. Source and tests take precedence over this note after restoration.

## Selected requirements

- A Player's queue should contain work the Player may order or carry out in the surface game. The
  distinction is **not** whether the work offers a choice. A fixed consequence may still be part of
  the physical game's freely ordered effects. Printed card tags installing themselves are an
  example of below-surface work: that installation is not among the effects a human orders.
- Below-surface tasks may be assigned to Admin, while retaining which Player's action the chain is
  happening on behalf of. A genuine gameplay task spawned later should return to the active
  Player's queue. The eventual narrower or context owner may be a different Player.
- Admin can explicitly create a Player-owned request for a named Player even when nobody is on
  turn. `BY` identifies attribution/trigger agency; it does not itself assign a task.
- Admin's normal Agent autoexecution is fixed at `EAGER`, rather than a policy clients can lower.
  The lower-level engine remains available for tests of intermediate states. If Admin could be
  slowed, the expected failure would mostly be stalled Player queues; the fixed policy removes that
  ordinary configuration.
- An on-turn Player is a stronger fact than merely having tasks queued. Prelude and Action are
  clear sequential-turn phases. Final Greenery and Corporation may also need an on-turn Player;
  Setup and Research appear not to. This phase classification has not been settled.

## Prototype and verified observations

The stashed prototype adds an initial task assignee separate from its controller and contextual
Actor, plus authored `Trigger FOR Admin: instruction` syntax. An Admin task can retain the Player
controller of the originating operation. Admin's selection can then produce an ordinary task on
that Player's queue; selecting an abstract task may subsequently hand narrowing to its contextual
Actor. A separate Agent change rejects attempts to set Admin's policy below `EAGER`.

The prototype moved five card-granted resource-value marker effects and the Promo attack watchers'
hidden resource-removal/production-decrease records from `::` to `FOR Admin:`. The unowned attack
watchers needed explicit care to keep the attacker as the resulting event Actor rather than crediting
the victim merely because the removed resource belonged to them. Canon/card classification also had
to retain Admin-assigned self-gain effects as persistent card effects instead of treating them as
card-play immediates.

The affected JVM suites passed, including 859 Terraforming Mars tests, 192 engine tests, the Agent,
Pets, state, canon, and card-generator suites, plus `spotlessCheck`. Two full-game replays passed
after removing their temporary Admin policy overrides. Tests that intentionally inspect an
intermediate Admin queue used direct policy-free ActorEngine transactions. These results support the
narrow migrations and the separation of assignment, control, and attribution; they do not establish
the general routing rule.

An attempted printed-tag migration to queued Admin work failed 30 Terraforming Mars tests and was
reverted. Card-entry watchers such as `EventCard(HAS SpaceTag): ...` currently test the card before
an Admin task can install its printed tag. Thus `::` still has a real timing role in the present card
model. The failure does not prove the tag must remain `::` after a card-model redesign.

## Unresolved design work

1. Audit the remaining `::` sites by *surface-game order* and by exact event timing. Payment and
   printed-tag paths are especially timing-sensitive; Admin-owned work need not be distinguished
   merely by its current `:`/`::` spelling. Do not infer from a fixed outcome that the Player should
   lose control of when it occurs.
2. Test a live `WhoIsOnTurn` component (or a smaller equivalent) in a sequential phase and a mixed
   Player → Admin → Player chain. The prototype still propagates `Task.controller` through events,
   so it does **not** test whether that propagation can be replaced by turn state. Explicit named
   Player requests must still work in no-turn phases.
3. Decide what guarantees the normal Agent boundary needs beyond a fixed Admin policy. The current
   shared loop may stop when work is blocked or unresolved; fixing a property setter alone does not
   prove that no actionable Admin task is exposed after every Player-facing call. Direct `addTasks`
   and `sneak` are also lower-level escape hatches. `EAGER` may choose among legal Admin tasks, so
   each migration still needs its own ordering and outcome review.
4. Reassess the permanent conceptual cost of an `Effect.adminAssigned` flag, a persisted
   `Task.initialAssignee`, and card-classification handling once the on-turn model is known. The
   identities are genuinely distinct in the prototype, but this may not be the smallest final
   representation.

`work1` has concurrent Agent, decision-log, and task-delegation edits. Inspect both diffs before
restoring or merging this stash; do not treat the stashed Agent implementation as final.
