# Admin routing and turn-state questions

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** considering Admin-assigned internal work, the meaning of an on-turn Player, or
> Player → Admin → Player task chains.
>
> **Status:** research questions and useful constraints. Current task semantics are in
> [IDENTITY.md](IDENTITY.md); scheduling alternatives are in
> [SEQUENCING.md](SEQUENCING.md#delegated-operations-and-scheduling-options). No routing syntax or
> implementation sequence is selected here.

## What assignment must preserve

A Player's pending work can include fixed outcomes whose timing the Player may choose. Whether an
instruction offers a narrowing choice therefore does not decide whether Admin should receive it.
The useful distinction is whether choosing when to perform it belongs to the surface game. Printed
tags installing themselves are an example of internal work; that does not establish that every
other fixed consequence has the same status.

Routing an internal step through Admin must preserve the controller of its surrounding operation.
A later Player choice should return to that controller, who may be P2 during a delegated response
on P1's turn. Replacing controller propagation with the on-turn Player would lose that distinction.
Explicit requests for a named Player must also work during phases without an exclusive turn.

Controller, selection recipient, and current assignment remain different facts. Trigger-side `BY`
matches event Actors. Instruction-side `BY` instead transfers a concrete queued task to the named
Actor, whose engine performs it. An unowned attack watcher, for example, must not assign work to the
victim merely because the changed resource belongs to them.

## Timing and automatic execution

Admin assignment does not establish priority, completion, or freedom from strategic choices.
An EAGER Admin policy can reduce visible internal work, but cannot repair missing scheduling rules
or prove that every legal Admin ordering has the same result. A stalled or abstract Admin task also
needs an honest disposition; changing a policy default does not establish that ordinary Player calls
always return with internal work settled.

Replacing `::` with queued Admin work can change what reactions observe. Card-entry listeners such
as `EventCard(HAS SpaceTag)` in
[`cards.json5`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/cards.json5) require printed
tags to exist when the card-entry event is matched.
Queued tag installation would be too late under the current model. This is a concrete timing
constraint, not a claim that tag installation must always use automatic effects under every future
card representation.

## Open choices

- Which fixed effects belong to the Player's ordering choices, and which are internal settlement?
- Which Actor should each queued rule record as its selection assignee, distinct from its
  controller and any later instruction-side `BY` assignee?
- How does Player → Admin → Player work preserve the active delegated controller through splitting,
  continuations, and new reactions?
- Which phases have an on-turn Player? Setup and Research currently permit simultaneous Player
  work; sequential turn state cannot replace explicit assignment there.
- What contract, if any, should ordinary Agent calls guarantee about outstanding Admin work?

Answer these from concrete game operations before adding an assignment flag, a turn component, or
a mandatory autoexecution policy. None of those mechanisms alone provides the lifetime of a
delegated payment operation.
