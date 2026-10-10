# NLG Review of `tfm-text`

`tfm-text` is a narrow symbolic NLG system with strong semantic traceability, explicit failure
behavior, and broad canonical content coverage. Its robustness to changes in how Pets expresses
equivalent semantics is more limited.

The main problem it addresses is not verb inflection but recovery of communicative intent from
executable game machinery. Most of the module’s useful behavior and its main architectural risk
follow from that task.

## What it actually is

A useful description is compiler-style, ontology-aware NLG for a controlled board-game
sublanguage:

`Pets AST + ClassTable → normalization/resolution → game-concept recognition → English syntax tree
→ structural rewrites → card layout and linearization`

That maps reasonably well onto the classical NLG pipeline described by
[Reiter](https://aclanthology.org/W94-0319/), although the stages blur:

- [`ExpressionResolver.kt`](../../src/common/dev/martianzoo/tfm/text/ExpressionResolver.kt#L22)
  resolves types, defaults, contextual `This`, ownership, and keyed semantic arguments.
- [`English.kt`](../../src/common/dev/martianzoo/tfm/text/English.kt#L53) performs content and
  document planning: top versus bottom, action/effect labels, requirements, immediate behavior, and
  scoring.
- The `render*.kt` families recognize domain constructions and lexicalize them.
- `Clause`, `Predicate`, `NounPhrase`, and related classes form a shallow English syntax tree.
- [`rewriteEnglishSyntax.kt`](../../src/common/dev/martianzoo/tfm/text/rewriteEnglishSyntax.kt#L6)
  aggregates adjacent clauses, factors noun heads, distributes steps, and attaches purposes.

It is therefore neither “just templates” nor a general grammar. That distinction is a continuum
anyway, as van Deemter, Krahmer, and Theune argued in
[Real versus Template-Based NLG](https://aclanthology.org/J05-1002/). I would call it a
knowledge-rich microplanner with a small realizer.

## Design strengths

### One semantic source of truth

Deriving wording from the same Pets declarations that drive the game is a central design advantage.
There is no table containing the final prose for every card. Published wording is evidence, not
production data, exactly as the project’s [language design document](LANGUAGE.md#goal) states.

That reduces the risk that executable rules and explanatory text silently diverge.

The expansion test demonstrates this property: an unregistered `Fanium : StandardResource` becomes
“Gain 2 fanium” through existing ontology and realization rules, rather than requiring a
card-specific answer in a lookup table.

### Explicit refusal

Unsupported meaning becomes bracketed canonical Pets through `Clause.RawPets`, accompanied
internally by a typed `Unresolved` reason. It is retained at the narrowest safe location. If
partially verbalizing a compound construction could mislead, the renderer refuses the compound as
a unit. The corresponding behavior is explicitly tested in
[`EnglishTest.kt`](../../test/jvm/dev/martianzoo/tfm/text/EnglishTest.kt#L432).

This makes incomplete coverage visible instead of producing fluent guesses, silent omissions, or a
generic “unsupported” sentence. For rules text, conspicuous incompleteness is preferable to
plausible but incorrect wording.

### Expression resolution

[`ExpressionResolver.kt`](../../src/common/dev/martianzoo/tfm/text/ExpressionResolver.kt#L64) does
not merely read positional arguments. It maps authored arguments onto dependency keys, supplies
semantic defaults, preserves which dependencies were actually authored, normalizes contextual
ownership, and keeps enough identity to produce local anaphora such as “that resource.”

This is semantic-role recovery rather than only string substitution.

### Sparse inherited vocabulary

`ComponentDescriber` supplies facts such as nouns, change frames, trigger frames, payment roles,
and metric constructions.
[`Describers.fact`](../../src/common/dev/martianzoo/tfm/text/Describers.kt#L100) chooses the nearest
provider through multiple inheritance, while
[`validateInheritedFacts`](../../src/common/dev/martianzoo/tfm/text/Describers.kt#L120) rejects
conflicting incomparable facts.

That fits Pets’ class ontology and resembles classification-based generation
and inheritance lexicons such as [DATR](https://aclanthology.org/J96-2002/). A subclass can inherit
most language behavior while overriding one independent channel.

The closed `ChangeFrame` and `TriggerFrame` alternatives in
[`ComponentDescriber.kt`](../../src/common/dev/martianzoo/tfm/text/ComponentDescriber.kt#L43) are also
a clear boundary: another component using an existing linguistic construction is data; a genuinely
new construction requires code.

### Structured English representation

The EST enables structural aggregation without regex-based text rewriting:

- factoring common objects and noun heads;
- coordinating predicates safely;
- distributing a common step count;
- preserving subjects, modifiers, and unresolved descendants;
- delaying agreement and punctuation.

The resulting behavior is not limited to fixed template filling. Deterministic canonical phrasing
is appropriate here because variation is generally unhelpful in technical rules.

### Board-game-specific distinctions

The renderer makes several domain-specific distinctions:

- paying versus spending versus removing;
- “any card” versus “another card” based on the current card’s resource capacity;
- ownership and “that player”;
- action costs versus results;
- immediate, persistent, and endgame effects;
- top/bottom artwork regions and `Action:`/`Effect:` labels;
- omission of unconditional fixed VP because the physical icon already expresses it.

Those are not generic English problems. They reflect the use of cards as communicative artifacts.

## The central weakness: semantic decompilation

Pets is executable semantics, not always communicative semantics. Some player concepts are
implemented as protocols involving markers, barriers, sequencing, and bookkeeping. `tfm-text` must
reverse-engineer those protocols.

One clear example is
[`renderOncePerActionProductionReward`](../../src/common/dev/martianzoo/tfm/text/renderEffect.kt#L419).
It recognizes two adjacent enabling effects, a latch class, its invariant, reset triggers, marker
removal, and a reward body, then recovers the simple player-facing concept “Once per action…”

Likewise, accepted payment resources are reconstructed from adjacent effects in
[`renderAcceptedResourcePayment`](../../src/common/dev/martianzoo/tfm/text/renderEffect.kt#L507).

This amounts to program analysis and introduces brittleness in generation:

- Semantically equivalent Pets encodings can render differently or refuse.
- Reordering operationally independent effects may affect language.
- A harmless implementation refactor can break wording.
- Renderer knowledge becomes a second source of truth—not for the rule itself, but for the
  convention by which engine machinery represents that rule.

The ordered recognizer chain in
[`renderEffect`](../../src/common/dev/martianzoo/tfm/text/renderEffect.kt#L25) illustrates the general
pressure. Each nullable recognizer means “this interpretation did not succeed,” but `null` does not
distinguish:

- definitely not applicable;
- recognized family, unsupported subtype;
- almost matched but violated an invariant;
- ambiguous with another interpretation.

Precedence is therefore encoded by source order, and overlap is hard to audit.

A universal “Description IR” would add another representation of the same facts and is unlikely to
be an appropriate remedy. A smaller alternative is to preserve authored communicative intent for
recurring protocols—payment, once-per-action limitations, delayed rewards—at the smallest semantic
seam where it naturally exists. The renderer should not repeatedly rediscover intent that the
author already knew.

## The advertised two-pass architecture is not yet reality

The project documentation states this directly: the two-pass architecture is a
[target, not current behavior](LANGUAGE.md#where-each-piece-of-logic-belongs).

Today the EST remains porous:

- `NounPhrase.text`, phrase modifiers, and verbs contain substantial raw English.
- Some renderers linearize substructures early.
- Structural rewrites are invoked locally rather than recursively over one complete tree.
- Contextual state such as whether a possessor has already been established still affects semantic
  rendering.
- Similar shapes reached through different renderer families can receive different wording.

I would finish this boundary incrementally, beginning with places where existing strings actively
block aggregation or correct scope. I would not import a general grammar framework merely to
eliminate every string.

## Linguistically, it is intentionally shallow

Compared with [SimpleNLG](https://aclanthology.org/W09-0613/), the surface realizer is modest:

- essentially singular/plural agreement;
- verbs generally supply their own inflected forms;
- explicit plural nouns;
- first-letter `a/an` selection;
- limited attachment and scope representation;
- little explicit tense, aspect, polarity, person, or information structure.

It is far from unification grammar, HPSG, CCG, chart generation, or generate-and-rank systems: there
is no grammar search, feature unification, packed ambiguity, or ranking.

That is mostly appropriate. Terraforming Mars cards form a tiny imperative/declarative sublanguage.
A general grammar would add substantial conceptual machinery for little gain.

Likewise, this is:

- not bidirectional;
- not an English semantic parser;
- not a controlled-natural-language authoring system;
- not a discourse planner;
- not designed for stylistic variation.

Its type-variable coreference is strong, but wider anaphora remains heuristic. The short card genre
makes that acceptable.

## Where linguistic adequacy still slips

A stable grammatical sentence can still be semantically ambiguous. The current Metallurgist output
is:

> Requires that you have 6 titanium or steel production.

The reviewed target says:

> Requires 6 steel and titanium production combined.

The first can suggest either resource’s production must individually reach six. This is an
example of why snapshot success is not by itself evidence of NLG adequacy.

Other smaller concerns:

- Default class-name lexicalization supports open vocabulary but can confidently choose the
  wrong mass/count behavior.
- A few vocabulary entries contain nearly complete procedures rather than genuinely lexical facts.
- `ComponentDescriber` mixes lexical knowledge with semantic adapter facts such as payment roles and
  production offsets. Pragmatic, but its name understates its responsibility.
- Concrete TFM class names still appear in renderer logic outside the vocabulary.
- The manual list used to validate inherited fields must stay synchronized with
  `ComponentDescriber`.
- I would audit number agreement for coordinated noun phrases before using them broadly as subjects.
- Reconstructing `Describers`, its resolver, and complete inheritance validation for every card
  context is needless repeated work; the vocabulary could be compiled once.

## Public API and product concerns

The public
[`EnglishCardTextRenderer`](../../src/common/dev/martianzoo/tfm/text/EnglishCardTextRenderer.kt#L7)
returns only top and bottom strings. Internally, rendering has structured refusal diagnostics, but
the facade does not expose them.

I would expose a diagnostic result or an additional expert API. Brackets tell a human something
failed; they do not let a tool group reasons, locate unsupported nodes, or prevent publication.

The constructor also looks more generic than it is: it accepts an arbitrary `ClassTable` while
hard-wiring the Terraforming Mars descriptions and requiring that table to contain their referenced
classes.

The raw-Pets fallback is useful developer output but poor final player-facing output. A publishing
UI should treat any unresolved node as a review blocker rather than simply printing the brackets.

English-only generation is currently a reasonable scope decision. If a second language becomes a
real requirement, however, the game-concept recognition must be separated more cleanly from English
lexical choices; otherwise every language will duplicate the decompiler.

## What the evidence establishes

The verification evidence is broad but should be interpreted carefully:

- A fresh `./gradlew :tfm-text:test --rerun-tasks` passed all 35 JVM test methods.
- All 532 loaded canonical cards have deterministic snapshots.
- Seventeen cards contain 25 typed unresolved nodes—about 3.2% of cards.
- The selected goal corpus has three unresolved nodes among 69 goals.
- An adversarial reproducible sample of 100 generated cards produced zero region crashes, while 47
  cards contained 73 unresolved nodes.

That last result is not an estimated failure rate: the generator deliberately emphasizes difficult
nesting, refinements, selectors, sequences, and gates. It does demonstrate both graceful
degradation and incomplete compositional coverage.

The snapshot strategy in
[`EnglishTest.kt`](../../test/jvm/dev/martianzoo/tfm/text/EnglishTest.kt#L26) is appropriate for a
deterministic generator. It proves stability, full corpus traversal, and preservation of visible
fallbacks. It does not independently prove semantic faithfulness, fluency, or comprehensibility.
NLG evaluation has long distinguished automatic regression measures from human adequacy evidence;
see [Belz and Reiter](https://aclanthology.org/E06-1040/).

Further testing priorities would be:

- metamorphic tests showing equivalent Pets formulations yield equivalent wording or equivalent
  refusal;
- exact assertion of unresolved node and reason, not only fallback counts;
- a CI crash-safety sample from the random generator;
- small human reviews focused on scope, attachment, and rule comprehension;
- provenance—edition, printing, expansion, image/source—for published-text evidence;
- better per-case diagnostics so one early snapshot failure does not conceal later ones.

I would not optimize BLEU or exact similarity to published cards.

## What I would preserve and change

Retain:

- one semantic source for execution and explanation;
- typed, visible refusal;
- keyed expression resolution;
- inherited sparse facts;
- deterministic canonical wording;
- local semantic views such as events, payments, quantities, and placement;
- structural rather than textual aggregation.

Change in this order:

1. Preserve communicative intent upstream for recurring operational protocols instead of adding
   more decompilers.
2. Make recognizer outcomes and precedence more explicit where overlap is real.
3. Complete the existing EST boundary only where it buys correctness or reusable aggregation.
4. Expose structured diagnostics publicly.
5. Compile/cache vocabulary validation independently of per-card context.
6. Add equivalence and human-adequacy evidence alongside snapshots.

I would not replace any production stage with an LLM. An LLM could suggest candidate paraphrases or
help reviewers discover ambiguity, but final card text needs the determinism, traceability, and
refusal discipline already present here.

The module’s main academic interest lies in semantic interpretation, microplanning, ontology use,
and safe failure rather than broad linguistic coverage. Current evidence supports using it as a
derivation and review system, with human review before publication. Fluent output should not by
itself be treated as publication-ready, and Pets refactoring should not be assumed to preserve
language output.
