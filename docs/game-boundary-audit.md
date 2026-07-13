# Game Boundary Audit

This audit reviews whether an unrelated board game with suitable mechanics could reuse all
Solarnet code outside `dev.martianzoo.tfm` while using none of the code inside
`dev.martianzoo.tfm`.

The boundary is not ready yet, but the misplaced behavior is concentrated in a manageable set of
seams. This report focuses on where behavior belongs. Backward dependencies are useful evidence,
but are not independently inventoried unless the code at that location actually implements
Terraforming Mars behavior.

Terraforming Mars-flavored comments, examples, and tests are also excluded unless they affect
runtime behavior.

## Outside `tfm` but game-specific

### P0: Bare numbers in Pets intrinsically mean megacredits

`pets/src/commonMain/kotlin/dev/martianzoo/pets/ast/ScaledExpression.kt` imports `MEGACREDIT`; its
default expression, constructors, and rendering all treat megacredits as the implicit unit.

Thus `5`, `X`, effect payouts, costs, and similar syntax carry a Terraforming Mars meaning in the
generic AST itself. Another game cannot define its own implicit currency, or have no implicit
currency, without replacing this class's behavior.

The default should be supplied by a game-specific language profile, or omitted expressions should
remain distinct in the AST until game-specific preprocessing.

### P0: The generic engine initializer performs Terraforming Mars setup

`engine/src/commonMain/kotlin/dev/martianzoo/engine/Initializer.kt` contains:

1. Colonies bundle detection.
2. Colony-tile selection and `AddColonyTile` execution.
3. `TradeFleetA`, `TradeFleetB`, etc. creation.
4. An unconditional `SetupPhase` transition.

The generic portion—creating `Engine` and singleton components—is correctly placed. Everything
after that should be a setup hook implemented by the game layer.

### P0: The generic class loader contains Colonies-specific reachability rules

`engine/src/commonMain/kotlin/dev/martianzoo/types/MClassLoader.kt` specially loads colony tiles,
`TradeFleet` subclasses, and `DelayedColonyTile` when bundle `C` is active.

This is not merely a dependency problem: it is Terraforming Mars content-selection policy
implemented inside the generic type system. The setup/game layer should provide the complete
initial class-name roots, after which `MClassLoader` should only perform generic transitive loading.

### P0: Generic turn and action APIs encode the Terraforming Mars signaling protocol

The protocol is split across ostensibly generic code and Terraforming Mars canon data:

1. `pets/src/commonMain/kotlin/dev/martianzoo/pets/Transforming.kt` converts every `Action` into an
   effect triggered by `UseAction1<This>`, `UseAction2<This>`, etc.
2. `pets/src/commonMain/kotlin/dev/martianzoo/api/SystemClasses.kt` declares `USE_ACTION` as an
   engine-significant name.
3. `engine/src/commonMain/kotlin/dev/martianzoo/engine/Implementations.kt` implements generic
   `startTurn()` as `NewTurn<Player>!`.
4. `engine/src/commonMain/kotlin/dev/martianzoo/engine/ApiTranslation.kt` implements `turn` by
   initiating `NewTurn`.
5. The actual `UseAction1..3`, `NewTurn`, `SecondAction`, and `Pass` definitions live in
   `canon/src/commonMain/resources/dev/martianzoo/tfm/canon/actions.pets`.

There are two principled choices: make this a documented generic engine protocol and move its
foundational declarations into the generic kernel, or move `TurnLayer`, action-to-effect
translation, and these conventions under `tfm`. The current half-and-half placement is the
problem.

### P1: The nominally generic player model is the Terraforming Mars 1–5 player model

`pets/src/commonMain/kotlin/dev/martianzoo/data/Player.kt` accepts only `Engine` and `Player1`
through `Player5`, publishes exactly those five constants, and constructs lists from that fixed
set.

This limitation propagates into effect ownership and engine scope creation. A compatible
six-player game, a game with named factions, or a game with multiple non-player authorities cannot
use the generic player abstraction as-is.

`Player` itself should accept configured actor identities. The Terraforming Mars setup layer can
retain the five-seat restriction.

### P1: Terraforming Mars's `PROD[...]` extension is automatically installed by generic machinery

`Prod` itself is correctly under `tfm`, but generic processing decides that every game and custom
instruction passes through it:

1. `engine/src/commonMain/kotlin/dev/martianzoo/engine/ApiTranslation.kt` puts `Prod.deprodify` in
   the standard input preprocessing chain.
2. `engine/src/commonMain/kotlin/dev/martianzoo/engine/Instructor.kt` applies it to custom-class
   output.
3. `engine/src/commonMain/kotlin/dev/martianzoo/types/MClass.kt` applies it while preparing class
   effects.

Because `Prod` returns a no-op transformer when the relevant classes are absent, this is less
immediately obstructive than the earlier findings. Still, game-specific transform selection
belongs in a configured preprocessing pipeline. That same extension point would let another game
install its own syntax transforms.

### P1: Most of the generic script application is actually the Terraforming Mars application

`script/src/main/kotlin/dev/martianzoo/script/ScriptSession.kt` hard-wires:

1. `Canon.SIMPLE_GAME`.
2. `GameSetup`.
3. `TfmWorkflow`.
4. Terraforming Mars commands and board/map views.
5. Terraforming Mars colors for generic access modes.
6. Terraforming Mars bundle and phase information in the prompt.

More Terraforming Mars behavior is spread through generic command/support packages:

1. `script/src/main/kotlin/dev/martianzoo/script/Access.kt` defines any phase change as
   `${name}Phase FROM Phase`.
2. `script/src/main/kotlin/dev/martianzoo/script/commands/NewGameCommand.kt` understands
   Terraforming Mars expansion letters and `GameSetup`.
3. `script/src/main/kotlin/dev/martianzoo/script/ScriptCompletionSources.kt` knows the six
   Terraforming Mars resources, playable `CardDefinition`s, maps, expansion combinations, phases,
   and `PROD`.
4. `script/src/main/kotlin/dev/martianzoo/script/commands/HelpCommand.kt` mixes the generic command
   catalog and Terraforming Mars commands.

The reusable script shell, command dispatch, generic query/task commands, and completion engine
belong where they are. A `TfmScriptSession` or injected application profile should supply setup
creation, commands, completion sources, workflow, prompt metadata, and colors.

### P2: The non-`tfm` REPL mixes reusable JLine plumbing with REgo application behavior

`repl/src/main/kotlin/dev/martianzoo/repl/JlineRepl.kt` constructs the concrete `ScriptSession`,
uses `.rego_history`, recognizes `rebuild` as a launcher protocol, and delegates
Terraforming-Mars/application help.

`script/src/main/kotlin/dev/martianzoo/script/ScriptServer.kt` similarly constructs the concrete
session and emits REgo-specific messages.

This is not specifically Terraforming Mars logic, but another game would not reuse it unchanged.
The JLine loop, history adapter, socket server, and completion adapter are reusable; main-method
wiring and product branding belong in an application package.

## Inside `tfm` but generally reusable

### P0: The fundamental Pets/engine runtime classes live in Terraforming Mars canon resources

`canon/src/commonMain/resources/dev/martianzoo/tfm/canon/system.pets` defines:

1. `Component`, the root of every game's component hierarchy.
2. `Class`.
3. `System`, `Temporary`, and `Signal`.
4. `Ok` and `Die`, which generic AST/task/engine code understands specially.
5. `Engine`.
6. `AutoLoad`, `Custom`, and `Atomized`, all consumed by generic engine/type logic.

These are not Terraforming Mars canon. They are the Pets runtime prelude. Another game would need
them even if it used no `tfm` package, and the generic engine cannot meaningfully operate without
equivalent declarations.

This file should become a generic Pets prelude resource, loaded independently of any game
authority. Terraforming Mars canon should extend that prelude with `global.pets`, `player.pets`,
and the rest.

### P1: `TfmAuthority` contains the missing reusable base implementation of `Authority`

`pets/src/commonMain/kotlin/dev/martianzoo/tfm/api/TfmAuthority.kt` mixes two categories:

1. Generic behavior: declaration aggregation, duplicate detection, validation of `Component` and
   `Class`, definition-to-declaration conversion, name indexes, custom-class lookup, and an empty
   test authority.
2. Terraforming Mars registries: cards, milestones, colony tiles, standard actions, and Mars maps.

The generic half should be something like `AbstractAuthority` or `DefinitionAuthority`.
`TfmAuthority` can extend it and contribute its game-specific definition collections and indexes.

### P1: `GameSetup` contains a useful generic setup model embedded in a game-specific class

`pets/src/commonMain/kotlin/dev/martianzoo/tfm/data/GameSetup.kt` combines:

1. Generally useful setup information: authority, selected content bundles, actors, and selected
   definitions.
2. Terraforming Mars policy: mandatory base/map bundles, exactly one Mars map, 1–5 players, and
   Colonies tile selection.

A generic setup interface/value should expose authority, participating actors, initial class
roots/definitions, and initialization behavior. The current `GameSetup` can remain as the
Terraforming Mars implementation.

### P1: The reusable asynchronous workflow driver is buried inside `TfmWorkflow.Auto`

The phase sequence and end condition in
`engine/src/commonMain/kotlin/dev/martianzoo/tfm/engine/TfmWorkflow.kt` are correctly
Terraforming-Mars-specific. But the surrounding machinery is general:

1. Workflow lifecycle and single-launch enforcement.
2. Queue-drained signaling through `game.onAtomicComplete`.
3. Suspending until player tasks drain.
4. Checkpoint ownership and rollback during shutdown.
5. Coroutine cancellation and cleanup.

Extracting a generic workflow runner would let another game express its own sequence without
rebuilding the subtle queue/coroutine/rollback integration.

### P3: Small generic presentation helpers are trapped in Terraforming Mars UI classes

1. `script/src/main/kotlin/dev/martianzoo/tfm/script/TfmColor.kt` contains a generic hex-to-ANSI
   foreground renderer inside a Terraforming Mars color enum.
2. `script/src/main/kotlin/dev/martianzoo/tfm/script/commands/TfmMapCommand.kt` contains a reusable
   half-space centering writer.

These are minor and should not drive the architecture, but they are the only clearly generic logic
in the otherwise properly Terraforming-Mars-specific script commands.

## Module-by-module result

1. **`pets`** has the most fundamental boundary problem: megacredit semantics and fixed player
   identities are generic, while the reusable authority implementation remains under `tfm`.
2. **`engine`** is mostly genuinely generic. Its misplaced logic is concentrated in initialization,
   turn/action protocols, and automatic installation of `Prod`.
3. **`canon`** is correctly Terraforming-Mars-specific except for `system.pets`, which is really the
   generic runtime prelude.
4. **`script`** has a good generic command framework, but its central session, setup command,
   completion sources, phase handling, and server construction are the Terraforming Mars/REgo
   application.
5. **`repl`** contains reusable terminal integration, but its executable wiring and branding are
   application-specific.

## Suggested extraction order

1. Move `system.pets` into a generic runtime prelude and make its loading explicit.
2. Introduce generic setup and authority implementations, retaining Terraforming Mars subclasses.
3. Replace generic initializer special cases with game-supplied initialization roots and hooks.
4. Decide whether turn/action signaling is a generic protocol or a Terraforming Mars layer, then
   colocate both its code and declarations.
5. Replace hard-coded `Prod` calls with a game-configured transformer pipeline.
6. Separate the reusable script session/command shell from Terraforming Mars application wiring.
7. Separate reusable REPL/server adapters from REgo branding and launcher behavior.
8. Clean up the resulting dependency directions and module/artifact boundaries.
