# Language renderer refactoring

> **Status:** Implemented. `docs/agents/LANGUAGE.md` describes the resulting current model.

> **Agent plan:** Refactor the current converter in place. Preserve `English`, the Pets-family
> dispatchers, canonical Describer lookup, and visible raw-Pets escape boundaries.

## Target boundaries

`ComponentDescriber` is passive data. It may provide:

- nouns and their number forms;
- grammatical category or construction enums;
- small lexical fragments such as a track subject;
- exceptional facts that genuinely belong to the component.

It must not inspect Pets nodes, choose aggregation, return clauses or sentences, contain rendering
callbacks such as the current `Requirement`, or encode an undifferentiated change merely because
several operations produce verbs.

`Describers` initially remains as lookup and inheritance infrastructure, but its rendering methods
are migration targets.

## Structured English values

Introduce only the structure required by current changes:

- `Clause`;
- `Predicate`;
- `NounPhrase`;
- `Modifier`;
- `Coordination`;
- a sentence or punctuation boundary.

These are internal values, not a general English framework. Existing public methods continue to
return `String`; one final linearizer performs capitalization, punctuation, agreement, and text
assembly. Raw strings remain appropriate for lexemes and explicit raw-Pets fragments, not partially
assembled clauses.

## First vertical slice: Gain and Remove

Create a focused change renderer that:

1. inspects the Pets change;
2. looks up its component Describer;
3. interprets the Describer's passive construction facts;
4. returns a structured clause.

Model distinct constructions honestly: countable-resource transfer, card-resource addition, track
movement, placement, production change, and fixed special gain. Do not introduce one enum claiming
these are equivalent. Replace the experimental `ChangeWords` during this migration rather than
building around it.

## Parent-owned aggregation

Change `renderInstructionTree` to retain structured clauses until the complete instruction is known.
Then implement:

- adjacent compatible-gain coalescing;
- `Or` predicate factoring;
- coordination under action costs;
- refusal to factor clauses with different predicates, destinations, or modifiers.

Delete `renderInstructionRun` and `renderGainAlternatives`; they are string-era workarounds for lost
structure.

## Bounded migration rounds

After the first vertical slice, move one family at a time:

1. metrics and requirements;
2. actions and costs;
3. triggers and effects;
4. scoring.

Each round changes the active path directly and removes the superseded methods. Do not maintain a
complete parallel converter.

## Completion criteria

The refactoring is succeeding when:

- `Describers` is essentially lookup, inheritance, and lexical access;
- no structural renderer names Foo or any canonical component;
- parents can transform children without parsing strings;
- unsupported Pets remain visible in square brackets at a safe structural boundary;
- current snapshot output remains stable unless deliberately reviewed;
- each round removes a responsibility from the current 864-line `Describers`.

Add integration coverage only for structural distinctions not already exercised canonically,
especially where factoring is and is not valid. Update `docs/agents/LANGUAGE.md` as each planned
boundary becomes committed behavior.
