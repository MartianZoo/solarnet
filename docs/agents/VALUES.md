# Project values

All else being equal, we would like to support every card and every rule in the game and its official expansions exactly as the designer dictates that it should work. And we make only rare and small compromises on that goal.

But there is a *lot* more that is important to us than just that.

## 1. Preserve conceptual integrity

- This is a "design project". It's not enough that it works correctly: we are always questing after the cleanest possible set of well-specified global game rules/behaviors from which all that correct behavior will flow *naturally*.
- We prefer to keep effects located directly on the game components that they belong with -- an extremely "object-oriented" approach. The fact that we have a `Photosynthesis` component instead of every `GreeneryTile` just writing `This: OxygenStep` is a very unfortunate concession we were forced into.
- We are willing to make "hacks" for a particular card when we have to. PharmacyUnion is a good example: it genuinely does seem to require a special consideration that *doesn't* follow from the general game rules.
- Adding a custom instruction or custom metric is a last resort! We try to find a way to express the behavior we need through the natural game mechanics alone.
- Verify disputed data and rules against current official materials and authoritative rulings. Do not mistake an unsupported feature for a data discrepancy, silently invent a rule, or copy another engine's behavior as authority.

## 2. Protect simplicity and coherence

- We work very hard to actively minimize the number of concepts, layers, and parallel mechanisms required to understand the system. Consolidation is usually better than adding another wrapper or “spec” type.
- Do not invent requirements or abstractions for hypothetical flexibility. Solarnet should have clean boundaries, but making it truly independent of Canon or suitable for unrelated games does not justify heroics. The realistic extension case is mostly Canon plus more material.
- Prefer one systemic mechanism over scattered hacks, but keep the cure proportional to demonstrated use cases. Hardcoding a narrow fact can be cleaner than building an unused framework.
- Watch for complexity spirals. Large changes should be teased into small, reviewable pieces that are independently beneficial. Revert or stash experiments that are not converging, while salvaging clean preparatory improvements.
- Use precise, stable domain vocabulary. The aspirational vocabulary distinguishes an Authority's complete knowledge, Modules, internal provider bundles, unresolved user intent, and the exact premise of one game.

## 3. Keep Pets and the domain model central

- Pets syntax should be compact, composable, internally consistent, and sensible to a normal reader. Prefer general precedence and Type-variable rules that remove redundant parentheses, repeated Owners, and bespoke syntax.
- In the aspirational model, an Authority, including Canon, is principally a coherent data provider. Selection, filtering, and game-state decisions belong outside its data APIs; custom metrics and instructions are the narrow exception.
- Load only vocabulary a game needs. Merely mentioning an expansion-gated type in a safe context should not drag the expansion's machinery into the game.
- Favor the aspirational model's affirmative, composable Modules: active components add the ambient behavior of the realized game rather than a growing web of negations and special exclusions.

## 4. Spend effort according to project priorities

- After fidelity, value completeness, simplicity, and composability. Supporting official material is worthwhile; fan material, unrelated games, Turmoil, and polished end-user UX are lower priorities unless explicitly selected.
- Performance is secondary to correctness and clarity. First remove repeated work and discarded results by improving structure. Add caching or hotspot optimizations only after understanding the work and preserving a clear, testable abstraction.
- We don't have to preserve any APIs for compatibility; we have no known clients.

## 5. Keep APIs and implementation disciplined

- Make APIs principled, small, type-safe, and consistent. Prefer domain types such as `ClassName` over strings; use consistent factories, argument order, naming, and visibility.
- Public API growth should be deliberate and visible. Use explicit API mode and the narrowest practical visibility. Compatibility with obsolete APIs is not a goal.
- Avoid Kotlin extension functions when possible.
- Preserve parallel logic together when seeing the rule in one place is clearer than distributing it among subclasses.
- Pets or ordinary gameplay input must produce domain exceptions, never programmer-error exceptions. Programmer-error failures are appropriate for impossible states caused by bad Kotlin code.

## 6. Keep documentation and plans useful

- Evergreen documentation should describe the stable current model in the existing document's voice. Keep migration history, temporary mechanisms, agent reasoning, and overly local detail out unless they remain important to readers.
- Update documentation when behavior, setup, commands, or public APIs change. Documentation is part of the implementation.
- Keep `TODO.md` curated and prioritized. Consolidate related work into coherent projects, remove stale or mysterious entries, and avoid recording every stray possibility.

## 7. Work in a controlled, reviewable way

- Review the whole relevant change through the requested lens—especially conceptual clarity—rather than substituting a generic correctness review.
- Keep scope explicit, the working tree understandable, and branches safely caught up. Do not commit without explicit permission.
- Be economical with agent time and experimentation. Automate repeated trials, checkpoint open-ended investigations, and stop when further work is unlikely to change the decision.
- Commit messages should lead with the user-visible or Pets/data-visible accomplishment, then briefly explain the significant implementation work.
- Keep reports concise, ranked, and decision-oriented. Surface important tradeoffs and evidence without narrating routine tool use.
