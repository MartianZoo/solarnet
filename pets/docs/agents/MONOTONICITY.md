# Static resource monotonicity analysis

> **Status:** research and a conservative Catalog report; no optimizer is implemented.
> The [Solarnet search problem](../../../docs/agents/OPTIMAL_SOLO.md#adopted-problem)
> supplies the TR63 objective and the meaning of continuation states used below.

## Source map

- [StandardResourceMonotonicityReport.kt](../../src/jvm/dev/martianzoo/tfm/petstools/StandardResourceMonotonicityReport.kt)
- [Canonical card data](../../src/common/dev/martianzoo/tfm/canon/bundles/base/cards.json5)

## Three claims that must not be confused

Suppose two otherwise equivalent continuation states differ only because the second has one more
component of type `E`. Let `best(state)` mean the greatest terminal score still attainable from that
state.

`E` is a **pure benefit** when the extra `E` never lowers `best(state)`. This is the monotonicity
claim: adding more `E` can leave the optimum unchanged or raise it, but cannot lower it.

There are three useful strengths of this claim:

- **Catalog-wide:** it holds for every relevant premise and state in the Catalog.
- **Premise-wide:** it holds after fixing one `GamePremise`; excluded classes can no longer supply
  counterexamples.
- **State-conditional:** it holds from one state because every remaining way to create a problematic
  component has become impossible.

These are claims about *having* an extra component at the same decision point. They do not by
themselves prove that an action which gains `E` is good: that action may cost money, consume a turn,
or cause other effects. They also do not prove that gaining `E` **now** is as good as gaining it
later. Timing can matter even when possession is beneficial.

The comparison between “otherwise equivalent” states also has to include everything that affects
the continuation: generation, queues, deck position or deck knowledge, used-action markers, and any
relevant history. Comparing component counts while silently changing those facts proves nothing.

## How a class challenges monotonicity

The first analysis is syntactic: inspect every included, inhabited class and custom implementation
for ways in which the presence or count of `E` can make the future worse. Important shapes include:

- an upper-bound or exact-count requirement, such as `MAX 0 E`;
- a forced harmful effect triggered by gaining or possessing `E`;
- a limit reached sooner because `E` exists;
- a conversion, payment rule, or score rule whose choices shrink as `E` increases; and
- a custom operation whose implementation consults `E` negatively.

An occurrence is a **hazard**, not automatically a proof. If extra `E` removes only an optional move
that is always useless or harmful, the attainable optimum does not fall: the optimizer would never
need that move. Likewise, a hazardous class that can never exist cannot affect the current game.

The initial conservative catalog scan is implemented by
`./gradlew :junkdrawer:standardResourceMonotonicityReport`. Its default scope is one valid TR63 solo
premise containing every compatible supported expansion. It treats each standard-resource stock
and its production rate as separate quantities. The scan reports upper-bound and exact
requirements with their rule locations, count-scaled instructions, AMAP transmutations into a
different type, effects carried by resource instances, and opaque custom operations. It does not
report a minimum requirement just because meeting it enables an optional card, a capped metric that
cannot cross its requirement threshold, a pure AMAP loss whose remaining amount is still monotonic,
or a removal-triggered protection effect. Custom Kotlin operations are listed separately because
their behavior needs an explicit contract or simulation.

The analysis should therefore record a short evidence ladder:

1. **Candidate:** no harmful interaction has been found by the complete scans currently available.
2. **Hazard found:** some class uses `E` in a potentially harmful way.
3. **Hazard reachable:** that class can occur in a legal continuation from the state being studied.
4. **Counterexample proved:** paired states or continuations demonstrate a lower attainable optimum
   with the extra `E`.

Only the fourth item mathematically disproves monotonicity. For sound optimization, however, every
case below the needed proof level is treated as **not certified monotonic** and receives none of the
pruning privileges of a pure benefit. This is the precise version of “when in doubt, call it
non-monotonic.”

## The closed catalog makes conditional proofs possible

For a particular premise, the projected class table is closed and its concrete types are enumerable.
For each `E`, analysis can build a set of blocker classes: every class whose rules contain a
potentially harmful use of `E`.

A blocker can be discharged when the engine can prove that it cannot matter in any continuation.
Examples include:

- its class is inactive in this premise;
- every physical copy is irrevocably discarded;
- its play requirement can no longer be met;
- its point in a known deck has already passed; or
- every operation capable of creating or replaying it has become impossible.

“The card was discarded” is sufficient only after enumerating recovery, copying, indirect draw, and
play operations. Absence proofs must follow producers as well as the blocked class itself. This is
where Solarnet's closed class table is especially valuable: the analysis can search the actual
loaded mechanisms rather than rely on a fixed list remembered by the optimizer's author.

The useful result is a conditional certificate such as:

> Energy is a pure benefit from this state provided none of `{Factorum, F2, F3}` can enter play.

In an open deck those conditions may be decidable exactly. In a hidden deck, unseen cards keep their
hazards alive. Reveals and irrevocable discards shrink the remaining blocker set, so information can
turn an uncertified component into a certified one during play.

“Monotonic except for these three unseen cards” is therefore valuable even before the three cards
are found. It isolates the uncertainty and tells later search exactly which observations can settle
it. Drawing a card is not pure observation—it also changes the hand, deck, and possibly the game—so
prioritizing draws for information is a decision heuristic rather than a free theorem.

## Factorum and Energy

Factorum currently contains the action:

```pets
-> MAX 0 Energy: PROD[Energy]
```

When the player has no Energy, this action can increase Energy production. Adding one Energy removes
that option. Factorum therefore supplies a counterexample to the claim that Energy is always a pure
benefit. The action is not intrinsically harmful, so it cannot be dismissed merely as an unwanted
option. Under the conservative rule, Energy is not catalog-wide certified monotonic.

This is also a sequencing example. Even if gaining Energy is inevitable and desirable, using
Factorum first and gaining the Energy second can dominate the opposite order. State monotonicity,
action value, and permission to move an action earlier are separate facts.

If Factorum is inactive or provably unable to enter play from the current state, this particular
hazard disappears. That does not finish the proof: the analysis must discharge every other blocker
of Energy too.

### The proposed later-entry chain

This reachability discussion came from an earlier catalog snapshot, before Prelude 2 support.
The [support inventory](../what-is-supported.md) now includes Prelude 2; Applied Science remains
listed among incomplete wild-tag cards. The full Board of Directors / Merger / Double Down chain
has not been reverified by this documentation move. Treat it as a research hypothesis, not an
executable proof. Refreshing the catalog scan is tracked in [TODO.md](../../TODO.md).

The existing definitions do establish the important shape of the problem: Merger can select and
play a corporation; Valley Trust can select and play a Prelude; and Double Down can copy a Prelude's
direct effect. Analysis must therefore discover multi-step producers rather than search only for an
instruction that names Factorum directly. Deck observations can break such a chain, but only after
all alternate orders and copying routes have been eliminated.

Nor should Board of Directors yet be described as the unique late route. [Secondary descriptions of
Prelude 2](https://www.reddit.com/r/TerraformingMarsGame/comments/1lk1pr2/cotd_wg_project_25_jun_2025/)
report that WG Project can select and play a Prelude during the game; if its physical card confirms
that text, it could reach Merger without Board of Directors. That historical lead needs primary-card
verification and a fresh model check before being relied upon, but its apparent existence already shows
why an exhaustive producer search is required.

### Which modeled cards can seed a freely chosen card with a generic resource?

The earlier catalog scan found exactly four card effects whose Pets expression creates the abstract
`CardResource` type on a `CardFront` target:

- **Viral Enhancers** targets the bio card whose tag triggered the effect, not an arbitrary card.
- **CEO's Favorite Project** requires the target already to have a card resource.
- **Corroder Suits** requires a Venus-tagged target.
- **Maxwell Base** also requires a Venus-tagged target.

Thus **no card in that scanned Catalog** meets all three requested conditions:
generic resource type, freely chosen target card, and neither an existing-resource nor Venus-tag
requirement. This conclusion is scoped to that earlier scan; it does not establish the result for the
current expanded Catalog. Applied Science would not
fill the gap in any event because its target must already have a resource.

## Intended analysis result

For each component type, a useful report should contain:

1. the scope: Catalog-wide, premise-wide, or one current state;
2. the status: certified, conditional, counterexample, or not yet certified;
3. every remaining blocker and the rule that makes it hazardous;
4. why each discharged blocker is impossible;
5. whether the conclusion holds for every hidden deck still consistent with observations; and
6. the evidence level reached by the monotonicity ladder above.

This retains the attractive simplicity of monotonic reasoning without pretending that the full card
catalog is simple. The optimizer may make aggressive reductions where it has a certificate and fall
back to branching everywhere else.
