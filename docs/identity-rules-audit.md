# Identity rules audit

This audit settles the identity inputs to effect matching before the runtime Owner model is
broadened. It is normative for the identity-model work; ongoing implementation planning remains in
`TODO.md`.

The key finding is that `BY` describes the Actor that performed the triggering `ChangeEvent`. It
does not choose the recipient, controller, or Actor of the consequence. Those facts often coincide,
but Philares, Lakefront Resorts, and World Government Terraforming prove that they are independent.

## Rules cases

`--` means that the changed component is unowned. "Effect Owner" is the Owner of the component
carrying the effect, not necessarily the owner of a component named by its trigger.

| Case | Performer | Effect Owner | Changed Owner | Reward recipient / Owner scope | Controller | Resulting Actor |
|---|---|---|---|---|---|---|
| Aphrodite on a Venus step | Player or Admin | Aphrodite Player | -- | Aphrodite Player | Aphrodite Player | Aphrodite Player |
| Arctic Algae on an ocean | Player or Admin | Arctic Algae Player | -- | Arctic Algae Player | Arctic Algae Player | Arctic Algae Player |
| Philares adjacency | Player placing the new tile | Philares Player | --; the adjacency names both tile Owners | Philares Player | Philares Player | Philares Player |
| Lakefront ocean effect | Player or Admin | Lakefront Player | -- | Lakefront Player | Lakefront Player | Lakefront Player |
| Lakefront adjacency bonus | Lakefront Player only | Lakefront Player | -- | Lakefront Player | Lakefront Player | Lakefront Player |
| Global-parameter TR | Player, or Admin during World Government | -- | -- | performing Player; none for Admin | same Player, if any | same Player, if any |
| Ordinary ocean-adjacency money | Player placing the tile, or Admin | -- | -- | performing Player; none for Admin | same Player, if any | same Player, if any |
| Area placement bonus | Player placing the tile, or Admin | -- | -- | performing Player; none for Admin | same Player, if any | same Player, if any |
| World Government parameter/track bonus | Admin | -- | -- | none for Owner-only rewards | selected Player | Admin |

Aphrodite, Arctic Algae, and Lakefront's first effect use `BY Anyone`: Admin therefore still
triggers them, but their output is scoped to the card's Owner. Philares similarly uses
`BY Anyone`; the adjacency's tile Owners decide whether its trigger matches, while the triggering
performer does not receive its reward or task.

Lakefront's second effect is different. `BY Owner` requires the Lakefront Player to have performed
the placement. Routing the consequence to that Player cannot make another performer's placement
qualify.

Global-parameter TR, ocean-adjacency money, and map placement bonuses are unowned effects whose
output uses contextual `Owner`. They resolve that Owner from the performer. Admin has no Owner
capability, so World Government changes intentionally produce no such reward. Track bonuses that
produce further neutral terraforming work still occur and remain Admin work.

## Language rules

1. `BY` is a predicate on `ChangeEvent.actor`, the performer of the triggering change.
2. `BY Anyone` accepts every Actor, including Admin. Here `Anyone` is card grammar for an
   unrestricted performer; it is not the ownership supertype used by `Owned<Anyone>`.
3. `BY Player` accepts any Player performer.
4. On an effect belonging to an Owned component, `BY Owner` accepts only a performer equal to that
   effect Owner. After class-effect specialization this may be represented as `BY Player2`, etc.
5. On an unowned effect that produces contextual Owner output, `BY Owner` requires an Owner-capable
   performer and binds the output to that identity. Today the only runtime identities with both
   capabilities are Players; the generalized runtime Owner representation must preserve this rule.
   A component that is itself an Owner, such as `Player1`, already supplies that context; its phase
   and setup effects do not acquire a synthetic performer restriction.
6. `BY SomeIdentity` compares the performer with that configured identity. Matching must eventually
   use configured identity lookup rather than `Actor.from` or name patterns.
7. An omitted `BY` adds no separate performer predicate. The trigger's component expression may
   already constrain an Owner, and unowned effects with contextual Owner output acquire the
   Owner-performer requirement described above. This is necessary for administrative signals such
   as `ProductionPhase`: the signal is not performed by every Player whose production effect fires.
8. A future complement spelling such as `BY !Owner` or `BY !Player1` should negate the corresponding
   performer predicate. It must not negate the consequence recipient or Task Actor. The current
   grammar accepts only a class name after `BY`, so complement support remains future work.

The original [`BY` design](https://github.com/MartianZoo/solarnet/issues/7) correctly identifies
the red outline as the explicit `BY Anyone` spelling and uses Lakefront, Philares, Aphrodite, and
Arctic Algae as the motivating cases. Its proposed implicit `BY Me` is captured where a rule
actually needs performer identity: through an explicit/specialized `BY Owner`, or through
contextual Owner output on an unowned effect. Treating every omitted `BY` as a performer filter
would incorrectly suppress phase and other administrative-signal effects.

The World Government interpretation also follows the published Venus Next rule and its FAQ
clarification: the selected Player makes the decision, while TR and placement/track rewards belong
to the neutral government rather than that Player. In this model Admin performs that work and has
no Owner capability, so Owner-only rewards have no eligible recipient.

## Implementation consequence

Effect matching must receive both identities it needs instead of collapsing them:

1. evaluate `BY` against the triggering event's Actor;
2. specialize generic consequence output using the effect's Owner or, for unowned reward effects,
   an eligible performing Owner;
3. route the resulting Task independently according to the effect-routing rule;
4. execute the eventual Task through its recorded Actor.

This audit does not settle the representation of World Government control. It only requires that a
selected Player's authority over the choice never replace Admin as the performer.
