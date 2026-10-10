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
| Compiled card immediate effects, actions, effects, and supporting rules | Always add OWN. |
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
  visible. A named EACH or RANK selector can provide a new Me.
- [PetElaborator](../../src/common/dev/martianzoo/pets/PetElaborator.kt) applies ordinary defaults
  before transforms, records Type-variable scopes after structural rewrites, and expands properties.
  Every marked EVAL initially defers its transform, including properties that can expand in the
  next step. Expansion preserves composition order and the captured Me, then resumes the ordinary
  dispatcher. There is no separate ownership pass or OWN-specific property branch.
- [LiveEffect](../../src/common/dev/martianzoo/engine/LiveEffect.kt) consumes the resulting trigger;
  it no longer adds actor filters during execution.

`Owned<Me@Owner>` supplies an inherited lexical name for its owner dependency; subclasses may
narrow it to Me@Player. An input producer may supply Me directly, and a named EACH/RANK selector
may shadow it. Merely being an Owner does not bind Me to This. Explicit recipients remain explicit
through all these scopes.

Whole-effect OWN still contains a conditional rule. Without a visible Me, `needsMe` scans the body.
If it needs a binding, OWN captures an omitted owner in an owned trigger, or introduces
`BY Me@Player` for an unowned non-System trigger. A System event without an owned recipient cannot
supply a Player Actor and is rejected. OR requires an explicit binding around the whole trigger.
If the body supplies all its recipients locally, the trigger can remain global. This is a retained
compromise, not the desired final simplification.

## Things that need human review

These are choices already present in the implementation, not merely future suggestions. Test
coverage does not establish that the user prefers them; concerns based only on inspection are
identified explicitly.

1. **The producer contract is less uniform than “every string gets OWN.”** Player and Admin Agents
   share an interface but add marks differently. Card requirements, invariants, and auto-selection
   predicates differ from card effects. These distinctions follow the fields' contexts, but the
   user may prefer separate entry points or a more uniform contract.
2. **A body's contents still affect its subscription.** In an ownerless class,
   `OWN[Pulse: Prize<Player1>]` can hear every Pulse. Adding bare `Plant` makes the effect need Me,
   so it gains `BY Me@Player` and stops hearing Admin's Pulse. Keeping `needsMe` preserved the first
   migration's behavior while reaching a tested stopping point. It should not become an unnoticed
   permanent rule.
3. **OWN also means actor filtering.** It is more than filling a dependency argument. With a
   non-Player Owner, the new static BY can name an owner that cannot act, making the subscription
   unable to fire; the old runtime filter constrained only Player owners. No affected Canon rule
   is known. This consequence was identified by inspection and still needs an executable scenario
   and a decision about intended meaning.
4. **HAS EVAL is conservatively restricted.** In a HAS-candidate position,
   `EACH Starter@Player(HAS EVAL Rule.requirement) { ... }` is rejected. Expanding a property there
   loses the positional candidate for defaults as well as OWN. Inline `HAS StartToken` works, as
   does EVAL inside RANK comparison keys. The rejection also excludes properties that would not
   actually need the candidate. It is a deliberate temporary restriction.
5. **Inside-out dispatch limits inferred bindings.** In an ownerless rule,
   `OWN[Pulse: PROD[OWN[Plant]]]` fails because the inner OWN runs before the outer application
   supplies Me. `OWN[Pulse: PROD[Plant]]` works. Explicitly binding and using Me also works, for
   example `OWN[Pulse BY Me@Player: PROD[OWN[Plant<Me@Player>]]]`. A preparatory handler phase was
   deliberately avoided; review whether this visible ordering is an acceptable authoring rule.
6. **Named selectors bind Me in their own refinements.** EACH and RANK now agree: `Me@Player` is
   visible in the selector's refinement and its body or comparison metrics. The previous traversal
   used different scopes for EACH and RANK refinements. The distinction matters for owned types
   nested inside arguments; direct HAS candidate binding often hides it.
7. **PROD transforms property values and allows unchanged metric factors.** A Plant count supplied
   through EVAL inside PROD now counts plant production, matching inline syntax. Previously that
   property value escaped PROD. Also, `PROD[CityTile]` as a metric is accepted and unchanged: a
   deferred scale factor need not contain a resource. Nonresource gains and costs remain invalid.

The larger qualification is architectural: Own still recognizes Owned, the owner dependency,
System, and Me. Deferred EVAL still has a dedicated Me field, and shared scope carries internal
represented-Class marker identities. Task assignment and passive-owner applicability still treat
ownership specially. Explicit syntax makes these rules inspectable; it has not yet removed them.

## Next steps

### 1. Remove the body scan after auditing affected Canon rules

**Recommended next change, not yet implemented:** whole-effect OWN without a visible Me always
supplies its trigger binding. Global subscriptions instead stay unmarked, mark only their
instructions, or spell out a binding. This would remove `needsMe` and the surprising relationship
between adding a result and changing the subscription.

Before changing the rule, temporarily make the existing no-binding shortcut report every hit.
Enumerate effects for every Canon class, including generated cards, maps, and inline classes;
searching authored OWN lines is insufficient. For each hit, choose the intended scope explicitly.
Check these semantic traps:

- Adding BY Player excludes Admin events.
- Capturing an owned trigger through Me@Player can exclude Neutral and SoloOpponent recipients.
- A global `IF 3 CityTile` can turn into a count of that player's cities.
- OR triggers and ownerless System triggers may require a different spelling.

Remove the temporary audit and the scan only after migrating those cases. Keep forward references
to represented-Class declarations in the test coverage; the old scan also collected marker facts.

### 2. Design explicit refinement-candidate references

**Selected direction; syntax unsettled:** let a refinement name its candidate, as recorded in the
[Pets roadmap](../../PETS_ROADMAP.md#language-and-modeling). Use examples with another player's
StartToken, nested dependencies, shared requirement properties, and award ranking. The design
should replace L9-9's positional rule and both candidate stacks in defaults and OWN. Adding a new
candidate field to transform scope did not solve property expansion for defaults and was abandoned.

### 3. Repair represented-Class role tracking

**Known concern from code inspection, not yet reproduced in a test:** with
`PROD[@StandardResource]: OWN[@StandardResource]`, an instruction-level mark can see the trigger's
old component-choice role rather than the Class-selection role produced by PROD. Whole-effect OWN
sees the rewritten trigger. No current Canon occurrence is known.

Reproduce the difference before selecting a fix. **Tentative:** record the role on the declaration
that a transform changes, instead of reconstructing it in another pass. Retain access to header and
trigger declarations outside an instruction's mark; that is why simply deleting
`representedClassMarkers` is insufficient.

### 4. Settle filtering and then audit the remaining engine special cases

Write a scenario for a non-Player Owner watching an unowned non-System event, then choose the
intended rule with the human. **Tentative alternatives:** OWN could always mean “performed by this
owner,” with a diagnostic for an impossible actor, or actor filtering could become a separate
explicit part of the rule. Do not restore a hidden runtime filter to preserve an accidental result.

Next inventory ownership-sensitive task assignment and passive-owner applicability. Distinguish
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
