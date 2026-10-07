# Content selection and expansion eligibility

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** deciding what is Content, how Bundles and Modules select it, whether a content
> item can be included without an expansion, or where to place support declarations for Content.
>
> **Status:** current selection model, working dependency boundaries, and a separately labeled
> unimplemented eligibility proposal.

The [class-existence scenario draft](../class-existence-scenarios-draft.md) records tested examples of
which Classes appear in configured games. Source and tests take precedence over this account.

## Roles and current selection

Distinguish these three roles, even when one Class fills more than one:

- A **Bundle** is source provenance and loading structure. It may provide Modules, Content, or
  both, but is not itself selected.
- A **Module** is an ambient-rule choice. Selecting one intrinsically selects the rule closure
  reachable from its declaration.
- **Content** is one authored game item intended for individual selection: a corporation card
  (including each beginner corporation), prelude card, project card, whole map, milestone, award,
  colony tile, global event, or party. Map areas are local to their map. Standard actions and
  standard projects are rule vocabulary, not Content. Content is a role, not a Pets supertype.

Most Content is individually selectable today. Maps currently fill both the Content and Module
roles, and their areas are selected as separate Classes during premise construction. All six
Turmoil parties currently enter intrinsically with Turmoil; individual party selection remains
unimplemented. Neither current constraint changes which items count as Content. Whether maps need
to remain Modules is unsettled.

The intended user choices are an individual Content Class or all applicable Content from a bundle.
Intermediate pool choices, such as excluding only one bundle's cards or choosing random milestones,
belong to configuration resolution before a game premise is constructed. The configuration model
does not yet have a separate grouped Content choice. Named `CardPack` Classes therefore remain
transitional Module subtypes, though a Content group should not be an ambient Module. Prelude 2
has `Prelude2CardPack`, not a second Prelude rules Module. The Milestones & Awards bundle has no
Module; its goals are currently selected individually.

Known incompatible Content pairs are checked against the complete game Class table by
`TerraformingMars.premiseRequirement`. The non-default `Unsafe` Module permits those pairs.
`autoSelectWhen` on cards and goals supplies ordinary pool preferences. Explicit Content
selection overrides a preference but still fails the final requirement unless `Unsafe` is selected.
Ecology Experts is individually selectable but absent from the normal Prelude 1 pool.

A Module whose Class Name equals its bundle name currently associates that bundle's cards and
colony tiles as default Content. Other Modules do not claim resource Content; Content needing its
own group control therefore lives in a separate same-named resource group. This coincidence is
load-bearing, not decorative — check
[`Bundle.kt`](../../src/common/dev/martianzoo/tfm/canon/Bundle.kt) before renaming a Module or moving
its Content.

The boundary has several useful examples:

- `PreludeCard` is support vocabulary required by both Prelude's intrinsic setup and phase rules
  and some selected Content. A Prelude face reaches it through `CardFront<Class<PreludeCard>>`;
  Valley Trust reaches it through its constructive instruction. Excluding Prelude Content must
  not remove the card back while Prelude rules remain selected.
- Colony tiles and Turmoil Global Events are individually selectable non-card Content associated
  with their rules Modules. A resolved choice excluding only project cards should leave these
  selected.
- Milestones & Awards are individually selected Content supplied by a bundle with no Module or
  pack control.
- Turmoil parties are Content even though their Classes currently enter as intrinsic Turmoil rules.
  Individual party selection must make the committee sequence tolerate an omitted party.

## Declaration placement

A content-local Class supplies a particular item's state or rule: map areas, special tiles, remote
areas, watchers, markers, special placement bonuses, ruling policies, and exceptional custom metrics
or instructions are examples. Keep it next to the item that needs it. A card's `components` field
can hold short declarations; use `cards.pets` or `*.cards.pets` for readable multiline declarations
such as Mars Nomads' `NomadsMarker`. A helper shared across bundles may be repeated beside each user,
subject to the catalog's equal-declaration check: Pristar and United Nations Mars Initiative both
declare `HasRaisedTr` and `TrWatcher`, while Robotic Workforce and Cyberia Systems both declare
`CopyProductionBox`. This is source ownership, not a second Class identity or a private namespace.
Shared support within a bundle needs one declaration. For
individually selectable cards, a shared helper in a card resource follows the cards that use it, as
Promo's `Disease` does. Bundle-level rule helpers in ordinary `.pets` files are ambient to a
same-named Module, as Promo's `MyResourceWasRemoved` and `MyProductionWasDecreased` are. A bundle
can own core vocabulary without a Module, though `PromoCardPack` currently is one. `Asteroid` and
`Floater` are core to the wider game. Turmoil's ruling bonus effects live on their Party
declarations; each party's policy Class sits immediately below it. Policies depend on the live
`Ruling<Party>` and `ActionPhase`; event positions depend on the live `GlobalEvent`. Removing those
owners through ordinary gameplay removes their dependents. Parties themselves are required
singletons. Card-granted resource values and Cathedral Option remain dependent on their granting
cards.

## Card-data compilation

`CardPetsGenerator` emits printed tags and attached capabilities as exact-count invariants, without
extra creation effects. Ordinary engine construction creates their concrete dependent parts; see
[ENGINE.md](ENGINE.md#queries-invariants-and-dead-ends) for the construction boundary. Identical
requirements are deduplicated, and repeated printed tags retain their exact counts.

Card metadata reads tags from inherited exact-count invariants. A required attachment carrying
persistent behavior counts toward the card's active role. Initial spendable resources, choices,
and shared-state changes remain ordinary instructions. Pharmacy Union's automatic starting-money
effect still precedes execution of the queued consequences of its microbe tags.

## Inclusion when an expansion is absent

**Working distinctions.** A Content item's bundle establishes provenance, not its dependencies.
Judge what the item actually needs in a game without the expansion. The
[value-dependency inventory](https://docs.google.com/spreadsheets/d/13WRf7ljJLuy3iwTr5caQgKhTPhNugPKJuGx1ikALshY/edit?gid=0#gid=0)
and its separate hard-dependency tab record the current case audit; avoid copying their row inventory
here. A minimum requirement on an absent parameter is hard; a maximum requirement alone is not.
Treat a card with even one unusable printed action as hard-dependent, along with mandatory effects
that cannot resolve, unclaimable milestones, unplaceable map areas, and awards whose metric cannot
distinguish players. These require the expansion even if other benefits survive. The chosen boundary
also makes `VenusTag` depend on `VenusNextExpansion`: selecting a card with that tag does not
activate Venus-tag vocabulary for the whole game. `Floater` belongs to the wider game, so floater
synergy is outside this expansion audit. Card VP is deliberately excluded from this *value* audit;
this is not a rule to ignore VP during play.

**Proposal, not implemented: test each affected payoff for viability.** Group an item's non-cost
outcomes, including penalties imposed by events, by activation (on play, a particular action or
trigger, or award scoring) and output (resource, production, step, card draw, metric, etc.).
Additive terms with the same activation and output form one payoff; an explicit `OR` choice forms
one payoff even when its alternatives differ. With the expansion absent, counts of its missing
objects are zero and events or actions supplied only by it cannot occur. Consider the smallest
otherwise valid setup containing the item and its other hard dependencies, but omitting the target
expansion. Ask whether each affected payoff *could* still produce a nonzero result in a reachable
state. If any is always zero, require explicit opt-in. If every affected payoff remains
possible, allow default inclusion. A branch explicitly guarded by a check for the expansion is
deliberately optional and may be skipped, provided the guard covers that branch; a reference
elsewhere is not proof of consent. Keep opaque cases for manual review. This test says whether a
kind of value survives, not whether the lost value is numerically small.

The proposed test keeps Constructor's city metric when Colonies is absent, matching the
[specific BGG ruling](https://boardgamegeek.com/thread/3242831/article/43755615#43755615).
Summit Logistics keeps its planetary-tag payment; Molecular Printing keeps its city payment;
Atmoscoop keeps its temperature alternative. Conversely, Ceres Tech Market loses its on-play
colony payment even though its later action works. Among the inventory's "Benefits reduced"
rows, Venus Infrastructure, Gyropolis, and Io Sulphur Research retain the affected payoff by
another term or choice; the others lose at least one payoff. Summit Logistics also lacks printed
Venus/Colonies dependency icons, providing published evidence that its smaller payout is allowed.
An explicit guard supports the optional bonuses on Cimmeria, Amazonis, and Vastitas map areas and
the extended Venus track's Colony bonus; Vitor's solo award guard illustrates the same intent
outside expansion selection.

**Current model diverges.** [`TfmCatalog.bundleCompatibilityRequirement`](../../src/common/dev/martianzoo/tfm/canon/TfmCatalog.kt)
collects all referenced Class Names without considering `OR`, guards, or whether a payoff remains
possible. It consequently blocks Constructor without Colonies, as characterized in
[`cards/BugsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/BugsTest.kt). Any implementation
of the proposal needs to preserve those distinctions without making bundle provenance a semantic
dependency or adding per-card exceptions. The magnitude boundary for surviving but severely reduced
payoffs remains a judgment to revisit before adopting this as an automatic-selection rule.
