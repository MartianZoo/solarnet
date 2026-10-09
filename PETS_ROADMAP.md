# Pets roadmap

Pets is the static-model side of the planned repository split: the Pets language and type model,
canonical and fake Terraforming Mars content, card-data generation, derived English, the Almanac,
and supporting generation or analysis tools. Runtime state, the engine, Agents, gameplay workflow,
replays, and Mars Playground belong to the later Solarnet roadmap.

The largest project-wide concern is the complexity accumulated between the engine and functional
tests. That machinery is outside this document, but it changes how work here is judged. A new Pets
API, generated hierarchy, adapter, or derived representation is valuable only if it lets consumers
delete more permanent machinery than it adds. A cleaner-looking extra layer is not progress.

Sections and items are ordered by current importance, but this is not a release schedule.
“Active” means substantial unfinished work exists. “Selected” means the direction has been
chosen but the design may remain open. “Exploratory” means the idea still has to justify its
conceptual cost.

## Internal design and consumer API

1. **Complete the static/runtime separation.** **Selected.** The future Pets repository should
   build and explain its static model without depending on game state or execution. Finish the
   separation now expressed by the dedicated `catalog` module and by Canon, Fake Canon, and Almanac
   having no state dependency: isolate source parsing from the model and continue making consumers
   depend only on capabilities they actually use. Prefer moving a complete responsibility or
   deleting a reverse dependency over adding paired adapters.

2. **Make Pets pleasant to consume directly.** **Selected.** The model should expose a small,
   typed, unsurprising path from declarations to class tables, types, properties, and instructions.
   Separate the expression API's natural, compact-resolved, and full-resolved purposes. Remove
   temporary Canon-facing seams and APIs that expose incidental implementation structure. The test
   for success is simpler real consumers, not a more elaborate facade.

3. **Decide the generated Kotlin API by net simplification.** **Exploratory.** The `codegen` branch
   demonstrates a rich typed hierarchy for canonical Pets vocabulary, but it also introduces a
   second large surface and substantial adapters in functional tests. Retain and integrate it only
   if it replaces stringly helpers, constants, and duplicated interpretation across production
   callers. If it mostly sits above the same machinery, keep the generator isolated or remove it.

4. **Keep a single semantic model.** **Selected.** Execution, English, icons, analysis, and Kotlin
   access must all consume the same declarations and type rules. Precompiled canonical content may
   replace runtime parsing eventually, but it must be output from the real Pets compiler rather
   than a parallel model. Generated metadata should preserve authored meaning and provenance.

5. **Stabilize public contracts only after responsibilities are clear.** Add binary-API checks and
   reduce visibility where they expose accidental details. There are no compatibility clients to
   protect, so improving the design takes precedence over retaining obsolete entry points.

## Code clarity and confidence

1. **Finish the specification-fidelity audit.** **Active.** Reconcile the language and type
   specifications, conformance tests, KDoc, and implementation. Prioritize places where authored
   meaning can silently change: declaration order, round trips, bindings, predicates, defaults,
   properties, and type intersections. When machinery lacks a real witness, try a bounded removal
   before documenting or extending it.

2. **Extract source parsing as an optional module.** **Selected.** Parsing, inline-class lowering,
   and source diagnostics should depend on the Pets model; the model should not depend on the parser
   or Better Parse. Keep canonical source compilation as a separate pipeline concern. This makes the
   library easier to understand and allows independently implemented parsers without widening the
   core model.

3. **Prefer deletion over cleanup around obsolete representations.** Remove stale helpers,
   transitional `CardPack` concepts, redundant transforms, and compatibility surfaces when their
   replacements are established. Do not preserve two ways to express or obtain the same fact.

4. **Improve diagnostics where they protect authoring.** Preserve file and source spans through
   parsing, lowering, generated inputs, and synthesized trees; report the actual invalid token or
   incompatible choice. Excellent diagnostics matter, but they should follow semantic clarity
   rather than create another metadata system.

5. **Pursue performance only when it changes what is possible.** Profile expensive type and
   refinement construction before redesigning it. Small percentage improvements and speculative
   caches do not earn roadmap space; a change that enables materially larger analysis may.

## Language and modeling

1. **Express structural relationships without nominal stand-ins.** **Selected.** Add an honest
   structural conjunction so rules can describe intersections such as an owned tile directly.
   Let refinements name their candidate when nested dependencies must relate to it. These changes
   should retire proxy classes and repeated full expressions rather than coexist with them.

2. **Make ownership explicit and regular.** **Selected direction.** Replace implicit ownership
   insertion with an explicit `OWN[...]` authoring operation, including whole-effect transforms and
   automatic card or map marks. Keep ordinary `Owned` and `Owner` declarations as the semantic
   foundation. Proceed only where inference and repeated special handling actually disappear.

3. **Support honest nested fanout where the game truly has two domains.** Quick Start's
   players-by-resource production is the proving case. Preserve independent recipient decisions and
   avoid turning `EACH` into an ordered loop, workflow mechanism, or general collection language.

4. **Represent events and lifetimes truthfully.** Direct point events should be Signals rather than
   self-transmutations. Cleanup lifetime and log visibility should be independent facts. Preserve
   paired gain/removal reactions while keeping authored self-transmutations forbidden.

5. **Extend properties and defaults only when they replace special cases.** Abstract property
   defaults, instruction-valued printed facts, and richer cardinality remain promising but
   unsettled. Adopt them only after inheritance, querying, binding, and materialization have a
   coherent systemic rule and the result removes existing bridges.

## Canon and game-rule modeling

1. **Keep ordinary game meaning in authored Pets.** Canon should describe cards, maps, goals,
   expansions, and global rules through general language concepts. Bounded custom semantics remain
   acceptable when they are smaller and more honest than a general feature; runtime orchestration
   must not learn card or expansion identities.

2. **Finish the content-selection model.** **Selected direction.** Distinguish source bundles,
   ambient Modules, and individually selectable Content. Support selecting all applicable Content
   from a bundle without treating that group as a gameplay Module. Complete individual Turmoil
   party and map selection, and give shared or replaced content clear provenance.

3. **Derive expansion eligibility from modeled meaning.** A reference to expansion vocabulary is
   not automatically a hard dependency. Determine whether each affected payoff remains viable when
   the expansion is absent, honor explicit guards, and keep opaque cases for review. Avoid
   per-card exception lists or renderer-only flags.

4. **Remove machinery justified only by marginal content.** **Selected.** Retire Mons Insurance,
   Crash Site Cleanup, and Law Suit if doing so permits deletion of attack-history and Actor-value
   reuse machinery. Apply the same scrutiny whenever support for a few minor components creates a
   lasting concept across the model.

5. **Improve broad fidelity through systemic findings, not a defect queue.** Individual card and
   rule defects belong in tests, issues, or focused plans unless they expose a foundational flaw or
   invalidate a major project claim. Expanding official-content coverage is useful chiefly when it
   tests composition or reveals a missing general rule.

## Derived applications of the static model

1. **Finish and integrate derived English.** **Active.** The `english` branch is substantial
   unfinished strategic work, not a prototype to restart. Preserve its typed, visible refusals and
   sparse inherited vocabulary. Reduce semantic decompilation of operational Pets, make recognizer
   outcomes clearer, expose structured diagnostics, and complete the English syntax tree only where
   it improves correctness or reusable composition. Published text is evidence, never the stored
   answer.

2. **Reach the card trifecta after the foundations can support it.** Bring roughly 300 cards to
   correct modeled meaning, strong derived English, and strong derived iconography from the same
   declarations. This is an integrating proof of the design, not the current top cleanup priority
   and not a coverage contest.

3. **Develop iconographic rendering as a real derived view.** The physical card grammar is central
   to Pets' notation, but icon generation is still aspirational. Begin with a representative corpus
   and explicit refusals, using the same semantic structures as English where they genuinely share
   meaning without forcing a common presentation model.

4. **Make Almanac the inspectable face of Canon.** Show normalized declarations, class
   relationships, type information, semantic usage, derived text, and eventually icons without
   starting a game. It should help authors and curious programmers understand the model, not grow
   into a second editor or runtime application prematurely.

5. **Enable static analysis and independent builders.** Support tools that answer useful questions
   about canonical or fan-authored declarations without executing a game. Favor small, composable
   library contracts and trustworthy diagnostics over a plugin framework or speculative support
   for unrelated games.

## Deliberately outside this roadmap

- Simplifying the engine-to-functional-test stack is the highest related project concern, but its
  implementation belongs in the Solarnet roadmap.
- Workflow, Agents, task scheduling, payments, replays, the REPL, and Mars Playground are runtime
  work even when they reveal pressure on Pets APIs.
- Isolated card defects, old open issues, and abandoned stashes are not roadmap commitments.
- Broad official-content coverage, multilingual generation, micro-optimization, and a polished
  player product remain conditional rather than active programs.

This roadmap synthesizes the current priorities in
[`VALUES.md`](docs/agents/VALUES.md), [`TODO.md`](TODO.md), the
[repository introduction draft](docs/pets-repo-draft.md), recent mainline
work, and the active `english` and exploratory `codegen` branches.
