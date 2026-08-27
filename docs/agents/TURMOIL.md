# Turmoil modeling record

> **Read when:** modeling Turmoil. Select only the rule family being discussed from the section map
> below; do not read the entire card/event catalog by default.
>
> **Skip when:** changing the existing Turmoil card pack without Turmoil expansion rules, or doing
> generic engine work just because Turmoil could someday use it.
>
> **Status:** research-backed proposal. Physical rules and event data are source material; component
> homes and Pets are working choices. `LANGUAGE?` marks unexpressed behavior. The captured source is
> not claimed to parse or run.

## Choose the section first

| Task | Read |
| --- | --- |
| Decide which component owns a rule | Choosing the behavioral home |
| Decide per-player/per-card multiplicity | Multiplicity and AMAP |
| Model parties, dominance, or ruling | Current Pets draft; Parties, dominance, and ruling |
| Model influence or delegates | Influence and delegates |
| Model ruling policies | Policies |
| Model the event queue or a named event | Global-event positions and shared card behavior, then only the relevant event family |
| Revisit global-parameter ownership | Related GlobalParameter ownership question |
| Resolve syntax/model gaps | Open language and modeling questions |

## Nearby source

- [Turmoil card-pack `classes.pets`](../../tfm-canon/src/commonMain/resources/canon/bundles/TurmoilCardPack/classes.pets)
  and [`cards.pets`](../../tfm-canon/src/commonMain/resources/canon/bundles/TurmoilCardPack/cards.pets)
  contain supported promotional cards, not the proposed Turmoil expansion model. Search for a named
  card before deciding whether it is a useful precedent.
- [`turmoilCardPackBundle.kt`](../../tfm-canon/src/commonMain/kotlin/dev/martianzoo/tfm/canon/turmoilCardPackBundle.kt)
  shows the current card-pack limit; do not mistake it for a Turmoil Module.

Sources:

- [Official Turmoil rules](https://fryxgames.se/wp-content/uploads/2023/07/TM_TURMOIL_ENG_RULESi.pdf)
- [Working global-event data](https://docs.google.com/spreadsheets/d/1w-n2oUlVg_YBiMzsRjWEVBg01nkqwt5CCDP2M0mRrFg/edit?gid=0#gid=0)

## Choosing the behavioral home

For every effect, first ask:

> If it were not for the ___ component, this rule would not exist.

This is a counterfactual and ontological question about the component's reason for existing. It is
not necessarily a question about the immediately preceding in-game cause. Active-voice phrasing is
a useful diagnostic: components contribute capabilities; rules do not merely happen around passive
data.

Several components may be necessary for one rule, so the blank alone does not mechanically decide
the answer. Paying Steel for a Building card requires both `Steel` and `BuildingTag`. The preferred
home is nevertheless `BuildingTag`: the payment capability emanates from the kind of card being
paid for, while Steel is the object accepted by that capability. `AquiferPumping` then composes as a
second provider of the same capability. Conversely, the fact that one Steel supplies two units of
payment belongs to `Steel`; Aquifer Pumping demonstrates why that value is intrinsic to the
resource rather than to Building cards.

The preferred conceptual home and the currently implementable declaration site are separate
questions. Timing, multiplicity, contextual ownership, or inheritance may prevent Pets from
placing an effect in its ideal component today. Determine the preferred home first; only then
decide whether the language needs a general facility or whether a faithful nearby representation is
better.

Useful corroborating questions are:

- Does removing this component naturally rescind the behavior?
- Is this component the grammatical subject that contributes the capability?
- Can another component independently provide the same capability without duplicating its
  intrinsic details?
- Does this placement cause one response per intended provider, or accidentally multiply it by the
  number of live components?

These questions discipline a subjective judgment; they do not turn it into an algorithm.

## Multiplicity and AMAP

When several components each respond to one signal, an invariant can consolidate their responses.
Turmoil influence uses this shape: every eligible non-leader delegate attempts an AMAP gain of
`DelegateInfluence`, whose concrete owned Type has `HAS MAX 1 This`. The first attempt gains one;
later attempts still have a valid Player dependency but zero invariant headroom and therefore
become `Ok`. This is the settled meaning of AMAP: execute the greatest legal count for an existing
target, without giving the player a choice.

AMAP must not conceal a missing dependency or inapplicable owner. Chairman and party-leader
influence are mandatory because each source is unique. The delegate gain is AMAP specifically
because several legitimate providers contend for one capped result.

Another possible multiplicity shape is for each delegate to produce one occurrence of a Signal and
for a single responder to react to `X ThatSignal`. That can be useful when the aggregate count is
itself the fact the model needs. It appears inferior here: influence wants a capped existential
source, and the direct AMAP contribution keeps the behavior on the delegates that provide it.

## Current Pets draft

The following sections preserve the complete current `turmoil.pets` draft, split only to explain
the modeling decisions around each part.

### Preamble, module, and per-player participation

```pets
// Working, partly aspirational Turmoil model.
// Rules source: https://fryxgames.se/wp-content/uploads/2023/07/TM_TURMOIL_ENG_RULESi.pdf
//
// For each effect, first ask which component best completes: "If it weren't for this component,
// this rule would not exist." That is usually where the effect belongs. This is a judgment about
// the component's reason for existing, not necessarily the immediately preceding game event.
// LANGUAGE? marks syntax or semantics that current Pets does not yet clearly express.

CLASS TurmoilExpansion : Module {
  This:: SendDelegateSA, UsePartyActionSA

  SetupPhase:: EACH Player { TurmoilPlayer, LobbyDelegate }
  SetupPhase:: Chairman<Neutral>, Ruling<Greens>

  // LANGUAGE?: reveal predetermined Coming and Distant events during setup. Delegates outside the
  // committee do not exist in this model, just as unplaced city tiles do not exist.
}

// This component means that its owner is participating in a game with Turmoil. It provides only
// rules that apply once to every participating player, independently of political position.
CLASS TurmoilPlayer : Owned<Player>, System {
  SolarPhase: -TerraformRating
  SolarPhase: LobbyDelegate.

  // The SolarPhase gate prevents the initial setup Ruling marker from granting a ruling bonus.
  Ruling<Party> IF SolarPhase: EVAL Party.rulingBonus
}
```

`TurmoilExpansion` provides the two permanent standard-action doorways because those actions exist
only when the module exists. The module also owns module-specific setup. Unplaced delegates are not
represented as reserve components; a paid delegate gain creates the marker just as a city
placement creates its tile.

`TurmoilPlayer<Player>` is the local provider for rules that apply exactly once to every player by
virtue of participating in Turmoil: TR revision, lobby replenishment, and receipt of a ruling
bonus. This is preferable to a vaguely named bag of effects, though the broad `SolarPhase` triggers
deliberately postpone the rulebook's finer sequencing.

### Parties, dominance, and ruling

```pets
ABSTRACT CLASS Party {
  HAS =1 This
  HAS MAX 1 PartyLeader<This>

  // LANGUAGE?: instruction-valued class properties.
  rulingBonus = Instruction
}

CLASS MarsFirst : Party {
  rulingBonus = { 1 / BuildingTag }
}

CLASS Scientists : Party {
  rulingBonus = { 1 / ScienceTag }
}

CLASS Unity : Party {
  rulingBonus = { 1 / PlanetTag }
}

CLASS Greens : Party {
  rulingBonus = { 1 / BioTag }
}

CLASS Reds : Party {
  // Lowest-TR selection has friendly ties in multiplayer and a distinct <=20 solo condition.
  rulingBonus = { @redsRulingBonus() }
}

CLASS Kelvinists : Party {
  rulingBonus = { 1 / PROD[Heat] }
}

ABSTRACT CLASS PartyStatus<Party> {
  CLASS Dominant {
    HAS =1 Dominant

    // The dominant marker supplies the identity of the next ruling party.
    SolarPhase: Ruling<Party> FROM Ruling
  }

  CLASS Ruling {
    HAS =1 Ruling

    // A policy exists exactly while its party is ruling during the Action phase. The concrete
    // Policy component itself provides every action or passive effect.
    ActionPhase: Policy<Party>
    -ActionPhase: -Policy<Party>
  }
}

CLASS ScientistsUsedMarker : Owned<Player>, Generational { HAS MAX 1 This }

// This permanent standard action is only a doorway. The active Policy component supplies the
// action itself, so removing the policy removes the capability.
CLASS UsePartyActionSA : StandardAction {
  -> UseAction<Policy<Party(HAS Ruling<This>)>>
}
```

Each Party owns the contents of its ruling bonus: if that Party Class did not exist, that specific
bonus would not exist. `Dominant<Party>` supplies the identity used when government changes.
`Ruling<Party>` provides the temporary policy because ruling status is why the policy becomes
available. The policy is gained on entry to the Action phase and removed on exit, so its four
passive effects and two actions do not exist outside that phase.

`UsePartyActionSA` does not contain either party action. It is a permanent doorway that delegates
to the active policy, analogous to the generic card-action doorway.

### Influence and delegates

```pets
CLASS Neutral : Owner

// The Current global event requests a fresh snapshot for each player. Bare Owned types in a
// delegate's effects specialize to that delegate's Owner. MeasureInfluence and Influence narrow
// that Owner to Player, so their would-be Neutral specializations are outside the type bounds.
CLASS MeasureInfluence : Owned<Player>, Signal

ABSTRACT CLASS Influence : Owned<Player>, Generational
CLASS ChairmanInfluence : Influence { HAS MAX 1 This }
CLASS PartyLeaderInfluence : Influence { HAS MAX 1 This }
CLASS DelegateInfluence : Influence { HAS MAX 1 This }

ABSTRACT CLASS AbstractDelegate : Owned<Owner> {
  CLASS Chairman {
    HAS MAX 1 Chairman

    This: TerraformRating
    MeasureInfluence:: ChairmanInfluence
    End: VictoryPoint
  }

  CLASS LobbyDelegate

  ABSTRACT CLASS PartyDelegate<Party> {
    CLASS PartyLeader {
      MeasureInfluence IF Dominant<Party>:: PartyLeaderInfluence
      End: VictoryPoint
    }

    CLASS NormalDelegate {
      // Every matching delegate tries. The Player dependency exists for each attempt; after the
      // first succeeds, MAX 1 leaves zero invariant headroom and makes later AMAP gains 0.
      MeasureInfluence IF Dominant<Party>:: DelegateInfluence.
    }
  }
}

CLASS SendDelegateSA : StandardAction {
  // A newly sent delegate begins as a non-leader; party maintenance may then promote it.
  -> NormalDelegate FROM LobbyDelegate
  5 -> NormalDelegate
}

// LANGUAGE?: maintaining PartyLeader and Dominant requires strict argmax comparisons with their
// distinct tie rules. Returning the appropriate delegates also requires owner-aware fanout.
```

Influence is measured for the Current event rather than continuously maintained through every
political change. `MeasureInfluence<Player>` asks the components that provide influence to
contribute it. The Chairman and dominant Party Leader each contribute their named mandatory
subtype. Every player-owned non-leader delegate in the dominant party attempts
`DelegateInfluence.`; `HAS MAX 1 This` caps that subtype separately for each Player.

The narrowing of both `MeasureInfluence` and `Influence` from `Owner` to `Player` is structural.
Within a Neutral-owned delegate, their contextual Neutral specializations are outside the declared
bounds. This is more precise than describing the result as generic runtime owner matching.

The free and paid delegate actions create `NormalDelegate`. The action does not appoint a leader;
party maintenance owns any subsequent promotion. The exact leader and dominance comparisons remain
an open language problem because their tie rules differ.

### Policies

```pets
// The active policy component supplies and rescinds passive policy behavior directly. It is not a
// permanent watcher whose IF conditions imitate component presence.
ABSTRACT CLASS Policy<Party> {
  HAS MAX 1 This
}

CLASS MarsFirstPolicy : Policy<MarsFirst> {
  Tile<MarsArea> BY Player: Steel<Player>
}

CLASS ScientistsPolicy : Policy<Scientists>, HasActions {
  10 -> ScientistsUsedMarker! THEN 3 ProjectCard
}

CLASS UnityPolicy : Policy<Unity> {
  Pay<Class<Titanium>>:: -Owed<>
}

CLASS GreensPolicy : Policy<Greens> {
  GreeneryTile: 4
}

CLASS RedsPolicy : Policy<Reds> {
  TerraformRating: -3
}

CLASS KelvinistsPolicy : Policy<Kelvinists>, HasActions {
  10 -> PROD[Energy, Heat]
}
```

Each policy is the reason its behavior exists. In particular, Reds is intentionally modeled as
`TerraformRating: -3`; substituting the upstream global-parameter change would confuse the observed
game event with the effect's behavioral home. Scientists and Kelvinists are plain `HasActions`
components. Their actions disappear automatically when their policy components disappear.

### Global-event positions and shared card behavior

```pets
// Slots are position and occupancy, not behavior providers.
ABSTRACT CLASS EventSlot {
  HAS =1 This

  CLASS Current {
    HAS MAX 1 GlobalEvent<This>
  }

  CLASS Coming {
    HAS MAX 1 GlobalEvent<This>
  }

  CLASS Distant {
    HAS MAX 1 GlobalEvent<This>
  }
}

// A concrete event remains the same Class as its EventSlot dependency is transmuted. Its two party
// dependencies are its top-left Distant destination and middle-right Current destination.
ABSTRACT CLASS GlobalEvent<EventSlot, DistantParty, CurrentParty> {
  instruction = Instruction

  // LANGUAGE?: dependency-specialized effects must remain effects of this exact event component:
  //
  //   first reveal      -> add a Neutral delegate to DistantParty
  //                        (normally Distant; the initial Coming card is the setup exception)
  //   entering Current  -> add a Neutral delegate to CurrentParty
  //   Global Event step while Current
  //                     -> EACH Player { MeasureInfluence }
  //                        THEN EVAL This.instruction
  //   Changing Times while Current
  //                     -> remove This
  //   leaving Current   -> transmute the Coming event to Current
  //   leaving Coming    -> transmute the Distant event to Coming
  //   leaving Distant   -> reveal the predetermined next event as Distant
  //
  // EventSlot should not provide these effects. They exist because this staged card exists.
}

// Current invariant checking prevents the three departure transmutations from maintaining =1 at
// every intermediate step. MAX 1 is honest for now; checking minima after the recursive :: chain
// would be a real language/engine feature.

// Data source:
// https://docs.google.com/spreadsheets/d/1w-n2oUlVg_YBiMzsRjWEVBg01nkqwt5CCDP2M0mRrFg
//
// Party dependencies and instruction shorthand below come from the working global-event data.
// Every instruction belongs to its concrete card. Several expressions remain intentionally
// aspirational, especially additive metrics and Instruction-valued class properties.
```

`Current`, `Coming`, and `Distant` are positions and occupancy constraints. The event cards—not the
slots—are the reason neutral delegates are added and printed instructions execute. A language
facility must therefore specialize behavior by the event component's `EventSlot` dependency while
leaving the effect attached to that exact event component. Moving these effects onto persistent
slot watchers would be implementable but would put them in the wrong conceptual home.

Changing Times also exposes a distinct invariant problem. The current engine checks each removal
or transmutation against minimum invariants immediately. Recursive `::` effects execute inline but
do not defer those checks until the chain completes. Consequently all three slots honestly use
`MAX 1` for now. Supporting `=1` after setup would require a general invariant-restoration scope,
not merely different effect punctuation.

### Base Turmoil global events

The two Party dependencies and instruction shorthand are transcribed from the working data. The 31
base cards follow; Election and Revolution incorporate their solo companion rows instead of
pretending those rows are additional physical cards.

```pets
CLASS AquiferReleasedByPublicCouncil<EventSlot> :
    GlobalEvent<EventSlot, MarsFirst, Greens> {
  instruction = { (StartToken: OceanTile BY Engine) OR Ok, Plant / Influence, Steel / Influence }
}

CLASS AsteroidMining<EventSlot> : GlobalEvent<EventSlot, Reds, Unity> {
  instruction = { Titanium / JovianTag MAX 5 + Influence }
}

CLASS CelebrityLeaders<EventSlot> : GlobalEvent<EventSlot, Unity, Greens> {
  instruction = { 2 / PlayedEvent MAX 5 + Influence }
}

CLASS Diversity<EventSlot> : GlobalEvent<EventSlot, Scientists, Scientists> {
  instruction = { 9 (Influence + Class<Tag>(HAS Tag<Owner>)): 10 }
}

CLASS DryDeserts<EventSlot> : GlobalEvent<EventSlot, Reds, Unity> {
  instruction = { (StartToken: -OceanTile BY Engine) OR Ok, StandardResource / Influence * }
}

CLASS EcoSabotage<EventSlot> : GlobalEvent<EventSlot, Greens, Reds> {
  instruction = { -Plant / Plant - Influence - 3 }
}

CLASS Election<EventSlot> : GlobalEvent<EventSlot, Greens, MarsFirst> {
  instruction = { @resolveElection() }
  metric = COUNT "Influence OR BuildingTag OR CityTile"
  soloInstruction = { TerraformRating / 5 (Influence + BuildingTag + CityTile) MAX 2 }
}

CLASS GenerousFunding<EventSlot> : GlobalEvent<EventSlot, Kelvinists, Unity> {
  instruction = { 2 / 5 (TerraformRating - 15) MAX 5, 2 / Influence }
}

CLASS GlobalDustStorm<EventSlot> : GlobalEvent<EventSlot, Kelvinists, Greens> {
  instruction = { -Heat / Heat, -2 / BuildingTag MAX 5 - Influence }
}

CLASS HomeworldSupport<EventSlot> : GlobalEvent<EventSlot, Reds, Unity> {
  instruction = { 2 / EarthTag MAX 5 + Influence }
}

CLASS ImprovedEnergyTemplates<EventSlot> : GlobalEvent<EventSlot, Scientists, Kelvinists> {
  instruction = { PROD[Energy / 2 (PowerTag + Influence)] }
}

CLASS InterplanetaryTrade<EventSlot> : GlobalEvent<EventSlot, Unity, Unity> {
  instruction = { 2 / SpaceTag MAX 5 + Influence }
}

CLASS MinersOnStrike<EventSlot> : GlobalEvent<EventSlot, MarsFirst, Greens> {
  instruction = { -Titanium / JovianTag MAX 5 - Influence }
}

CLASS MudSlides<EventSlot> : GlobalEvent<EventSlot, Kelvinists, Greens> {
  instruction = { -4 / OwnedTile(HAS Adjacency<OceanTile>) MAX 5 - Influence }
}

CLASS Pandemic<EventSlot> : GlobalEvent<EventSlot, Greens, MarsFirst> {
  instruction = { -3 / BuildingTag MAX 5 - Influence }
}

CLASS ParadigmBreakdown<EventSlot> : GlobalEvent<EventSlot, Kelvinists, Reds> {
  instruction = { -2 ProjectCard, 2 / Influence }
}

CLASS Productivity<EventSlot> : GlobalEvent<EventSlot, Scientists, MarsFirst> {
  instruction = { Steel / PROD[Steel] MAX 5 + Influence }
}

CLASS RedInfluence<EventSlot> : GlobalEvent<EventSlot, Kelvinists, Reds> {
  instruction = { -3 / 5 (TerraformRating - 10) MAX 5, PROD[1 / Influence] }
}

CLASS Revolution<EventSlot> : GlobalEvent<EventSlot, Unity, MarsFirst> {
  instruction = { @resolveRevolution() }
  metric = COUNT "EarthTag OR Influence"
  soloInstruction = { 4 (EarthTag + Influence): -2 TerraformRating }
}

CLASS Riots<EventSlot> : GlobalEvent<EventSlot, MarsFirst, Reds> {
  instruction = { -4 / CityTile MAX 5 - Influence }
}

CLASS Sabotage<EventSlot> : GlobalEvent<EventSlot, Unity, Reds> {
  instruction = { PROD[-Steel, -Energy], Steel / Influence }
}

CLASS ScientificCommunity<EventSlot> : GlobalEvent<EventSlot, Reds, Scientists> {
  instruction = { 1 / ProjectCard + Influence }
}

CLASS SnowCover<EventSlot> : GlobalEvent<EventSlot, Kelvinists, Kelvinists> {
  instruction = { StartToken: -2 TemperatureStep BY Engine, ProjectCard / Influence }
}

CLASS SolarFlare<EventSlot> : GlobalEvent<EventSlot, Unity, Kelvinists> {
  instruction = { -3 / SpaceTag MAX 5 - Influence }
}

CLASS SolarnetShutdown<EventSlot> : GlobalEvent<EventSlot, Scientists, MarsFirst> {
  instruction = { -3 / ActiveCard MAX 5 - Influence }
}

CLASS SpinOffProducts<EventSlot> : GlobalEvent<EventSlot, Greens, Scientists> {
  instruction = { 2 / ScienceTag MAX 5 + Influence }
}

CLASS SponsoredProjects<EventSlot> : GlobalEvent<EventSlot, Scientists, Greens> {
  instruction = { EACH ResourceCard(HAS CardResource) { CardResource }, ProjectCard / Influence }
}

CLASS StrongSociety<EventSlot> : GlobalEvent<EventSlot, Reds, MarsFirst> {
  instruction = { 2 / CityTile MAX 5 + Influence }
}

CLASS SuccessfulOrganisms<EventSlot> : GlobalEvent<EventSlot, MarsFirst, Scientists> {
  instruction = { Plant / PROD[Plant] MAX 5 + Influence }
}

CLASS VolcanicEruptions<EventSlot> : GlobalEvent<EventSlot, Scientists, Kelvinists> {
  instruction = { (StartToken: 2 TemperatureStep BY Engine) OR Ok, PROD[Heat / Influence] }
}

CLASS WarOnEarth<EventSlot> : GlobalEvent<EventSlot, MarsFirst, Kelvinists> {
  instruction = { -TerraformRating / 4 - Influence }
}
```

The regular cards intentionally keep compact Pets-like arithmetic even where current Metrics do
not support addition after a capped term. Election and Revolution instead state custom operations
and retain their metrics as class data. Their tied ranking, chairman replacement, and solo behavior
are not good reasons to add isolated ranking syntax.

### Venus Next and Colonies global events

```pets
// Additional global events used with Venus Next and Colonies.

CLASS VenusInfrastructure<EventSlot> : GlobalEvent<EventSlot, MarsFirst, Unity> {
  instruction = { 2 / VenusTag MAX 5 + Influence }
}

CLASS CloudSocieties<EventSlot> : GlobalEvent<EventSlot, Unity, Reds> {
  instruction = { EACH ResourceCard { Floater<ResourceCard>. }, Floater / Influence }
}

CLASS CorrosiveRain<EventSlot> : GlobalEvent<EventSlot, Kelvinists, Greens> {
  instruction = { -2 Floater! OR -10, ProjectCard / Influence }
}

CLASS JovianTaxRights<EventSlot> : GlobalEvent<EventSlot, Scientists, Unity> {
  instruction = { PROD[1 / Colony], Titanium / Influence }
}

CLASS MicrogravityHealthProblems<EventSlot> :
    GlobalEvent<EventSlot, MarsFirst, Scientists> {
  instruction = { -3 / Colony MAX 5 - Influence }
}
```

These five cards belong to Turmoil but observe components supplied by Venus Next or Colonies. They
should be active only when their required expansion supplies a meaningful component domain; that
is a general premise/projection concern rather than a Turmoil-specific runtime exception.

## Related GlobalParameter ownership question

The base declaration currently says:

```pets
ABSTRACT CLASS GlobalParameter : Atomized {
  This: TerraformRating
}
```

A Player-authored parameter gain supplies a contextual Player, so the effect creates that Player's
mandatory TerraformRating. World Government Terraforming is authored `BY Engine`; Engine is not a
Player, and the current effect machinery silently produces no TerraformRating task when it cannot
obtain a contextual Player.

That outcome is correct for the game, but its present explanation may not be. Investigate whether
the rule should instead state its applicability explicitly:

```pets
This BY Player: TerraformRating
```

Also characterize whether World Government Terraforming ought to fail under the existing authored
rule rather than relying on the missing context to suppress the effect. An AMAP TerraformRating
gain is another possibility to examine, but it must not casually redefine AMAP: an existing target
with zero invariant headroom is different from a missing Player dependency. The investigation is
tracked in [`TODO.md`](../../TODO.md).

## Open language and modeling questions

The draft currently exposes these questions without selecting implementations:

1. **Instruction-valued class properties.** Parties and global-event Classes want immutable
   instruction data that a live component can evaluate in context. Numeric Metric and Requirement
   properties exist; general Instruction properties do not.
2. **Dependency-specialized component behavior.** One exact global-event component should provide
   different behavior while its slot dependency is Distant, Coming, or Current. The effect must not
   migrate to a persistent slot watcher just because that is easier to express.
3. **Invariant-restoration scope.** Changing Times wants the final state to contain exactly one
   Coming and Distant event while a recursive automatic chain temporarily empties each slot.
   Current minimum invariants constrain every individual transmutation.
4. **Party maintenance.** Party Leader and Dominant selection both require maxima, but use distinct
   tie rules and must return or promote concrete delegates without inventing reserve components.
5. **Solar sequencing.** The rulebook orders TR revision, Current event resolution, new government,
   lobby refill, and Changing Times. The components should retain their own behavior even if a
   workflow eventually supplies ordered step signals.
6. **Additive Metrics.** Many global events add Influence after a capped or grouped Metric. Current
   Metric union is deliberately not general arithmetic addition.
7. **Forced reveal.** The next event is predetermined by a shuffled deck. Revealing it inside `::`
   should not become a player choice; the eventual real-card/deck model should supply one concrete
   top card.

Do not implement these as seven isolated Turmoil exceptions. Each is useful only if it follows
from a small general rule that improves the component model.
