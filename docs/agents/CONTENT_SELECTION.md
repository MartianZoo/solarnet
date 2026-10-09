# Content selection and expansion eligibility

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** deciding what is Content, how Bundles and Modules select it, whether a content
> item can be included without an expansion, or where to place support declarations for Content.
>
> **Status:** current selection model, a proposed generic bundle/content convention, working
> dependency boundaries, and a separately labeled unimplemented eligibility proposal.

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

`GameConfig` and `GamePremise` use the same signed Class-selection vocabulary for Modules and
individual Content. A positive name requests that Class; a minus-prefixed name excludes it. The
resolved premise derives its Module set by classifying the included names against the Catalog's
Module registry, so there is no separate stored Module selection.

Integer entries are a separate, additive setup operation rather than Class-existence constraints.
Selected setup rules create the default quantity; the generated premise then applies the configured
delta. Terraforming Mars currently uses `SelectableCorporationCount` with default 2 and
`SelectablePreludeCount` with default 4, so `-1 SelectablePreludeCount` produces three offers.

The intended user choices are an individual Content Class or all applicable Content from a bundle.
Intermediate pool choices, such as excluding only one bundle's cards or choosing random milestones,
belong to configuration resolution before a game premise is constructed. The configuration model
does not yet have a separate grouped Content choice. Named `CardPack` Classes therefore remain
transitional Module subtypes; the proposal below would retain such a pack as an otherwise empty
Module rather than introduce a separate grouping concept. Prelude 2 has `Prelude2CardPack`, not a
second Prelude rules Module. The Milestones & Awards bundle has no Module; its goals are currently
selected individually.

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

## Proposed generic bundle/content convention

**Proposal, not implemented.** Replace the category-specific discovery in `TfmCatalog` with a
documented source convention that does not know Terraforming Mars Classes or content categories.
The convention should explain default Class selection by inspecting Pets sources and their bundle.
It must not derive semantic dependencies merely from source provenance.

### Source contract

- `Module` joins the standard system declarations rather than remaining a Class known only to
  `TfmCatalog`. `autoSelectWhen` and `premiseRequirement` each acquire one standard property
  identity inherited from `Component`; the former has configuration meaning only on Modules and
  Content roots. Generic premise code discovers concrete Module subclasses in the Catalog's master
  table, and premise-local declarations may not declare Modules.
- A bundle is a source unit with an explicit, unique `bundleName`, independent of its resource
  directory. Two bundles with the same name in one Catalog are invalid; one Bundle may aggregate
  several resource locations when needed.
- Files ending in `.pets` declare Classes and nothing more: placement there confers no selection,
  availability, or dependency. A bundle's owning Module necessarily comes from ordinary source,
  because content sources cannot declare Modules.
- Files ending in `.content.pets` contain individually selectable Content roots. Every declaration
  in such a file must be concrete, non-Module, and top-level. Support declarations belong in
  ordinary `.pets`; because ordinary source is not automatically locked to a Module, independently
  selecting a content root may still reach its support. Every support Class needed by a root must be
  reached through ordinary Pets structure, adding `HAS Class<...>` where no constructive reference
  already does so. Content that is also a Module, such as a map, is selected as a Module and has no
  content root.
- If a bundle's ordinary source declares a concrete Module whose Class Name equals `bundleName`,
  that Class is the conventional owning Module. A concrete Module named like a bundle but declared
  only in another bundle is invalid. Other Modules in the bundle remain independent and acquire no
  Content by co-location.
- Selecting the owning Module contributes every content root in that bundle as a default Class
  selection. Without an owning Module, the roots remain individually selectable but receive no
  bundle default. A pack with defaults and no ambient rules is therefore honestly an otherwise
  empty Module, rather than a second grouping concept.
- Identical duplicate declarations retain one Class identity. Every declaring site, including two
  files in the same bundle, must agree on source kind. A Content root supplied by several bundles is
  a default of each owning Module. A Bundle reached repeatedly through composed Catalogs counts
  once; two distinct Bundles with the same name are invalid. Declarations contributed outside a
  Bundle are ordinary.

For example, `Prelude1CardPack/prelude1.pets` would declare `Prelude1CardPack`,
`cards.content.pets` would contain only the Prelude card roots, and `cards.pets` would contain any
support declarations. The generic loader would relate the first two through the same-name rule; it
would not know what a Prelude or card is.

Canon supplies a reason for each source rule:

| Provision | Canon example | Why the provision exists |
| --- | --- | --- |
| Standard `Module` and property identities | `TerraformingMars`, `PreludeExpansion`, `TharsisMap`, `Prelude2CardPack`, and `MandatoryVenusVariant` all use the same configuration mechanism, despite representing base rules, expansion rules, a map, a card pool, and a toggle. | Their shared premise behavior should come from the standard `Module` role and standard properties, not five interpretations in `TfmCatalog`. |
| Explicit bundle identity | `PreludeCommon` supplies `PreludeCardPack` and `PreludeCard` but is not `PreludeExpansion` or `Prelude1CardPack`; those are separate source units with separate purposes. | A directory basename happens to identify current standard bundles, but should not become semantic identity. |
| Ordinary `.pets` has no selection meaning | [`PreludePhase`](../../src/common/dev/martianzoo/tfm/canon/PreludeExpansion/prelude.pets) is support explicitly reached by `PreludeExpansion`; [`Disease` and `NomadsMarker`](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.pets) support particular promo cards. | Sharing a file kind or bundle must neither select a helper nor force the whole pack when an individual card reaches it. |
| `.content.pets` contains roots, not helpers | `MarsNomads` is selectable Content, while `NomadsMarker` is its state. United Nations Mars Initiative and Pristar are roots, while their identical `HasRaisedTr` and `TrWatcher` declarations are support. | Treating every declaration in today's card resources as Content produces fake user choices; inferring roots by “not referenced” would misclassify Content that refers to other Content. |
| A Module that is also Content has no second declaration | `TharsisMap`, `HellasMap`, and the other maps are both map choices and live rule Modules. | Putting a map in `.content.pets` as well would give the same Class two default-selection meanings. Its Module selection is already the user choice. |
| Same-name ownership ignores neighboring Modules | The Venus bundle declares `VenusNextExpansion`, `WorldGovernmentRule`, and `MandatoryVenusVariant`; only `VenusNextExpansion` has the bundle's name. | Selecting World Government must not accidentally contribute the Venus card deck merely because both declarations share a source bundle. |
| An otherwise empty pack Module is legitimate | [`Prelude2CardPack`](../../src/common/dev/martianzoo/tfm/canon/Prelude2CardPack/prelude2.pets) selects a pool but carries no ambient Prelude rules; those belong to `PreludeExpansion`. | Reusing the signed Module mechanism is smaller than inventing a separate kind of content-group switch. |
| Duplicate classification must agree | `HasRaisedTr`/`TrWatcher` are repeated beside United Nations Mars Initiative and Pristar; `CopyProductionBox` is repeated beside Robotic Workforce and Cyberia Systems. | Equal declarations must remain the same support Class. If a copy were a content root elsewhere, file provenance would give the same Class contradictory configuration meaning. |

### Generic default resolution

Module `autoSelectWhen` retains its current fixed-point meaning: an unmentioned Module is added when
its condition becomes true. Only after that Module set settles does each selected owning Module
offer its bundle's Content roots. A root's own inherited `autoSelectWhen`, when present, filters
that default. Its condition counts the same names as the Module fixed point: explicit positive
choices, resolved Modules, and players, but never setup-adjustment Classes or other content defaults.

The empty Terraforming Mars configuration demonstrates why Module defaults need a fixed point.
`TerraformingMars` first selects itself; that makes `CorporateEraExpansion` and the default
`TharsisMap` applicable, while excluding Corporate Era makes `QuickStartVariant` applicable.
Likewise, selecting `VenusNextExpansion` makes `WorldGovernmentRule` applicable, and selecting
`PreludeExpansion` makes `Prelude1CardPack` applicable. These relationships are already declared by
their `autoSelectWhen` properties; generic Kotlin should only iterate them to stability.

The resulting precedence remains:

1. an explicit negative selection excludes the Class;
2. an explicit positive selection includes it without applying `autoSelectWhen`;
3. otherwise a selected owning Module contributes it when `autoSelectWhen` is absent or true and
   its tentative premise is valid.

An unmet `autoSelectWhen` is a negative result, not merely an omitted positive: if another selected
Class structurally requires that Class, the premise is invalid. A failed tentative premise merely
withholds the default. Maps cannot acquire both meanings of `autoSelectWhen`, because Module
declarations are forbidden in content sources.

Compatibility becomes an authored fact checked by generic premise validation. A dependency on a
single Module can be ordinary structure, as `DesertSettler` already writes `HAS ElysiumMap`.
`premiseRequirement`, available on every Class, expresses conditions structure cannot, such as
disjunctions and `Unsafe` exceptions. Premise construction, rather than world construction, should
perform the checks currently split between `GamePremise` and `Engine.validatePremise`, including
every inherited `premiseRequirement` on a concrete Class in the closure.

A default root's **tentative premise** consists of the settled configured selections plus that root,
without other content defaults. If it fails any check the final premise applies—an excluded Class or
unrequested Module in its closure, an unmet `premiseRequirement`, or root viability—the root is not
contributed. An explicitly requested root is never tentative: the same failure makes the premise
invalid. The complete premise is validated again; defaults that pass separately but fail together
are an authoring error. This requires a new generic loader capability to compute or attribute
closure failures per candidate root; the current loader computes only the final closure and cannot
yet perform this step.

This replaces source-derived `classAvailabilityModules`. Bundle provenance never makes a Class
private, and nothing is private unless an authored fact says so.

The current special handling of a card front, its card back, and every bundle-locked Class it names
must therefore migrate into authored properties plus generic premise checks. For example, a
condition inherited only from `BeginnerCard` must be placed on the selectable corporation roots
before card-specific selection code is removed. Structural closure deliberately ignores references
that merely occur in counts, requirements, and triggers, so the tentative check is narrower than
today's all-references compatibility rule. A dead optional payoff such as Ceres Tech Market's colony
payment needs an authored default condition; explicit selection may still be valid. Constructor's
city metric can then remain valid without Colonies, matching the ruling described below.

Canon examples distinguish the default-resolution rules:

| Rule | Canon example | Expected explanation |
| --- | --- | --- |
| Explicit negative beats a Module default | `PreludeExpansion, -Prelude1CardPack` keeps Prelude rules but removes the Prelude 1 pool; `TerraformingMars, -CorporateEraExpansion` causes `QuickStartVariant` to become the fallback Module. | The minus is a signed Class fact, not an exception in the bundle loader. |
| Explicit positive bypasses a preference, not validity | `EcologyExperts` may be requested despite its `MAX 0 Prelude1CardPack` preference, but the final `TerraformingMars.premiseRequirement` still rejects an incompatible Ecology Experts/Viral Enhancers combination unless `Unsafe` makes the authored requirement true. | `autoSelectWhen` controls defaults; premise requirements control valid resolved configurations. |
| A root condition chooses between defaults | Elysium's `Generalist`/`Generalist2` and Milestones & Awards' `Producer`/`Producer22` use opposite `QuickStartVariant` conditions. | An unmet condition must exclude the losing alternative rather than leave it available for accidental structural re-entry. |
| Card-back policy must reach the selectable root | `BeginnerCard` currently owns `autoSelectWhen = BeginnerVariant`, while the selectable Classes are `BeginnerCorporation1` through `BeginnerCorporation5`. | The generic loader must not retain card-front/card-back knowledge; generation should place the effective condition on each root. |
| Structural dependency can state a simple requirement | Elysium's `DesertSettler` says `HAS ElysiumMap`. | Selecting the award without its map reaches an unrequested Module and fails through ordinary structure; no availability table is needed. |
| `premiseRequirement` handles a condition structure cannot | `MandatoryVenusVariant` requires both `MultiplayerMode` and `VenusNextExpansion`; `TerraformingMars` uses disjunctions involving `Unsafe` for known content conflicts. | These are authored configuration facts, not consequences of which directory contains the declarations. |
| Tentative closure removes a structurally impossible default | `OldMiningColony` immediately creates `Colony<>`; without Colonies, its tentative premise cannot realize that instruction. | The pack default should be withheld, while an explicit request should report the same invalid premise precisely. |
| Nonstructural references require authored eligibility | `CeresTechMarket` only counts `Colony` for a payment, so structural closure does not require Colonies. | Its dead payoff should keep it out of ordinary defaults through an authored condition, not through an indiscriminate all-names scan. |
| A surviving payoff need not require an expansion | Amazonis's `Constructor` counts `Colony OR CityTile`; city tiles remain meaningful without Colonies. | The narrower structural/authored rule admits Constructor and fixes the current over-rejection described in the eligibility section. |
| Support follows the root structurally | United Nations Mars Initiative and Pristar explicitly name `Class<TrWatcher>`; `MarsNomads` constructively names `NomadsMarker`. | Moving support to ordinary `.pets` is safe only when each selected root still reaches everything it needs. |

### Intrinsic Module rules

The Module's ordinary Pets dependency closure remains its intrinsic rule layer. File placement must
not manufacture intrinsic dependencies. When a Class must exist with the Module but is not already
reached structurally, the Module declaration should say so with existing syntax such as
`HAS Class<PreludePhase>`. Generated maps should likewise put their concrete area Classes into the
map Module's structural requirements instead of relying on `TfmCatalog` to recognize map data. A
Content root reached by a selected Module's intrinsic closure cannot be excluded: an explicit
negative or unmet default condition then makes the premise invalid.

`PreludeExpansion` illustrates an explicit intrinsic dependency with `HAS Class<PreludePhase>`.
`TerraformingMars` similarly names its phase and action vocabulary. A map exposes the harder case:
`MarsMap` creates every inhabited `MarsArea`, but today's Kotlin first selects the concrete Tharsis,
Hellas, or other area Classes. Generating `HAS Class<Tharsis_1_1>, ...` on `TharsisMap` would make
that fact visible where it belongs. Conversely, Turmoil's six Party Classes currently enter its
intrinsic closure; they must leave that closure before a premise can honestly exclude an individual
party.

Bundle and source-kind handling belongs below `TfmCatalog`. Its complete inputs are bundle identity,
source kind, declarations, and the standard `Module` relation. It must not mention `CardFront`,
`ColonyTile`, `MarsMap`, `Milestone`, `Award`, `MultiplayerMode`, `Unsafe`, or any concrete Module.

### Deliberately separate policies

The convention is additive: it says which Classes a Module contributes by default. It does not
define exact goal-pool replacement, random colony choices, or live setup markers. Those are
configuration policies layered over signed Class selections. If exact named goal pools remain
supported, their grouping must be declared in Pets or neutral source metadata rather than inferred
from `Milestone` and `Award` in Kotlin.

The distinction is visible in ordinary configurations. Selecting `HellasMap` currently supplies its
five milestones and five awards, but explicitly naming four milestones replaces only the milestone
pool while leaving the awards alone. That exact-pool behavior is not a consequence of
`.content.pets`; it needs declared group policy. Similarly, `ColoniesExpansion` makes every colony
tile Class available while setup chooses particular tiles, and `TurmoilExpansion` supplies a deck
of Global Events while a game reveals only positions from that deck. Bundle defaults describe the
available Classes, not these later choices.

Likewise, the current `Callisto` to `CallistoSelected` name construction is Mars-specific setup
policy. A future premise should name an explicit selected Class such as `CallistoSelected`, whose
Pets declaration reaches the underlying tile; how an explicitly selected setup Class creates its
live marker still needs a separate generic design. Do not teach the bundle loader about colonies.
Counted setup adjustments such as `-1 SelectablePreludeCount` remain additive setup operations and
do not participate in this file convention. Kotlin workflow checks that name particular Modules are
also separate workflow debt, not bundle loading.

For example, `PreludeExpansion` creates four `SelectablePreludeCount` components and the premise
adjustment `-1 SelectablePreludeCount` changes that setup quantity to three; neither entry requests
or excludes a Class. In the proposed colony cleanup, `CallistoSelected` would instead be a Class
naming a concrete colony choice. The Kotlin workflow's direct checks for `PreludeExpansion`,
`WorldGovernmentRule`, `ColoniesExpansion`, and `TurmoilExpansion` are yet another concern: they
choose phase progression, not bundle contents.

### Migration and acceptance

1. Promote `Module` discovery and its two premise properties into generic Catalog/Pets semantics.
   Give generic Bundles explicit identities and retain ordinary-versus-content source provenance.
2. Prove same-name ownership, duplicate classification, Module fixed points, default conditions,
   signed precedence, tentative-root validation, and generic premise requirements with a
   game-neutral Catalog fixture.
3. Split generated and handwritten card roots into `.content.pets`, leaving support in `.pets`.
   Move card-back-only default conditions onto their selectable roots. Every non-card root currently
   found merely because it is unreferenced must become a content root or be reached structurally;
   then remove `moduleCardClassNames` and card-specific root discovery.
4. Move colony tiles, goals, and other individually selectable non-card Content into content
   sources. Generate explicit map-area structural requirements on each map Module. Move the goals'
   multiplayer default condition into Pets or neutral group metadata, checking the solo closure
   before choosing its exact structural spelling.
5. Replace source-derived availability with generic closure validation. Before deleting anything,
   compare default sets and explicit-request outcomes with the current code for each tested Module
   combination and account for every intentional difference. Then delete `classAvailabilityModules`,
   `selectionsFor`, the category-specific explicit-inclusion checks, and bundle/category-based
   default discovery from `TfmCatalog`. Decide whether generic viability's `requirement` property is
   a standard Pets fact or belongs in a game policy. Exact goal pools and selected-colony setup
   remain separate until their own declared representations replace the current Kotlin policies.
6. Preserve the class-existence scenarios and premise assertions throughout. At completion, no
   selection path names a Terraforming Mars Class or tests a Terraforming Mars content supertype,
   and each bundle default can be explained entirely from its Pets files, source suffixes, and the
   convention above.

Concrete Canon acceptance cases keep those migration steps honest:

- An empty configuration reaches `TerraformingMars`, `CorporateEraExpansion`, and `TharsisMap` by
  their declared defaults; `-CorporateEraExpansion` reaches `QuickStartVariant` instead.
- `PreludeExpansion, -Prelude1CardPack` retains `PreludePhase` and the Prelude setup rules without
  receiving the Prelude 1 roots. Explicitly adding a Prelude 1 card requests only that root and the
  support it structurally reaches.
- `WorldGovernmentRule` does not receive the Venus deck, while `VenusNextExpansion` does. The
  ownership result follows the bundle-name convention, not Module proximity.
- Explicit `MarsNomads` reaches `NomadsMarker` without selecting the rest of `PromoCardPack`.
  United Nations Mars Initiative and Pristar still resolve to the same `TrWatcher` identity.
- Without Colonies, `OldMiningColony` is withheld as a default and fails when explicitly requested;
  Ceres Tech Market follows its authored default condition; Amazonis's `Constructor` remains valid
  because its city-tile metric still has meaning.
- `HellasMap` may supply its content roots, but replacing only its milestone pool remains a separate
  declared group policy. `-1 SelectablePreludeCount` changes an offer quantity and never becomes a
  Class-existence request.

The boundary has several useful examples:

- `PreludeCard` is support vocabulary required by both Prelude's intrinsic setup and phase rules
  and some selected Content. A Prelude face reaches it through `CardFront<Class<PreludeCard>>`;
  Valley Trust reaches it through its constructive instruction. Excluding Prelude Content must
  not remove the card back while Prelude rules remain selected.
- Colony tiles and Turmoil Global Events are individually selectable non-card Content associated
  with their rules Modules. An explicitly configured tile currently receives its concrete setup
  marker, such as `CallistoSelected`, which specializes `SelectedColonyTile<Class<Callisto>>`; a
  tile present only through the Module default does not. A resolved choice excluding only project
  cards should leave these selected.
- Milestones & Awards are individually selected Content supplied by a bundle with no Module or
  pack control.
- Turmoil parties are Content even though their Classes currently enter as intrinsic Turmoil rules.
  Individual party selection must remove them from Turmoil's intrinsic closure and make the
  committee sequence tolerate an omitted party.

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
