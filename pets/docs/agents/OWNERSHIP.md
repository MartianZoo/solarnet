# Lexical ownership

> **Status:** current elaboration rules and explicitly labeled saved experiments.
> Task assignment and event attribution belong to [Solarnet](../../../docs/agents/IDENTITY.md).

## Lexical ownership

`Anyone` is the common supertype of `Owner` and `Actor`. `Player` is both; Admin is only an Actor;
SoloOpponent and Neutral are only Owners. Use bare `Owned` in subclass declarations unless
narrowing its bound or linking a variable. Use literal `<Anyone>` for unrestricted ownership and
trigger-side `BY Anyone` for every performer.

`Owned<Me@Owner>` gives the owner dependency an inherited lexical name. Subclasses can narrow that
same binding, such as `Me@Player`. Independently inherited bindings with the same name are
ambiguous unless they reach the same dependency. A marked `EACH Me@Player` or
`RANK Me@Player` rebinds `Me` inside its body; an unmarked selector preserves the outer binding.
`Player(HAS ... RANK { ... })` preserves it too. Ranking supplies compatible omitted metric
dependencies for each competitor before ownership insertion, including through `EVAL`; explicit
`Score<Me@>` still refers to the outer owner. This fills only outermost metric expressions;
it does not descend into their arguments or refinements. Award scoring retains explicit
`RANK Me@Player` for nested ownership in Collector and Excentric. See language rule L4-8.

`Me@` and other named references may omit their type when the visible name selects a unique
binding, as specified by language rule L1-7. The supplying occurrence still writes its type. An
`EVAL` captures the lexical `Me` from its evaluation site; ordinary Type-variable specialization
carries that capture through deferred fanout and ranking. The engine does not reconstruct an owner
from a selected component or event Actor.

A `HAS` candidate fills a compatible omitted dependency before lexical `Me` is inserted. Thus
`EACH Starter@Player(HAS StartToken) { ... }` tests each candidate's token. An incompatible
candidate leaves the owner open for lexical insertion.

An ownerless rule may infer `Me` from an owned trigger or, for a non-System event, its Player
actor. Elaboration rejects an attempt to infer that Player from an ownerless System event;
`Phase: Plant` needs an explicit recipient when `Phase` is System. A System event with an owned
dependency can still supply the recipient through that dependency.

### Ownership transition: current code and saved experiments

The selected stopping point keeps implicit ownership and the useful fixes independent of OWN.
Current Canon does not justify general support for arbitrary transform combinations. The
`ownership` branch remains the place to pursue making Owned and Owner ordinary; its existing
prototype and `docs/agents/OWNERSHIP.md` are the starting point, not a discarded direction.
Do not merge that prototype into current code as incidental cleanup.

Making inferred Actor and recipient bindings explicit belongs to that transition, not to the
independent fixes kept here. Adding bare `BY Player` to the 16 audited rules only duplicated an
existing filter while leaving the actual binding implicit; those edits were removed. For future
work, prefer the fully explicit relationship, for example:

```pets
TemperatureStep BY Me@Player IF =3 TemperatureStep: PROD[Heat<Me@>]
```

This states both who performed the event and who receives the reward. It is a useful target for
what OWN could eventually insert, not an approved migration of current Canon.

The later experiment in supporting deferred transforms through EVAL and preserving transforms
around extracted effects/actions is saved in local annotated tags:

- `archive/transforms-before-main`: snapshot whose full build had passed before merging main.
- `archive/transforms-after-main`: snapshot with unfinished integration after merging main.

These tags point to stash commits: their main trees contain tracked files; their third parents
contain the files that were untracked. Preserve both when recovering an experiment. Neither tag
selects this machinery for current code, and the later snapshot need not be repaired for archival
purposes. The experiment exposed the cost of carrying authored transform wrappers through source
copying, class extraction, property evaluation, and consumers expecting concrete effects/actions.
Resume it only for a demonstrated need, or as part of the deliberate ownership redesign.

An October 10 audit at `fba575219` scanned all 1,581 compiled Canon classes, including generated
cards. None of its 11 distinct RANK expressions had a selector refinement; no HAS refinement
contained a Requirement.Eval. The EACH/RANK selector-scope disagreement and lost HAS-EVAL candidate
therefore had no occurrence in that catalog. Award metrics evaluated inside RANK are a different
case. The user selected leaving both questions documented without changing behavior. These are
observations about current content, not restrictions on future content.

The same temporary audit elaborated effects in solo and two-player premises across all seven maps,
with the supported expansion/card packs selected. Trigger inference is actively used by placement
bonuses, standard projects, colony rewards, phase offers, and global-parameter rewards. In
particular, `GlobalParameter`'s `This: TerraformRating` acquires `BY Me@Player`, excluding Admin
events. That dependency does not itself demonstrate incorrect gameplay; removing inference needs
a deliberate migration. The audit could not elaborate WildlifeDome's `GreeneryTile<>`, which has
no gain dependency default to accept; that separate content defect limits the elaboration sweep.
The user selected recording WildlifeDome for later rather than fixing it during this work.

**Potential later simplification, not selected for implementation:** stop supplying a missing player
from the Actor merely because an effect's instruction needs an owner. A temporary engine probe with
trigger inference disabled confirmed that `Pulse: Temperature, Plant` responds to Admin with both
tasks, leaving `Plant!` abstract. The caller can choose `Plant<Player2>` and complete it. This exposes
an unresolved recipient instead of silently filtering the whole effect, but does not itself reject
an authoring mistake. Decide separately whether Canon needs such an open choice or whether these
declarations should fail. Inference from an owned trigger's recipient is a separate question.
