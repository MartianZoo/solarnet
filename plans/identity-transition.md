# Identity and Control Transition

## Purpose

This plan resumes the cross-owner task handoff deliberately deferred by the earlier identity
cleanup. Preserve its settled Actor, Owner, Player, Engine, Opponent, assignee, and authored `BY`
distinctions while adding the minimum queue-control model needed by Philares and native workflow.

The immediate goal is not a generalized identity framework. It is to distinguish two concrete
operations that had previously been conflated:

1. delegate one task to another Actor and then return; and
2. transfer control to another Actor until that Actor's queue drains.

## Carried-forward identity rules

- **Actor** is who executes an instruction and is recorded as `BY` on resulting state changes.
- **Owner** says whose components they are. Changing another Owner's components does not make that
  Owner the Actor.
- **Player** is both an Actor and an Owner. **Engine** is an Actor but not an Owner. **Opponent** is
  an Owner but not an Actor.
- Trigger-side `BY` filters the Actor of the triggering change. Instruction-side `BY`, described
  below, instead says which Actor performs and narrows that instruction. Neither use selects an
  Owner merely because that Owner's components are affected.
- Resolving an active component effect binds its contextual `Owner` from the component carrying the
  effect. Later execution by another Actor must not rebind those already resolved types.
- A task's queue determines who may select and narrow that task. Queue membership must not be
  inferred merely from the Owner appearing in its instruction.

Mons Insurance, Crash Site Cleanup, Protected Habitats, and similar rules distinguish the Actor
who caused a change from the Owner affected by it. If Player1 removes Player2's resources or
production, the change is `BY Player1`. The same principle explains Helion: another Actor removing
Helion's megacredits is not Helion paying, so Helion has no opportunity to substitute heat.

## Ordinary triggered work

Triggered work normally remains part of the surrounding operation and goes to that operation's
control queue, even when the effect-bearing component and every affected component belong to
another Player. Executing one inner instruction `BY` another Actor does not move the surrounding
operation or its later consequences into that Actor's queue.

For example, if Player1's city placement triggers Player2's Rover Construction, the resulting
concrete `2 Megacredit<Player2>!` can remain in Player1's queue and execute `BY Player1`, `VIA` the
Player2-owned card. Player1 controls its timing, and no decision belonging to Player2 is involved.

The current Effector behavior that routes triggered work to the effect Owner is therefore only a
compatibility behavior. Effect ownership supplies contextual Owner binding, which an explicit
instruction-side `BY Owner` may use; ownership alone does not transfer control of the operation.

## Instruction-side `BY` and one-task delegation

Philares needs to say explicitly that its Owner performs and narrows one instruction:

```pets
StandardResource<Player2>! BY Player2
```

The instruction-side `BY` wrapper is concrete enough to execute when its Actor is concrete. Its
inner instruction may remain abstract because executing the wrapper creates or exposes that inner
instruction as a task for the named Actor rather than narrowing it in the current scope.

This has one semantic meaning: assign authority over exactly the wrapped instruction to the named
Actor. Its queue behavior depends on whether that Actor already controls the operation:

- If the named Actor already owns the currently actionable queue, put the inner task in that same
  queue. Do not suspend a queue against itself.
- Otherwise suspend the current control queue, even if it contains no other tasks; put exactly the
  inner task in the named Actor's queue; and resume the suspended queue when that task's handling is
  complete.
- In either case the named Actor narrows and executes the inner task, and its changes are `BY` that
  Actor.
- Give the inner task the original triggering cause, so Philares appears directly as its `VIA`
  cause rather than through an intermediate delegation task.

Delegation does not transfer the surrounding operation. Consequences sparked while Player2
executes the delegated task still return to Player1's suspended control queue by default. Those
consequences may themselves explicitly delegate individual choices. This preserves Player1's right
to order the rest of the consequences of Player1's action.

Delegation should be authored explicitly with instruction-side `BY`. An abstract instruction is
evidence that Philares needs it, but abstractness itself must not infer who owns a choice. The test
for the local case is the currently actionable queue, not the active player: an active player's
queue may itself be suspended while another Actor handles delegated work.

## Trigger specialization and Splice

Matching a concrete change may specialize abstract class names appearing in the trigger. Those
specializations are bindings for the entire effect, including its instruction-side `BY` Actor. The
same abstract class name must receive the same specialization at every linked occurrence; a match
that would bind it inconsistently is not valid. This is analogous to the linked occurrences of `X`
within a `THEN` instruction.

This linking does not prevent independent narrowing of otherwise unbound instructions split by a
`Multi`; for example, the two bare occurrences in `OceanTile, OceanTile` may still select different
areas. A binding already established by the trigger remains binding inside every branch or `Multi`
element.

Splice should therefore be expressible directly, without manufacturing one watcher per Player:

```pets
MicrobeTag<CardFront, Anyone>:
    (2 Megacredit<Anyone> OR Microbe<CardFront, Anyone>) BY Anyone
```

For a Microbe tag gained on Player1's Tardigrades, trigger specialization binds `Anyone` to Player1
and `CardFront` to that concrete Tardigrades card. Player1 already controls the operation, so the
instruction-side `BY Player1` establishes Player1's choice authority without suspending the queue.

Trigger specialization is fallible. A card can have a Microbe tag without being able to hold
Microbes, so substituting that card into `Microbe<CardFront, Anyone>` may form an invalid type. An
invalid specialized type makes its smallest enclosing atomic instruction `Die`, not `Ok`; ordinary
instruction simplification then prunes `A OR Die` to `A`, while a sequence containing a mandatory
`Die` remains impossible. Thus Splice gracefully leaves only the megacredit choice for such a card.

## Transferring control until a queue drains

Native workflow needs a different operation. When Engine grants Player1 an action turn, Engine is
not asking Player1 to refine one isolated task; it is yielding control of the operation until
Player1's queue has completely drained.

Provisionally, this control-transfer operation must:

1. leave Engine's continuation task in Engine's suspended queue;
2. make Player1's queue the current control queue;
3. route consequences of Player1's work back into Player1's queue, including newly triggered work;
4. allow nested one-task delegation, which temporarily suspends Player1 and then returns to it; and
5. resume Engine only when Player1's queue is empty and no nested suspension is outstanding.

This is suitable for work such as:

```pets
UseStandardAction<Player1>! OR Pass<Player1>!
```

The final Pets spelling of control transfer is intentionally undecided. It must not be represented
as ordinary instruction-side `BY`, because their consequence-routing and completion boundaries are
different.

## Queue suspension semantics

- Suspension belongs to a queue, not an individual task.
- A suspended queue retains its tasks for inspection, rollback, and eventual resumption, but none
  of those tasks is currently actionable.
- A suspended queue suppresses outward empty/drained notification even when it physically contains
  no tasks. In particular, workflow must not advance while another Actor is handling work on behalf
  of that queue.
- Nested suspension must have an explicit parent/child relationship so completion resumes the
  correct queue. Suspension cycles are invalid.
- The engine should be able to identify the Actor or Actors on whom progress is currently blocked
  from the non-suspended actionable queues and the suspension relationships.

This model may eventually offer a replacement for the prepared-task global lock by suspending
other queues, but changing preparation is not part of the first experiment.

## First experiment

1. Add focused characterization for the contrast between Rover Construction and Philares when the
   same Player1 placement triggers both Player2-owned cards. Protect Owner resolution, cause,
   queue, narrowing authority, and resulting Actor attribution.
2. Add queue suspension and one-task delegation only far enough to implement the Philares rule.
   Remove the compatibility routing that directly assigns Philares's initial triggered task to its
   Owner once the new behavior is protected.
3. Express Splice with trigger specialization and instruction-side `BY`. Cover both a Microbe
   resource card and a Microbe-tagged card that cannot hold Microbes, including `OR Die` pruning.
4. Exercise nested consequences from the delegated Philares task and verify that they return to
   the suspended parent queue unless they explicitly delegate again.
5. Then prototype one Engine-to-Player control-until-drain handoff. Do not yet redesign prepared
   tasks or compile the complete phase graph.

## Open decisions

- Choose the final Pets syntax and name for control-until-drain.
- Decide how the current control queue is represented internally. Prefer deriving it from queue
  suspension relationships or a small execution frame rather than adding another identity field
  to every task.
- Define precisely which task events are visible for instruction-side `BY` while keeping the inner
  task's gameplay cause direct.
- Define how trigger bindings retain their authored variable identity through type resolution;
  current class-name substitution is in the right vicinity but must not normalize away `Anyone`
  or silently collapse inconsistent bindings.
