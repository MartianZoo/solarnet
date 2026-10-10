# Pets roadmap

Pets is the static-model side of the planned repository split: the Pets language and type model,
canonical and fake Terraforming Mars content, card-data generation, derived English, the Almanac,
and supporting generation or analysis tools. Runtime state, the engine, Agents, gameplay workflow,
replays, and Mars Playground belong to the [Solarnet roadmap](SOLARNET_ROADMAP.md).

The largest project-wide concern is the complexity accumulated between the engine and functional
tests. That machinery is outside this document, but it changes how work here is judged. A new Pets
API, generated hierarchy, adapter, or derived representation is valuable only if it lets consumers
delete more permanent machinery than it adds. A cleaner-looking extra layer is not progress.

The next step below is the selected repo-split work. The remaining sections describe longer-term
directions, not prerequisites for that step or a release schedule.
“Active” means substantial unfinished work exists. “Selected” means the direction has been
chosen but the design may remain open. “Exploratory” means the idea still has to justify its
conceptual cost.

## Next step: separate Gradle builds

**Selected 2026-10-09; not yet implemented.** Keep the current Git repository, but make Pets and
Solarnet independently buildable inside it. Creating separate Git repositories comes later.

The code preparation is already in place: Catalog and the static tools have their own modules;
Canon, Fake Canon, Almanac, and codegen do not depend on runtime modules. State tests no longer
compile Catalog-owned test support, and the Pets browser test runs under Pets rather than Web.
The earlier experiment compiled the two module groups using a Gradle composite, but retained all
source files in both checkouts. It did not prove independent source trees or artifact consumption.

### Developer workflow requirements

Developer comfort is a condition of completing the split. Prove the workflows below while the
builds still share this Git repository; fix cumbersome setup or daily operations before creating
separate repositories. These are acceptance requirements, not claims about the current build.

- **Work against a fixed release.** A Solarnet checkout without Pets source must build using an
  exact Pets version recorded in Git. Keep the consumed Pets libraries on the same release, with
  upgrades made explicitly in Solarnet. The build-separation step proves this with freshly
  published local artifacts; hosted tagged releases follow before the Git split.
- **Edit both projects together.** Select a local Pets build and use composite substitution, so
  editing Pets and rerunning a Solarnet test or application rebuilds the affected dependencies
  without manual publication or copying files. Support opening both builds together in the IDE
  for source navigation, editing, and running tests.
- **Select Pets independently for each working copy.** For example, Solarnet `work1` can use Pets
  `work1`, while Solarnet `work2` uses a different Pets checkout. Persist the selection locally per
  working copy, without changing tracked dependency declarations or other worktrees. Do not infer
  a pairing from branch names or silently select an adjacent directory. The initial combined
  checkout may explicitly include its in-repository `pets/` build by default.
- **Make the selected mode visible.** Report the Pets release or local source path being used.
  Provide an explicit way to verify artifact consumption even when local source is normally
  selected. An invalid source selection must explain how to correct it rather than silently
  switching modes.
- **Keep familiar daily commands.** Combined build, test, and formatting commands must cover both
  builds, and the development server must still serve Viewer, Web REPL, and Almanac together.
  Wire verification tasks explicitly: compiling an included library does not run its tests.
  Document the actual setup, mode-switching, and verification commands when implemented.

### Implementation scope

1. **Give Pets a self-contained `pets/` build.** Move `pets`, `catalog`, `tfm-card-data`,
   `tfm-card-generator`, `tfm-canon`, `tfm-fake`, `almanac`, `pets-tools`, and `codegen` there,
   together with their source, resources, tests, and required documentation. Include the Gradle
   wrapper, settings, conventions, version catalog, lockfiles, and configuration needed to build
   without reaching into the Solarnet tree. Leave runtime modules in the root build. Give the
   included convention builds distinct identities.
2. **Make Solarnet consume Pets libraries by artifact coordinates.** For normal development in
   this checkout, use Gradle composite substitution to build those libraries from local Pets
   source. Also provide a way to disable substitution and use built artifacts. Keep Kotlin package
   names and behavior unchanged; this is build separation, not an API redesign.
3. **Preserve the combined development workflow.** Root build, test, and formatting commands and
   CI must still cover both halves. Keep the existing JVM and browser coverage, including the
   Pets browser test and Web's history test and selected replay. Adapt `webAppsDevelopmentRun` so
   Viewer, Web REPL, and Almanac still run together: Viewer currently starts Almanac tasks in the
   same build and reads JavaScript from the shared output layout. Update affected build and setup
   documentation with the actual commands.

### Acceptance checks

- Copy only the Pets half outside this checkout. Build and test it there, including its JavaScript
  targets and Almanac packaging. Publish the required JVM/JS libraries and dependency metadata to
  a temporary local Maven repository.
- Copy only the Solarnet half to a different location, with no Pets source tree. Disable composite
  substitution, resolve Pets dependencies exclusively from those freshly built artifacts, and
  build, test, and package the runtime applications there. Neither isolated build may read source
  or build support from the original checkout or the other half.
- Verify normal composite development in the combined checkout: root checks cover both builds,
  and the shared development server serves all three applications. Record the commands and
  results; compilation through a composite alone does not satisfy the isolated-build checks.
- Open the combined builds in the developer's IDE and verify source navigation and editing across
  them. Make a temporary Pets change exercised by a Solarnet test or application, rerun it, and
  confirm that the local edit is used without publishing. Restore the temporary change afterward.
- Demonstrate independent Pets selections in two disposable working copies, including an external
  Pets source path. Changing either selection must leave the other unchanged. Check the reported
  mode and the failure message for a missing selected checkout.
- From a working copy normally using local Pets source, explicitly select artifact verification
  and confirm that it uses the recorded version. CI must exercise both source composition and
  artifact consumption; a successful composite build cannot establish that publication works.

### Release workflow before the Git split

Keep releases lightweight: a Pets tag should trigger testing and publication of the matching
JVM/JS libraries and dependency metadata, with an immutable version Solarnet can record. Routine
library releases should not require manual release notes or repeated publication steps. Choose
the publishing host and version convention later; automate and verify this path before creating
separate Git repositories.

A change to both a Pets API and its Solarnet callers can be developed and tested together, but
separate repositories require separate commits. Document the landing order: test the pair, land
and publish the Pets change, then update Solarnet's pinned version and verify artifact consumption
before landing the dependent Solarnet change. Automation should expose the commits and versions
being tested or published. The split does not remove this coordination cost.

**Not part of the initial build-separation step:** creating the Git repositories, setting up hosted
releases or choosing their version policy, extracting the parser, or redesigning Pets APIs. Parser
extraction remains a separate library-design direction below, not a prerequisite for the split.

## Internal design and consumer API

1. **Make Pets pleasant to consume directly.** **Selected.** The model should expose a small,
   typed, unsurprising path from declarations to class tables, types, properties, and instructions.
   Separate the expression API's natural, compact-resolved, and full-resolved purposes. Remove
   temporary Canon-facing seams and APIs that expose incidental implementation structure. The test
   for success is simpler real consumers, not a more elaborate facade.

2. **Decide the generated Kotlin API by net simplification.** **Exploratory.** The isolated,
   game-independent `codegen` module demonstrates a rich typed hierarchy for Pets vocabulary, but
   adopting that output would introduce a second large surface. Integrate it only if it replaces
   stringly helpers, constants, and duplicated interpretation across production callers. If it
   mostly sits above the same machinery, keep the generator isolated or remove it.

3. **Keep a single semantic model.** **Selected.** Execution, English, icons, analysis, and Kotlin
   access must all consume the same declarations and type rules. Precompiled canonical content may
   replace runtime parsing eventually, but it must be output from the real Pets compiler rather
   than a parallel model. Generated metadata should preserve authored meaning and provenance.

4. **Stabilize public contracts only after responsibilities are clear.** Add binary-API checks and
   reduce visibility where they expose accidental details. There are no compatibility clients to
   protect, so improving the design takes precedence over retaining obsolete entry points.

## Code clarity and confidence

1. **Finish the specification-fidelity audit.** **Active.** Reconcile the language and type
   specifications, conformance tests, KDoc, and implementation. Prioritize places where authored
   meaning can silently change: declaration order, round trips, bindings, predicates, defaults,
   properties, and type intersections. When machinery lacks a real witness, try a bounded removal
   before documenting or extending it.

2. **Extract source parsing as an optional module.** **Selected; independent of the repo split.**
   Move `Parsing`, `DerivedClassLowerer`, the parsed system-declaration provider, and source
   diagnostics into a parser module that depends on the Pets model. Keep Better Parse with source
   input, not the model. The model construction API already supports independent parsers. Canonical
   content still needs separate build-time conversion to typed declarations before its consumers
   can omit runtime parsing entirely.

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

2. **Make ownership explicit and regular.** **First migration implemented; simplification remains.**
   OWN marks are explicit in compiled and authored Pets, and OWN/PROD share transform handling.
   Continue only where inference and repeated special handling actually disappear. The
   [ownership note](docs/agents/OWNERSHIP.md) owns the current design, next steps, tentative ideas,
   and choices needing human review.

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
work, the active `english` branch, and the exploratory `codegen` module.
