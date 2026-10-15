# Authoring quantified changes

> **Status:** current syntax and Canon authoring guidance.
> Live quantities and failure behavior are specified by the
> [Solarnet resolution contract](../../../docs/agents/QUANTIFIERS.md).

## Before writing an explicit quantifier

1. Inspect the changed Class's gain or removal default; omission does not mean mandatory in every
   context.
2. Count matching events and effect activations. Repeated components normally scale through
   repeated hits, not an added `AMAP` or authored count.
3. Add `!`, `?`, or `*` only when the rule intentionally differs from the inherited default.
4. Keep the quantifier responsible only for the count of one change; choice, targeting, gates, and
   sequencing have separate rules.

A quantifier controls the count executed by one gain, removal, or transmutation. It does not choose
an `OR` arm, satisfy a gate, choose a concrete target, or determine a `PER` metric. Those operations
compose with quantification but have their own rules.

## Syntax and defaults

| Syntax | Name | Contract |
| --- | --- | --- |
| `!` | mandatory | Execute exactly `n`: require sources and dependencies immediately, validate count bounds at operation completion. |
| `?` | optional | Let the player choose any count from zero through `min(n, m)`. |
| `*` | AMAP | Execute `min(n, m)`; the engine, not the player, chooses the count. |

With an explicit scalar, place the quantifier immediately after it (`3? Plant`). With the default
scalar of one, place the quantifier after the type (`Plant?`).

An omitted quantifier is filled from the changed Class before play. `Component` supplies mandatory
gain and removal defaults. A subclass may override either direction independently. For example,
`GlobalParameter` gains and removals default to AMAP, while `CardResource` overrides only gain to
AMAP. An explicit symbol always wins.

For `Gaining FROM Removing` with no explicit quantifier, the gain and removal defaults are combined
into the transmutation's one quantifier: it permits exactly the counts both defaults permit, which
works out to mandatory beating AMAP and AMAP beating optional (L9-8). That is a combination of two
policies, not a ranking — narrowing still treats mandatory and AMAP as incomparable (L3-4). Each
side still supplies its own dependency defaults before the Types are resolved.

## Known Terraforming Mars consequences

- Local Heat Trapping's `2 Animal` is abstract AMAP. With no positive animal holder, the whole
  animal choice is `Ok`. Otherwise the player chooses an existing eligible holder and places as
  many of the two animals as that holder can accept. An absent Fish is unavailable; an existing
  maxed holder would be an eligible zero selection.
- CEO's Favorite Project and Corroder Suits use abstract AMAP card-resource gains. They become `Ok`
  when no compatible card can receive a resource.
- Ocean placement's default targets `WaterArea(HAS MAX 0 Tile)`, so occupied areas are outside its
  Type domain. When the ocean invariant leaves every target at zero capacity, the whole abstract
  AMAP placement is `Ok`.
- Atmoscoop authors separate concrete AMAP global-parameter arms. A maxed arm resolves to `Ok` and
  remains selectable because it is an explicit arm; Atmoscoop is not one abstract parameter domain.
- Viral Enhancers authors separate plant, animal, and microbe choices. An uncollectible
  animal or microbe arm specializes to `Ok`, allowing no bonus even when the entering bio card
  can hold the other resource. Only animals and microbes are eligible; diseases are excluded.
  Splice's microbe-or-MC choice behaves the same way for a card that cannot hold microbes.
- Pharmacy Union does not rely on AMAP treating a vanished card as zero. Its microbe effect
  explicitly chooses between adding Disease while Pharmacy Union exists and `Ok` after it has
  flipped; its independent 4 M€ loss remains pending either way.
