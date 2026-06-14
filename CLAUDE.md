You should take advantage of the available documentation:

When you need to understand project basics, read README.md.

When you need to understand what code lives where, read docs/packages.md.

When you need to understand how game state is represented (components, multisets, the everything-is-a-component philosophy), read docs/language-intro.md for the big picture and docs/component-types.md for the specific classes (tiles, resources, cards, TR, phases, etc.).

When you need to understand the Pets type system (abstract vs. concrete types, generic dependencies, variance, This, Class<X>, singleton types, production encoding), read docs/type-system.md.

When you need to understand Pets syntax (instructions, requirements, actions, effects, per/gated/or, intensity modifiers !/?/., PROD[...], FROM), read docs/syntax.md and docs/cheat-sheet.md for quick lookup.

When you need to understand how the engine executes instructions (ComponentGraph, TaskQueue, EventLog, Timeline, Instructor prepare/execute pipeline, Effector triggers, Limiter, auto-exec, the Gameplay API layers, ApiTranslation, Koin wiring, TfmWorkflow coroutine), read docs/engine.md — it is thorough and current.

When you need to understand obscure design decisions (why atomization matters, why PharmacyUnion uses ::, how event cards work, OxygenStep's OR Ok trick), read docs/game-insights.md.

When you need to understand the project's goals, non-goals, and priorities, read docs/faq.md.

