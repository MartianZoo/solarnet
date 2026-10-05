# Pets tricks, hacks, and cheats

This is an authoring guide to the small Pets expressions that carry surprisingly large rules in the
Terraforming Mars canon. The examples come from the canon `.pets` declarations and the Pets strings
in `cards.json5`; they describe current behavior, not proposed syntax. Each trick has two examples
chosen for what they reveal, rather than every place it occurs. The formal rules are in the
[Pets language specification](pets-language-spec.md) and [type-system specification](type-system-spec.md).

## 1. Leave out an owned argument when lexical `Me` supplies it

`Owned<Me@Owner>` gives owned cards a lexical owner. On a card, bare owned resources, production,
tags and many triggers mean *this card's player*. `EACH Me@Player` explicitly rebinds that name to
each selected player. Use literal `<Anyone>` when a dependency should accept every owner.

- **Ecoline:** [`3 Plant, PROD[2 Plant]`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/cards.json5) gives its player both the plants and production, without spelling either owner.
- **Tharsis Republic:** [`CityTile: 3 MC`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/cards.json5) observes the corporation's own city tiles and pays its owner. Its adjacent `CityTile<Anyone, MarsArea>` effect deliberately watches all players' cities.

This is an Owned-specific insertion, not a general rule that every bare expression means “mine.” An
ownerless `OceanTile` trigger on an owned card instead filters by the event's actor; Arctic Algae
writes `OceanTile BY Anyone` to watch everyone.

## 2. Omit an argument that merely repeats its declared bound

A type has a bound for *every* dependency even when its spelling supplies no argument. Write only
the constraints that narrow those bounds. This is different from lexical ownership insertion
above: omitting a bound-equivalent argument does not bind a particular component.

- **Action-used marker:** [`ActionUsedMarker<@ActionCard>`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/actions.pets) narrows the card, leaving the marker's declared `GenerationScope` bound implicit. Its declaration is in [card-model.pets](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/card-model.pets).
- **Stormcraft:** [`Billing<Class<Heat>>`](../src/common/dev/martianzoo/tfm/canon/ColoniesExpansion/cards.json5) watches heat debts from any action. Its one argument finds the resource-class slot, leaving both `HasActions` and `ActionSlot` at their bounds.

The type system's compact rendering uses the same principle. An omitted argument is safe only if
the remaining arguments still match the intended dependency keys.

## 3. Let the argument's type find its dependency slot

Arguments are matched left to right to the first *compatible open* dependency, not simply to the
first position. A card or an area can often be named alone even when ownership was declared first.
When two slots accept the same kind, order still matters.

- **Kaguya Tech:** [`CityTile<@MarsArea> FROM GreeneryTile<@MarsArea>`](../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.json5) puts the area in each tile's area slot; the owner slot is skipped. The marker makes both sides use the same chosen area.
- **Turmoil delegates:** [`PartyDelegate<This, DelegateHolder@Owner>`](../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/classes.pets) puts the party in its party slot and the owner in its owner slot, despite the inherited owner key coming first.

This is why `Foo<Bar>` can be wonderfully short but is not always unambiguous: if `Bar` fits two
open keys, the first takes it.

## 4. Put `FROM` inside the one argument that changes

`Foo<To FROM From>` is one transmutation of `Foo`, with only that dependency changed. It shares the
class, count, quantifier and any unchanged arguments. It also keeps one open narrowing choice rather
than duplicating it on both sides.

- **Air Raid:** [`5 MC<Me@Owner FROM Anyone>`](../src/common/dev/martianzoo/tfm/canon/ColoniesExpansion/cards.json5) takes five M€ from a chosen owner and gives those same five to the card's owner. With no owner holding five, the transfer is unavailable.
- **Special Permit:** [`4 Plant<Me@Owner FROM Anyone>`](../src/common/dev/martianzoo/tfm/canon/Prelude2CardPack/cards.json5) expresses the same ownership transfer for plants, without separately spelling a removal and a gain.

The compact form requires the same root class on both sides and exactly one changing argument.
For different classes, use ordinary `A FROM B`. Literal `Anyone` does not exclude the current owner;
`Player(NOT Me@Owner)` would express that restriction. A full form may also be necessary to keep a
marked variable shared: [Banned Delegate's leader transfer](../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/cards.pets)
repeats `@Party` on both sides. Compacting it leaves only one occurrence and is rejected.

## 5. Mark a choice once and carry it through a larger operation

`@` marks a type choice that multiple occurrences must share. It is especially valuable when a
full transmutation changes classes, or a `THEN` continuation must use the earlier choice.

- **Kaguya Tech:** [`CityTile<@MarsArea> FROM GreeneryTile<@MarsArea>`](../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.json5) cannot move a greenery off one hex and create the city on another; the area is one choice.
- **Changing Times:** [`Current<Event@Class> FROM Coming<Event@Class>`](../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/classes.pets) moves each selected Global Event from Coming to Current while preserving its exact event class.

Matching names without markers do not themselves make one shared choice. A marker must be in the
scope established by its instruction, effect or `EACH` selector.

## 6. Let an illegal event happen, then make the operation dead-end

An automatic `:: Die` listener can reject a bad operation *after* its triggering change. The
enclosing transaction rolls back that change, its consequences, tasks and history. This is the
large hack: the rule can describe the forbidden result directly, without prechecking every path by
which somebody might produce it. It is appropriate only when a dead end rejects the entire
operation; it does not make an already committed result harmless.

- **Protected Habitats:** [`-Plant OR -Animal OR -Microbe BY Player(NOT Me@Owner):: Die`](../src/common/dev/martianzoo/tfm/canon/CorporateEraExpansion/cards.json5) catches an opponent's attack regardless of which action caused the removal. The attempted removal rolls back.
- **Global parameter completion:** [`-@GlobalParameter:: Die`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/board.pets) rejects any later attempt to lower a completed track, no matter which card or event requested it.

The same rollback pattern protects the Mars Nomads marker through its ordinary `: Die` effect.
It depends on transaction rollback, not on `Die` somehow preventing the triggering event from starting.

## 7. Use a zero-or-one metric as an `if` without a branch

`I / M` performs `I` as many times as the metric says. A metric that is zero or one makes the
whole change either `Ok` or one copy of `I`. It can replace the longer shape
`(condition: I) OR (opposite condition: Ok)` when a counted component expresses the condition.

- **St. Joseph of Cupertino Mission:** [`End: VictoryPoint / Cathedral`](../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.json5) awards a point only if its optional `Cathedral` marker exists; `Cathedral` has `HAS MAX 1 This`.
- **Diversity global event:** [`10 MC / (9 (Class<@Tag>(HAS @Tag<Me@Player>) OR Influence) MAX 1)`](../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/cards.pets) turns a threshold into a zero-or-one payout: below nine, no M€; at nine or more, ten. This is the same trick with a capped computed metric rather than a singleton component.

The first is the canon's clearest literal `/ SomeSingleton` example. If the metric can exceed one,
the expression repeats the change; it is no longer a boolean gate.

## 8. Make a fallback arm available only when its exception applies

`OR` chooses among feasible arms. A `MAX 0 X: ...` gate makes an exceptional arm available only
when `X` has no candidates. The normal arm stays the normal rule, with no special case baked into
its type.

- **Lava Flows:** [`LavaFlows_SpecialTile<VolcanicArea> OR (MAX 0 VolcanicArea: LavaFlows_SpecialTile<>)`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/cards.json5) uses a volcanic area if the map has one and the normal special-tile placement default otherwise.
- **Default greenery:** [`CLASS DefaultGreeneryTile`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/board.pets) offers `GreeneryTile<LandArea(HAS Neighbor<OwnedTile>)>` and opens the wider `GreeneryTile<LandArea>` choice only when no empty adjacent area exists.

`OR` is a player choice if both arms survive. The zero test matters: an ungated fallback would offer
an extra placement even when the normal placement is available.

## 9. Use `<>` to opt in to a gain's hidden placement rule

An empty argument list does not change the type, but on a gain it explicitly accepts that class's
gain-only dependency default. A bare gain that would silently take such a default is rejected.
This lets the short notation stay short while still flagging a consequential board choice.

- **Aquifer standard project:** [`1 MC / cost -> OceanTile<>`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/actions.pets) accepts the `WaterArea(HAS MAX 0 Tile)` default from `OceanTile`.
- **City standard project:** [`1 MC / cost -> CityTile<>, PROD[1 MC]`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/actions.pets) accepts the city default that avoids neighboring cities.

`<>` is not a universal “use defaults” decoration. It is invalid where that use has no dependency
default to accept. A bare removal can also deliberately decline its removal-only default.

## 10. Put an amount policy on the class, then omit it everywhere else

Classes can declare gain and removal quantifiers. Bare `I` then inherits a policy: mandatory `!`,
as-much-as-possible `.`, or optional `?`. A single class declaration governs many cards and
effects. In particular, `GlobalParameter` gains use `.`, while `CardResource` gains use `.`.

- **Large Convoy:** [`5 Plant OR 4 Animal`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/cards.json5) inherits ordinary plant and card-resource gain policies. The animal arm can give as many as a compatible holder permits, including zero if no useful holder exists.
- **Atmoscoop:** [`2 TemperatureStep OR 2 VenusStep`](../src/common/dev/martianzoo/tfm/canon/VenusNextExpansion/cards.json5) inherits `GlobalParameter`'s as-much-as-possible gain policy, so a track with less than two spaces left can still be raised to its limit.

An explicit quantifier should mean a real exception. When an abstract AMAP target has no useful
concrete narrowing, it becomes `Ok`; a mandatory target does not.

## 11. Make a refinement's candidate fill an unspoken dependency

Inside `Foo(HAS Bar)`, the `Bar` query can use the `Foo` candidate as its missing dependency. It
is a compact join: ask each candidate whether the matching dependent component exists. This is
not the same as asking about the enclosing card's owner.

- **First-player selection:** [`Player(HAS StartToken)`](../src/common/dev/martianzoo/tfm/canon/VenusNextExpansion/venus.pets) selects the player who owns the Start Token, with no explicit `StartToken<Player>` argument.
- **Community Services:** [`CardFront(HAS MAX 0 Tag)`](../src/common/dev/martianzoo/tfm/canon/ColoniesExpansion/cards.json5) counts cards with no tags; the omitted `Tag` holder is each candidate card.

Writing `StartToken<Anyone>` instead would accept a token owned by any player. The bare dependent
expression is what leaves the candidate slot open.

## 12. Use `EACH` to turn a type query into a family of effects

An `EACH` selector binds a marked component or class once per match; the body is then repeated with
that choice. It can replace a long manually enumerated list while keeping each choice local.

- **Mars map:** [`This: EACH Class<@MarsArea> { @MarsArea }`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/board.pets) creates every selected map area from the class table; each new map supplies its own area classes.
- **Promo card pack:** [`This: EACH @Class<Resource> { ResourceRemovalWatcher<@Class> }`](../src/common/dev/martianzoo/tfm/canon/PromoCardPack/promos.pets) installs a watcher for each resource class, so new resource subtypes use the same attack record rule.

`EACH Me@Player { ... }` binds lexical `Me` inside its body to each selected player;
`EACH Player { ... }` instead preserves the enclosing binding.

## 13. Let an inherited `This` rule cover every subclass

`This` in a class rule is rebound when the effect is specialized for a subclass or component. A
single generic rule can therefore make subclass-specific records and consequences without one
effect per card, resource, or global parameter.

- **Event cards:** [`-This ... :: PlayedEvent<Class<This>>`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/card-model.pets) records the exact event-card class when that card leaves play.
- **Global parameters:** [`This: TerraformRating`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/board.pets) gives the actor TR when an oxygen, temperature, ocean, or Venus step is raised, all from the one `GlobalParameter` superclass rule.

The contextual actor still matters: an Admin-driven step is not a player's TR gain. Inherited
`This` removes duplicated declarations; it does not erase attribution.

## 14. Bind a count once with `X`

One `X` within an instruction or action is one open positive amount. Repeating it fixes an exchange
rate without writing a separate action for every possible quantity.

- **Sulphur-Eating Bacteria:** [`X Microbe<This> -> 3X MC`](../src/common/dev/martianzoo/tfm/canon/VenusNextExpansion/cards.json5) spends any chosen positive number of its microbes for three M€ each.
- **Ceres Tech Market:** [`X ProjectCard -> 2X MC`](../src/common/dev/martianzoo/tfm/canon/Prelude2CardPack/cards.json5) sells any chosen positive number of cards for two M€ each.

`X` never means zero; use `?` or an `Ok` arm when declining the change is part of the rule.

## 15. Count distinct kinds by counting `Class` components

`Class<@Tag>(HAS @Tag<Me@Owner>)` counts tag *classes* for which the player has a tag, rather than
counting every tag component. One expression turns a pile of duplicate icons into a distinct-kind
count. The same shape works for resource kinds and card-resource kinds.

- **Diversifier milestone:** [`8 Class<@Tag>(HAS @Tag<Me@Owner>)`](../src/common/dev/martianzoo/tfm/canon/HellasMap/hellas.pets) asks for eight distinct tag types, regardless of duplicate tags.
- **Collector award:** [`Class<@Resource>(HAS @Resource<Me@Player>)`](../src/common/dev/martianzoo/tfm/canon/AmazonisMap/amazonis.pets) scores the number of resource kinds the player has, rather than the number of cubes.

The `Class<...>` candidates are the things being counted. Matching `@` markers make the nested
`HAS` check that candidate class for the player; repeating an unmarked class name would be an
independent query.

## 16. Repeat a header variable to force two dependencies to agree

A `@` variable repeated in a class header ties dependency paths together. Supplying either side
constrains the other, so the game cannot create a resource or tag owned by one player on a card
owned by someone else.

- **Card resources:** [`CardResource<ResourceHolder<Class<This>, @Owner>> : Owned<@Owner>`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/resources.pets) ties the cube's owner to the owner of its holder; `Class<This>` also chooses a holder compatible with that resource subtype.
- **Tags:** [`Tag<TagHolder<@Player>> : Owned<@Player>`](../src/common/dev/martianzoo/tfm/canon/TerraformingMars/card-model.pets) ties the printed tag's owner to its card's owner, without a separate per-tag validation rule.

Without the repeated marker, the two owner positions would be independent even though they have
the same spelling and bound.

## 17. Replace a one-change `OR Ok` with `?`

An optional quantifier already allows zero. When the only choice is one elementary change or no
change, `?` states it without an `OR` arm. The changed count is one in both examples, so optional
amounts cannot introduce a partial result.

- **Mars University:** [`ScienceTag: ProjectCard FROM ProjectCard?`](../src/common/dev/martianzoo/tfm/canon/CorporateEraExpansion/cards.json5) offers one card exchange or none, including when the player has no card to exchange.
- **Cathedral offer:** [`This: UseAction<CathedralOption>?`](../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.json5) lets the city owner accept or decline the card draw offer. The owner default and the unchanged `ActionSlot` bound make both extra arguments unnecessary.

This does not replace every `OR Ok`: a sequence may have work that must still follow `Ok`, a gate
may control when `Ok` is legal, or an optional count greater than one may allow intermediate amounts
the original choice did not.
