# Explicit ownership and ordinary transforms

> **Agent information:** This note records the ownership transition's intended design, current
> implementation, and unresolved choices. It supersedes the temporary review report.
>
> **Read when:** changing OWN, lexical Me, ownership insertion, transform composition, property
> capture, or the card/map/input contracts that add ownership marks.
>
> **Status:** explicit marks and the first Canon migration are implemented. Making Owned and Owner
> ordinary throughout the engine remains unfinished. Items marked **tentative** are ideas to
> evaluate, not approved implementation work.

## The intended result

The author should be able to predict ownership from the written rule and its producer's contract.
Raw `Plant` does not acquire an owner merely because an engine receives it. `OWN[Plant]` requests
an explicit transformation using the visible `Me`. A card compiler may add that mark, provided its
contract says so. Adding `OWN[Heat]` elsewhere in a string must never change how Plant is treated.

The user has selected these requirements:

- Each receiving API has a fixed policy for adding OWN. It never examines the string to decide
  whether an existing mark disables wrapping. The current policy distinctions need review below.
- Unmarked Pets receives no implicit owner insertion or implicit trigger actor filter. Ordinary
  declared defaults, including the card-location default `<Hand>`, remain in effect.
- OWN and PROD use ordinary transform handling, including when properties expand later. There must
  not be an OWN-specific repair hidden in property evaluation or execution.
- Whole-effect OWN may supply a missing player binding from its trigger. This permission does not
  authorize unmarked rules to borrow a player from the event.
- Runtime code may require a concrete Effect and reject an unresolved enclosing transform.
  Source-copying code must preserve authored marks.
- All owners use ordinary effect specialization. Rules that can belong to non-Players explicitly
  select a Player before producing Player-only results; city and greenery scoring stays on the tiles.

Ownership, recipient, and performing Actor remain distinct. For example,
`Placement<This, @Player>: Plant<@Player>` carries a recipient explicitly. An unowned, non-System
event could instead use `Placement<This> BY @Player: Plant<@Player>`, which captures its performing
Actor. Current Canon Placement is Owned and System: Admin performs the event, while its dependency
identifies the player receiving the bonus. Changing that model requires choosing which relationship
the event should carry; removing Owned alone would lose information. Task control and event
attribution are described in [IDENTITY](IDENTITY.md).

## What is implemented

### Producers and source preservation

The current contracts are defined on [Agent](../../src/common/dev/martianzoo/agent/Agent.kt) and
[CardDefinition](../../src/common/dev/martianzoo/tfm/carddata/CardDefinition.kt):

| Source | Current policy |
| --- | --- |
| Player Agent input | Always wrap the entire input, including text already containing OWN. |
| Admin Agent or direct PetElaborator input | Never add a mark; explicit marks still apply. |
| Compiled card direct effects, actions, effects, and supporting rules | Always add OWN. |
| Card requirement property | Keep literal; PlayCard applies OWN at its use. |
| Card static invariants and auto-selection predicates | Keep literal, without implicit player scope. |
| Generated map placement rules | Add OWN. Handwritten Canon marks the rules that need it. |

These policies depend on the producer or field, never on the presence of a substring. Thus a Player
Agent receiving `Plant, OWN[Heat]` processes `OWN[Plant, OWN[Heat]]`. Shared property definitions may
remain literal: `OWN[EVAL Shared.score]` transforms that use, while plain `EVAL Shared.score` does
not add ownership. A property can itself contain OWN when every use should request it.

`EffectTree` and `ActionTree` represent authored syntax, including enclosing marks. Existing
`Effect` and `Action` remain the concrete shapes. `untransformed` is for inspecting written
metadata. `instructionWithTransforms()` preserves marks when extracting an instruction; its caller
must supply bindings formerly supplied by the removed trigger. Inline-class extraction and
production-box copying also preserve enclosing transform order.

### Rewriting and scope

[Pets sections 8–9](../pets-language-spec.md#8-transform-blocks) own the language rules. The
implementation responsibilities are:

- [TransformHandler](../../src/common/dev/martianzoo/pets/TransformHandler.kt) dispatches inside-out.
  Its shared Rewriter supplies lexical context and retains marks around EVAL for later expansion.
  Both OWN and PROD use it. Repeated OWN coalesces only when no different transform intervenes;
  nested PROD is rejected.
- [Own](../../src/common/dev/martianzoo/pets/Own.kt) fills omitted owner dependencies from Me,
  respecting explicit recipients, Anyone, linked dependencies, HAS candidates, and whole-Type
  references. It also emits explicit BY filters for unowned, non-System subscriptions when Me is
  visible. A named EACH or RANK selector can provide a new Me. TASK, LATER, and FINALLY provide their
  recipient as Me inside their bodies; OWN uses that recipient without changing unmarked bodies.
- [PetElaborator](../../src/common/dev/martianzoo/pets/PetElaborator.kt) applies ordinary defaults
  before transforms, records Type-variable scopes after structural rewrites, and expands properties.
  Every marked EVAL initially defers its transform, including properties that can expand in the
  next step. Expansion preserves composition order and the captured Me, then resumes the ordinary
  dispatcher. There is no separate ownership pass or OWN-specific property branch.
- [LiveEffect](../../src/common/dev/martianzoo/engine/LiveEffect.kt) consumes the resulting trigger;
  it no longer adds actor filters during execution or skips invalid effects for passive owners.

`Owned<Me@Owner>` supplies an inherited lexical name for its owner dependency; subclasses may
narrow it to Me@Player. An input producer may supply Me directly, and a named EACH/RANK selector
may shadow it. Merely being an Owner does not bind Me to This. Explicit recipients remain explicit
through all these scopes.

Whole-effect OWN always supplies a binding when Me is not visible. It captures an omitted owner
in an owned trigger, or introduces
`BY Me@Player` for an unowned non-System trigger. A System event without an owned recipient cannot
supply a Player Actor and is rejected. OR requires an explicit binding around the whole trigger.
The instruction's contents do not select the subscription. Global rules stay unmarked or mark only
their instructions, for example `SetupPhase: OWN[EACH Me@Player { TradeFleet }]`. Named trigger
bindings may be unused in the result, so this rule needs no scan for an omitted owner.

`Component.owner` reads only its `Owned` dependency; being an `Owner` no longer makes a component
its own owner. Player-Agent input still supplies that Player as its explicit elaboration scope.

City and greenery scoring stays on each tile. `EACH Scorer@Player(HAS This)` selects its Player
owner before awarding points; a SoloOpponent tile selects nobody. City points still count adjacent
greenery owned by anyone. Turmoil chairman and party-leader scoring uses the same selection.
Influence triggers explicitly bind a Player who holds the relevant chairman, leader, or delegate
component, so Neutral never becomes an invalid Player argument. These are ordinary Pets queries;
`LiveEffect` specializes and validates every component through the same path. See
[TileScoringTest](../../test/common/dev/martianzoo/tfm/tests/rules/TileScoringTest.kt) and the scoring
and influence scenarios in
[TurmoilRulesTest](../../test/common/dev/martianzoo/tfm/tests/rules/TurmoilRulesTest.kt).

## Things that need human review

These are choices already present in the implementation, not merely future suggestions. Test
coverage does not establish that the user prefers them; concerns based only on inspection are
identified explicitly.

1. **The producer contract is less uniform than “every string gets OWN.”** Player and Admin Agents
   share an interface but add marks differently. Card requirements, invariants, and auto-selection
   predicates differ from card effects. These distinctions follow the fields' contexts, but the
   user may prefer separate entry points or a more uniform contract.
2. **OWN also means actor filtering.** It is more than filling a dependency argument. With a
   non-Player Owner, the new static BY can name an owner that cannot act, making the subscription
   unable to fire; the old runtime filter constrained only Player owners. No affected Canon rule
   is known. This consequence was identified by inspection and still needs an executable scenario
   and a decision about intended meaning.
3. **HAS EVAL is conservatively restricted.** In a HAS-candidate position,
   `EACH Starter@Player(HAS EVAL Rule.requirement) { ... }` is rejected. Expanding a property there
   loses the positional candidate for defaults as well as OWN. Inline `HAS StartToken` works, as
   does EVAL inside RANK comparison keys. The rejection also excludes properties that would not
   actually need the candidate. It is a deliberate temporary restriction.
4. **Inside-out dispatch limits inferred bindings.** In an ownerless rule,
   `OWN[Pulse: PROD[OWN[Plant]]]` fails because the inner OWN runs before the outer application
   supplies Me. `OWN[Pulse: PROD[Plant]]` works. Explicitly binding and using Me also works, for
   example `OWN[Pulse BY Me@Player: PROD[OWN[Plant<Me@Player>]]]`. A preparatory handler phase was
   deliberately avoided; review whether this visible ordering is an acceptable authoring rule.
5. **Named selectors bind Me in their own refinements.** EACH and RANK now agree: `Me@Player` is
   visible in the selector's refinement and its body or comparison metrics. The previous traversal
   used different scopes for EACH and RANK refinements. The distinction matters for owned types
   nested inside arguments; direct HAS candidate binding often hides it.
6. **PROD transforms property values and allows unchanged metric factors.** A Plant count supplied
   through EVAL inside PROD now counts plant production, matching inline syntax. Previously that
   property value escaped PROD. Also, `PROD[CityTile]` as a metric is accepted and unchanged: a
   deferred scale factor need not contain a resource. Nonresource gains and costs remain invalid.

The larger qualification is architectural: Own still recognizes Owned, the owner dependency,
System, and Me. Deferred EVAL still has a dedicated Me field, and shared scope carries internal
represented-Class marker identities. Task assignment still treats ownership specially. Explicit
syntax makes these remaining rules inspectable; it has not yet removed them.

## Next steps

### 1. Design explicit refinement-candidate references

**Selected direction; syntax unsettled:** let a refinement name its candidate, as recorded in the
[Pets roadmap](../../PETS_ROADMAP.md#language-and-modeling). Use examples with another player's
StartToken, nested dependencies, shared requirement properties, and award ranking. The design
should replace L9-9's positional rule and both candidate stacks in defaults and OWN. Adding a new
candidate field to transform scope did not solve property expansion for defaults and was abandoned.

### 2. Repair represented-Class role tracking

**Known concern from code inspection, not yet reproduced in a test:** with
`PROD[@StandardResource]: OWN[@StandardResource]`, an instruction-level mark can see the trigger's
old component-choice role rather than the Class-selection role produced by PROD. Whole-effect OWN
sees the rewritten trigger. No current Canon occurrence is known.

A separate reproduced case uses a represented Class from a class header inside a locally bound
fanout: `OWN[Resolve: EACH Me@Player { @GlobalEffect }]` attempts to transform the header variable
metadata without an outer Me. The current `PendingGlobalEffect` instead marks the gain inside the
fanout: `ResolveGlobalEvent: EACH Me@Player { OWN[@GlobalEffect] }`. Investigate the outer mark's
metadata traversal without changing that explicit local spelling merely for uniformity.

Reproduce the difference before selecting a fix. **Tentative:** record the role on the declaration
that a transform changes, instead of reconstructing it in another pass. Retain access to header and
trigger declarations outside an instruction's mark; that is why simply deleting
`representedClassMarkers` is insufficient.

### 3. Remove the remaining engine ownership special cases

Write a scenario for a non-Player Owner watching an unowned non-System event, then choose the
intended rule with the human. **Tentative alternatives:** OWN could always mean “performed by this
owner,” with a diagnostic for an impossible actor, or actor filtering could become a separate
explicit part of the rule. Do not restore a hidden runtime filter to preserve an accidental result.

Also inventory ownership-sensitive task assignment. Distinguish
real ownership from components made Owned only to carry a player through Admin work. Any replacement
must preserve recipients, delegated choices, event attribution, and phases without a unique player;
coordinate those decisions with [IDENTITY](IDENTITY.md), rather than guessing a current player.

## Other tentative ideas

- **General lexical capture instead of Eval.me.** This might make deferred properties less special.
  Pursue it only if it replaces the dedicated field and existing scope machinery; a parallel scope
  representation would make the explanation worse.
- **Express OWN through more ordinary operations.** A general way to fill selected dependencies,
  combined with explicit trigger binding/filtering, might eventually remove hardcoded knowledge
  of Owned or System. No satisfactory formulation has been selected. Demonstrate that it expresses
  both OWN and PROD without a new family of special hooks before adopting it.
- **Remove Owned from events that only carry a recipient.** Explicit dependencies or BY can express
  some placement-like rules. Recipient and Actor are not interchangeable; the example above shows
  why this is a modeling question rather than a mechanical rewrite.
- **Give expressions a transform form.** Expression-only APIs currently adapt through a count or
  requirement to apply OWN. A new Expression transform family could remove those adapters, but
  would expand parsing and consumer obligations. This is low priority until it removes more
  complexity than it adds.

## Evidence and review discipline

The first migration passed the full repository build, including required browser checks and replay.
That is a stopping point, not proof that the choices above are the final design. Use
[OwnTransformTest](../../test/common/dev/martianzoo/engine/OwnTransformTest.kt) for observable
ownership, actor, and deferred-property scenarios;
[Lang08TransformsTest](../../test/common/dev/martianzoo/pets/Lang08TransformsTest.kt) and
[TransformHandlerTest](../../test/common/dev/martianzoo/pets/TransformHandlerTest.kt) for composition;
and [Lang09ElaborationTest](../../test/common/dev/martianzoo/pets/Lang09ElaborationTest.kt) for lexical
scope and diagnostics. Source copying and inherited scope also have language/type tests.

For each revision, check the intended-result requirements above, update the owning language rule or
producer contract, and remove superseded machinery. Preserve transform order across property
expansion, marks when copying, explicit recipients, and the distinction between raw and marked
inputs. Use the [testing procedure](TESTING.md) and full gameplay/replay checks when the Canon audit
or shared elaboration changes warrant them. Keep this note focused on the design and outstanding
decisions; it need not retain review transcripts or a chronological work log.
