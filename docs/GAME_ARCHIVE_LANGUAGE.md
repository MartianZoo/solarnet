# The game archive language

This document records the language's design priorities and the decisions made while developing
its human-readable game archives. It is a design document, not a complete grammar or a claim
that the prototype already meets every goal. Implementation details and current limitations live
in [the archive engineering notes](agents/EXPORT.md).

## The governing rule: never guess what happened

The owner's prime directive is:

> The instructions must absolutely be unambiguous: we must never take the risk that we are doing
> something different from what was expected!

We must never risk executing something different from what
the author intended. If the file leaves materially different interpretations open, importing it
must fail and explain what needs to be stated more precisely. A shorter file is not worth a weaker
meaning.

This rule governs both handwritten files and generated files. An exporter must preserve the
recorded game's meaning, not merely produce text the importer happens to accept. Agreement on a
final score alone does not establish that the intervening choices and inventory changes agree.

The intended conveniences are often called “do what I mean,” but the motivation is to avoid
**dependence on implementation details**. A reader should not have to know whether a particular
reward is currently hidden inside an EACH, whether a card play needs a separate action opener,
or which internal task carries a bill. The file states the intended game operation. The engine
may fill in preparation only when that operation determines it unambiguously.

Uncertainty is not permission to choose a plausible interpretation. Task insertion order, the
first matching alternative, a convenient card-specific exception, or the expected final score
must not settle a choice the file did not settle. Failure is preferable to a plausible wrong game.

## A readable archive, not a recording of API calls

The aim is a clean archival representation of games people actually played. It should be pleasant
to inspect, handwrite, annotate, compare, and replay. The selected format is a custom language with
Pets expressions in its bodies. Executable JavaScript was considered and rejected; a JSON skeleton
was tried and abandoned. General programming constructs and a second JSON/task-log format are
not part of the design.

The archive describes game actions, their choices, and their consequences. It does not preserve
Agent method calls, transient task IDs, event ordinals, or the historical arrangement of internal
helper objects. Refactoring those details should not require rewriting an otherwise unambiguous
record of the same game.

That does not make task state irrelevant. Remaining obligations, selected alternatives, linked
continuations, task priority, ownership, and card identities where tracked can change what happens
next. An archive may stop between actions, even if the next action is already offered. Export
rejects an unfinished action, payment, or selected task.

Differences in attribution between otherwise identical gains are not important by themselves.
Different inventory changes, different choices, different remaining work, or different legal next
steps are important. We do not require identical event ordinals or an identical incidental event
sequence; we do require evidence that an allowed simplification preserves the meaningful game.

## Explicit execution by default

Player autoexecution is not a prerequisite for importing a file. Ordinary player-facing work is
written explicitly unless a defined convenience can infer the preparation without ambiguity.
Administrative execution continues to follow the normal engine rules.

For example:

```text
Pink: PlayCard(ResearchColony) {
    Pay(1 Titanium, 17 MC)
    Colony<Luna>
    PROD[2 MC]
    2 ProjectCard
}
```

The action arguments identify the action and information needed to determine its cost. Choices
that resolve its effects belong in the body. Payment is a separate Pay command inside that body;
putting payment allocations in the action's argument list was superseded.

PlayCard deliberately performs the required action opener when necessary, the PlayCard invocation,
and the named card's movement from its back. Repeating `ResearchColony FROM ProjectCard` would
expose an implementation step without adding a choice. The source still matters: a play can name
a location other than Hand, including a Self-Replicating Robots berth. Granted and nested plays
use the same operation. A nested PlayCard can be followed by Pay and its effects in the enclosing
body; it does not require another level of braces.

UseAction1 and UseAction2 identify card actions. UsePolicy is a standard model action and needs
no party argument. It is not an importer special case masquerading as an action on a party card.

## Parentheses and braces

Parentheses supply a command's inputs: `pay(3 Titanium, 1 MC)`,
`selectTask(Plant THEN Heat)`, `exMachina(-2 MC, PROD[Energy])`, and
`exMachinaDropTask(ProjectCard)`. Each is a complete command, without a body.

Braces contain further archive entries: `playCard(Name) { ... }`, `Research { ... }`, and
`Green { ... }`. Those bodies can contain commands with their own bodies. Commands without
arguments omit parentheses, as in `pass` and `doRequiredActions { ... }`.

Header sections such as `Config`, `GlobalEventDeck`, `Cards`, and `SetupRules` also use braces
for their contents; each header defines what it can contain. Embedded Pets expressions retain
their own grammar, including `EACH ... { ... }`, `PROD[...]`, and expression parentheses.

## Players, turns, and phases

An actor prefix identifies whose operation is being requested. Braces group an action's work or a
phase's work; they do not impose an artificial limit of two actions per braced section. Changes of
acting player identify ordinary turn changes. The model owns the legality of turns and action
counts. Declining an optional second action can be inferred from the next player's action; a decline
at an unfinished endpoint still needs to be represented. Pass is explicit.

Blank lines between distinct action-phase turns matter for readability, although whitespace does
not execute a turn. The last player still acting can continue without invented turn markers.
Final greenery uses the normal action-style presentation with ConvertPlants and an explicit finish.
There is no special final-greenery mini-language.

Research should be a single block per player per generation:

```text
Pink: Research {
    4 ProjectCard<Selecting>
    -2 ProjectCard<Selecting>
    Pay(6 MC)
    2 ProjectCard<Hand FROM Selecting>
}
```

This block includes the draw. There is no reason for the author to split discarding, paying, and
receiving cards merely because the engine creates their tasks at different times. Setup, production,
world government, and global events similarly group explicit work. A Production label does not
implicitly execute a player's remaining production tasks.

Collecting research by player can change the interleaving of draws and purchases in the source.
Such presentation changes must preserve the relevant state, choices, and recorded card identities.
The current replay checks establish that the grouped presentation works for those games; they are
not a proof that arbitrary tasks in arbitrary phases can be reordered safely.

## Pay is a complete allocation, with defined completion

Pay is an established convenience. It gives the complete allocation for the current bill, submits
those allocations in the written order, and declines unused offers as part of completing payment.
It does not choose how to spend the player's resources.

```text
Pay(4 Titanium, 2 MC)
Pay(2 Microbe<Psychrophiles>, 5 MC)
```

Card-held resources use their normal Pets identities. Unused alternatives are declined before the
final remainder payment; a zero remainder is also completed when alternatives cover
the bill. Putting the final MC payment first and Steel after it may fail; the importer must not
silently reorder that allocation. An incomplete, excessive, or otherwise invalid allocation fails.
A failed payment command must restore its own attempted payments and preparation.

## Matching belongs to the shared game interaction

Task matching should behave the same for programmatic callers and archives. Questions such as
whether a world-government task must say BY Admin belong to the matcher/model, not an archive-only
abbreviation table. VIA uses the ordinary task matcher to name a source when that distinction is
necessary.

A recipient prefix can request another player's response inside the current player's action:

```text
Pink: 4 MC
```

This can release matching work held for Pink and perform it through Pink's Agent. Pink's own
pending work remains eligible. The sender's unrelated pending releases must not be executed just
because there happens to be a single release available.

A numeric total can combine compatible pending gains and losses using the shared total rule. It
must describe the whole matching total, including zero-valued work; the importer does not search
for a convenient subset. A total does not authorize consuming rewards that will only be created
by executing its members. For example, explicit final scoring can be `Green { 75 VictoryPoint }`
when that matches the player's complete pending scoring total. Scoring is player work.

## Inferring preparation from the next requested task

The selected direction is to recognize the requested operation through uniquely determined
structural preparation. Expanding a necessary EACH or selecting a uniquely identified OR arm can
remove implementation details from the file. This is shared matching behavior, with Player
autoexecution still disabled.

For example, L1 Trade Terminal may expose its resource gifts through:

```text
EACH @ResourceHolder(HAS CardResource) { CardResource<@ResourceHolder>? }
```

If `Asteroid<RotatorImpacts>` uniquely identifies work inside that EACH, the matcher should expand
that container and execute the requested asteroid task. Other exposed gifts remain pending until
specified. The inference must not execute unrelated gains, select other players' answers, decline
unmentioned optional work, or run arbitrary actions to make the request possible.

OR selection follows the same standard. Given `(Plant THEN Heat) OR (Steel THEN Energy)`, Plant
identifies the first alternative and leaves Heat pending. Given `(Plant THEN Heat) OR
(Plant THEN Energy)`, Plant alone does not identify the continuation. The file must select the
intended alternative explicitly. Flooding's ocean-only and ocean-plus-attack alternatives likewise
must not be conflated merely because both can begin by placing an ocean.

Directly available tasks and tasks reachable through preparation must be considered together.
Trying the first EACH after a failed direct match is not enough: it can miss an ambiguity or choose
a different obligation. Existing selected tasks, priorities, actor assignment, source qualifications,
and shared bindings across THEN still apply. Ambiguous or failed preparation must leave the failed
command unchanged, not leave a half-expanded queue behind.

SelectTask remains the explicit way to preserve a choice that cannot be inferred from the next
requested step. Linked stages are executed separately; executing a named first stage does not grant
permission to run the rest of its THEN sequence.

## The exporter must use the same conveniences

Improving handwritten import syntax alone has very little value if generated archives still expose
all the old scaffolding. Every added deduction should be accompanied by exporter use and tests of
the shorter generated output.

The exporter should consult the same matching rules to establish that omitting an EACH or
SelectTask will reconstruct the intended preparation. It must retain an explicit line whenever
that conclusion is ambiguous, unsupported, or timing-dependent. In particular, delaying an EACH
can change which holders it enumerates or which amounts it calculates. Success on a final-state
comparison alone is not permission to move that expansion.

Round-trip checks compare the meaningful game, including pending work and recorded identities.
Generation checkpoints and inventory-change comparisons strengthen final-state checks. Focused
cases must also prove rejection of ambiguous alternatives and restoration after failed preparation.
A suite of successful full games is necessary evidence, not an excuse to guess in uncovered cases.

## Configuration, unknown cards, and external outcomes

A file needs its header and complete GameConfig, but must not list hundreds of unused cards merely
to describe the resolved catalog. Nor should it require a complete unused deck. Record the external
outcomes that are actually needed, including observed global-event order and named draws where
tracking is enabled. Do not invent unseen card identities or pad missing outcomes.

Untracked cards are the default. A Cards header currently opts into tracked arrivals and departures.
That header is an accepted interim representation. Future notation such as a named ProjectCard
type, or Research arguments containing card names, remains a syntax question rather than a settled
requirement. The file must not imply tracking just because a played card has a name.

SetupRules carries local Pets declarations needed by a game. Archival independence remains a goal:
a file that needs Kotlin-only catalog declarations is not yet self-contained. Whether it uses Canon,
legitimate supplied content, or a documented stand-in must remain honest; the importer must not
invent missing card behavior. Format/rules versioning is deliberately not the present priority.

## Comments, corrections, and reading inventories

Comments are essential. Hand-authored archives can preserve source observations, uncertainty,
explanations, and corrections beside the relevant play. Ordinary exports omit comments. Regenerating
commands must not erase commentary in checked-in authored files.

`exMachina(change, ...)` applies the comma-separated changes as a single correction. It can
appear wherever the game needs it and does not consume an action. It takes no command body.
It must not become a hidden repair the importer uses to make an otherwise invalid archive reach
the expected result.

The long-term goal is to account for user-facing inventory changes directly from the file: a reader
should be able to total a player's plants using arithmetic, without executing the rules. This can
require changes in the model as well as the language. A ruling bonus paid by Admin is a model issue;
manufacturing an apparent player submission in the exporter would misrepresent what happened.

The exact component criterion remains open: Iconic and non-System do not currently mean the same
thing. Constructed tags, delegate changes, and automatic consequences complicate either criterion.
All six Turmoil ruling bonuses, annual TR loss, and chairman TR require further model work; the
owner explicitly deferred the ruling-bonus changes. Preserve those limits instead of claiming the
current archive is already a complete arithmetic ledger.

## AUTO, strictness, and incomplete support

AUTO is low priority and is not implemented. If introduced, it must prove that every legal order
of the covered work, including tasks created along the way, gives the same meaningful result. It
cannot mean “execute the rest in the queue's usual order.” If different orders can differ, importing
must fail until the file supplies more information. Slow proof is acceptable; a heuristic guess is
not. Harmless attribution differences need not make otherwise equal results different.

Strictness applies without AUTO too. Unexpected syntax, unused arguments, wrong phases, invalid
payments, missing recorded outcomes, and unresolved choices fail. Unsupported export states must
also fail rather than silently dropping information. Previously completed commands can remain
completed after a later import failure; the failed command itself must be restored.

REPL work, a formal grammar, and versioning do not need to delay the current design. The acceptance
criterion for a convenience is concrete: it expresses the intended game more directly, has a unique
meaning, is used by the exporter, preserves round-trips, and rejects the cases where that meaning
cannot be established.
