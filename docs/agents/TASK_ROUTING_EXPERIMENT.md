# Admin routing and turn-state questions

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** considering Admin-assigned internal work, the meaning of an on-turn Player, or
> Player → Admin → Player task chains.
>
> **Status:** direct `System` gain tasks are assigned to Admin at admission. Removal routing,
> broader housekeeping classification, and scheduling remain open. Current task semantics are in
> [IDENTITY.md](IDENTITY.md); scheduling alternatives are in
> [SEQUENCING.md](SEQUENCING.md#delegated-operations-and-scheduling-options).

## Current System gain routing

When a normalized task directly gains a subtype of `System`, alone or under a top-level
instruction-side `BY`, the task begins assigned to Admin before its add event is recorded.
Contextual selection reapplies the assignment before a concrete `BY` performs its authoritative
handoff. A non-Admin performer then fails the existing `System` creation guard instead of being
silently replaced by Admin.

The task retains its original controller and selection assignee. Its continuation therefore
returns with both identities intact. For queued effects of the System gain, the effect component's
Player owner and changed component's Player owner still take precedence; an unowned effect of an
unowned System gain falls back to the retained selection assignee rather than Admin, who merely
performed the internal gain. Automatic effects retain their ordinary Actor rule.

This rule changes neither presentation nor scheduling. `Hidden` remains the independent
presentation classification. Admin's Agent policy determines whether an Admin-assigned task runs
without an explicit command; assignment alone supplies no priority or completion guarantee.

The rule currently covers gains, matching `System`'s existing Admin-only creation contract. Apply
it to removals only after auditing current System removals for Player decisions or timing choices.

## What assignment must preserve

A Player's pending work can include fixed outcomes whose timing the Player may choose. Whether an
instruction offers a narrowing choice therefore does not decide whether Admin should receive it.
The useful distinction is whether choosing when to perform it belongs to the surface game. Printed
tags installing themselves are an example of internal work; that does not establish that every
other fixed consequence has the same status.

Routing an internal step through Admin must preserve both the controller of its surrounding
operation and its selection recipient. They can differ when P1's operation triggers P2's effect:
the surrounding operation remains P1's, while an unowned System wrapper produced by that effect
must return its downstream choice to P2. Replacing either identity with the on-turn Player would
lose that distinction. Explicit requests for a named Player must also work during phases without
an exclusive turn.

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
- Should removing a `System` Component receive the same immediate Admin assignment as gaining it?
- Which Actor should each queued rule record as its selection assignee, distinct from its
  controller and any later instruction-side `BY` assignee?
- How should Player → Admin → Player identity continue through automatic effects that themselves
  create further queued reactions?
- Which phases have an on-turn Player? Setup and Research currently permit simultaneous Player
  work; sequential turn state cannot replace explicit assignment there.
- What contract, if any, should ordinary Agent calls guarantee about outstanding Admin work?

Answer these from concrete game operations before adding an assignment flag, a turn component, or
a mandatory autoexecution policy. None of those mechanisms alone provides the lifetime of a
delegated payment operation.
