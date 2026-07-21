# Bundles and game options

This document defines the configuration model. It deliberately does not describe a migration or
implementation sequence.

## The model

Starting a game requires an `Authority`, exact `GameOptions`, and a `GameAssembler`.

An **Authority** defines the complete environment in which games may be assembled. It provides a
closed set of **Bundles** whose combined contents are self-consistent. `Canon` is the only important
Authority for now. A future Authority may add personal bundles to Canon, replace canonical bundles,
or provide an unrelated environment.

A **Bundle** is only a unit of data ownership, provenance, and distribution. It may contain Pets
declarations, JSON definitions, custom implementations, and metadata. Bundle presence has no game
meaning and is not observable through Pets. A bundle is neither a game option nor a live component,
even when their names happen to coincide.

**GameOptions** fully specify one game without randomness. They select actors, rule options, maps,
individual definitions, content groups, vocabulary, and any exact setup values. A client may make
random choices, apply convenient defaults, or accept short option codes, but it must resolve those
choices before constructing GameOptions. This is the conceptual role; it does not preserve the
shape or responsibilities of the current `GameOptions` data class.

A **GameAssembler** combines one Authority with GameOptions and produces the `MClassTable` for that
game. It owns content selection, replacement, class reachability, phantom-type policy, and all
configuration compatibility decisions. This is the proper home of the existing “slurping” policy
that examines selected definitions to decide which classes the game actually needs.

`Ruleset` is not a fourth configuration layer. The responsibilities currently associated with that
name divide between the Authority's available catalog and the GameAssembler's game-specific output.

## Authorities and bundles

An Authority validates its entire available environment before any game is assembled. In
particular:

- every ordinary class name must be known;
- bundle dependencies must be satisfied;
- identical declarations from several bundles may coalesce, while conflicting declarations are an
  error;
- definition identities, replacement targets, and provider metadata must be coherent.

This validation distinguishes a known but inactive name from a typo. A game may leave `Colony`
inactive, but an Authority containing `Colnoy` should be invalid.

Bundle dependencies are hard environment dependencies, not option implications. The Terra Cimmeria
bundle depends on the Colonies bundle because its rules mention Colonies vocabulary. Therefore an
Authority cannot provide Terra Cimmeria without also making the Colonies data available. This does
not enable the Colonies game option in any assembled game.

The Authority is also the boundary for future customization. “Canon plus my content” means a new
Authority whose bundles are validated together; it does not mean mutating Canon or a running game.

Every canonical Pets file, JSON definition, and custom implementation belongs to a bundle, including
unsupported data. The shared runtime declarations in `pets/system.pets` do not. JSON definitions
derive their provenance from the bundle that reads their directory rather than carrying raw bundle
attributes. Canonical bundle directories follow the `StandardFormBundle` resource contract so JVM
and JavaScript discover the same supported files and report unexpected ones.

## GameOptions and active content

GameOptions are an exact signed selection. Positive selections may contribute defaults, while
explicit negative selections mask those defaults before expansion. Selectors may denote different
kinds of things:

- a rule option such as `SoloMode` or `ColoniesExpansion`;
- a map or a map's milestone set;
- an individual card, milestone, award, or colony tile;
- a named content group;
- a vocabulary class that should be active even without its usual expansion option.

The common text representation is one selector per line, with `-` marking a counteraction:

```
Player1
CorporateEra
ElysiumMap
PreludeExpansion
VenusNextExpansion
-Hoverlord
Io
-TradeEnvoys
```

Counteractions take precedence independently of file order and mask defaults before those defaults
are expanded. They are not cleanup applied after content has loaded. An independently selected item
from a counteracted group does not re-enable the group or its other defaults; its own semantic
requirements must still hold.

Selecting a definition makes it active content; it does not gain an instance of its class. Enabling
a rule option may create a singleton component before `SetupPhase` so its behavior can be ordinary
Pets rules. Selecting vocabulary is not inherently a rule option: activating `VenusTag` need not
mean enabling the Venus track, World Government Terraforming, or all Venus cards.

Semantic requirements refer to GameOptions, never to bundle presence. Filtering and replacement
happen after signed selections and defaults are resolved but before class assembly. Replacement is
same-kind, transitive, and based on stable definition identities. Replacement targets must be known
to the Authority; cycles and multiple applicable replacements for one target are errors.

Configuration constraints and option-specific setup behavior should be ordinary Pets rules where
that is natural. For example, the selected map can be constrained to exactly one and the Colonies
option can validate and interpret the exact selected colony names. General compositional workflow
changes may remain explicit until the language has a convincing model for them.

## Game assembly and class reachability

The GameAssembler begins with roots supplied by the resolved GameOptions: option singletons,
selected definitions, actors, explicitly selected vocabulary, and shared runtime classes. It then
follows **activation edges** from those roots. Structural needs such as supertypes and dependency
signatures are activation edges; a behavioral mention is not automatically one.

This gives class references a crucial property: a reference may use content already selected for
the game, but it cannot select that content. Mentioning `Colony` in an instruction must not activate
Colonies, just as mentioning `VenusTag` on one selected card must not activate Venus Next.

`AutoLoad` is interpreted within this same active-content policy. Merely making a provider bundle
available must not activate every `AutoLoad` declaration it contains.

The assembler must distinguish three sets:

1. names known to the Authority;
2. active classes with loaded declarations and behavior;
3. known but inactive classes represented as phantom types.

APIs that enumerate playable or creatable concrete types use only the second set. Type parsing and
validation may use all three.

## Phantom types

A **Phantom Type** is known to the MClassTable because the Authority validated its name, but its
class is inactive in this game. It retains enough catalog information to validate its type shape
and relationships, but contributes no components, effects, defaults, invariants, or class-literal
component.

Phantom types have these semantics:

- counting the type yields zero;
- counting `Class<Phantom>` yields zero;
- they are excluded from subtype enumeration, automatic narrowing, and player choices;
- an optional or AMAP gain/removal succeeds at quantity zero;
- a mandatory gain/removal is impossible and may be normalized early to `Die`;
- a trigger on a phantom type can never fire;
- an unknown name that was not validated by the Authority is an error, not a phantom.

This general rule replaces special handling for unresolved class literals. `Class<Colony>` and
`Colony` are both valid, resolvable queries with count zero when Colony is phantom.

Turning atomic changes into `Die` lets ordinary Pets composition express optional-content rules.
Choices discard dead branches, while an unavoidable dead instruction remains a real incompatibility.
The assembler should reject selected content whose required initialization or self-creation reduces
to an unavoidable `Die`, and report the inactive class that caused it.

Structural absence is stricter. An active class cannot have a phantom superclass or an unusable
dependency signature; that is a failure to assemble the game rather than behavior to defer until
play.

## Governing examples

### Terra Cimmeria and Colonies

The map data maps its special letter only to `TcColonyBonus`; that type owns the conditional rule.
The Terra Cimmeria bundle has a hard provider dependency on the Colonies bundle, so every supporting
Authority knows `ColoniesExpansion` and `Colony`.

Without the Colonies option, those classes are phantom. Their class-literal metrics count zero and
the mandatory colony branch becomes `Die`, leaving the no-op branch. With Colonies enabled, the
classes are active and the bonus means paying 5 M€ and placing a colony.

### Selected Venus cards without Venus Next

An Authority may provide the Venus bundle while GameOptions select a few Venus cards but not the
Venus Next option. Bundle availability alone activates nothing. If `VenusTag` remains phantom, the
cards' mandatory tag creation becomes `Die`; the assembler should detect that those cards cannot be
constructed correctly and reject the selection.

A future configuration could explicitly activate `VenusTag` as vocabulary without enabling the
Venus track, or deliberately transform the selected cards to remove their Venus tags. Those are
content-selection features, not bundle-loading behavior.

### Colonies workflow ordering with Venus

As workflow rules become compositional, the Colonies bundle may state that when the Venus expansion
is in use, `ColoniesSolarPhase` cannot occur until `VenusSolarPhase` has completed. Because that
rule names Venus vocabulary, Colonies has a hard provider dependency on the Venus bundle. The
dependency guarantees that an Authority containing Colonies can validate the rule; it does not
enable Venus in any game.

Without the Venus option, `VenusNextExpansion` and `VenusSolarPhase` are phantom and the guarded
ordering rule is inert. With Venus enabled, they are active and the same rule enforces the phase
ordering. The Colonies bundle therefore contains one valid rule that is innocuous under every valid
GameOptions selection, rather than separate bundle variants or workflow code paths.

## Invariants

- Authorities define valid available environments; GameOptions define games.
- Bundle dependencies affect availability only and never imply game options.
- GameOptions are exact and non-random.
- Behavioral references do not activate provider content.
- Known inactive classes are phantom; unknown names are errors.
- The MClassTable represents exactly one assembled game and never expands after assembly.
