package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.Exceptions.CustomCodeException
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Catalog
import dev.martianzoo.state.ClassSelection
import dev.martianzoo.state.CustomInstruction
import dev.martianzoo.state.CustomMetric
import dev.martianzoo.state.GameConfig
import dev.martianzoo.state.GamePremise
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/** Diagnostics for mistakes made after a custom catalog has compiled successfully. */
internal class PostCatalogDiagnosticsTest {
  private val source =
      """
      ABSTRACT CLASS Species { HAS MAX 1 This }
      CLASS Rose : Species
      CLASS Tulip : Species
      CLASS Water
      CLASS Seed : Owned
      CLASS Garden<Species> : Owned {
        HAS MAX 1 This
        score = 2
        yield = COUNT "Water"
        requirement = HAS "Water"
      }
      CLASS Tray<Species> {
        DEFAULT +Tray<Rose>
        DEFAULT -Tray<Tulip>
      }
      ABSTRACT CLASS Scored { score = Metric }
      CLASS Recursive { score = COUNT "EVAL This.score" }
      CLASS Marker
      CLASS OptionalModule
      CLASS AbsentActor : Actor { HAS MAX 1 This }
      """
          .trimIndent()
  private val premise = testGamePremise(source, players = 2)
  private val catalog = premise.catalog
  private val table = catalog.classTable

  @Test
  internal fun unknownExpression() {
    val expression = parse<Expression>("Gardne")
    val error =
        assertFailsWith<ExpressionException> {
          table.resolve(expression)
        }

    assertEquals("no class named `Gardne` in the current game", error.detail)
    assertEquals(
        """
        |no class named `Gardne` in the current game at 1:1
        |Gardne
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownArgument() {
    val expression = parse<Expression>("Garden<Missing>")
    val error =
        assertFailsWith<ExpressionException> {
          table.resolve(expression)
        }

    assertEquals("no class named `Missing` in the current game", error.detail)
    assertEquals(
        """
        |no class named `Missing` in the current game at 1:8
        |Garden<Missing>
        |       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun wrongDependencyArgument() {
    val expression = parse<Expression>("Garden<Water>")
    val error =
        assertFailsWith<ExpressionException> {
          table.resolve(expression)
        }

    assertEquals(
        "argument `Water` does not match an available dependency; declared bounds: `Owned_0=Owner`, `Garden_0=Species`; already supplied: none",
        error.detail,
    )
    assertEquals(
        """
        |argument `Water` does not match an available dependency; declared bounds: `Owned_0=Owner`, `Garden_0=Species`; already supplied: none at 1:8
        |Garden<Water>
        |       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun repeatedDependencyArgument() {
    val expression = parse<Expression>("Garden<Rose, Tulip>")
    val error =
        assertFailsWith<ExpressionException> {
          table.resolve(expression)
        }

    assertEquals(
        "argument `Tulip` does not match an available dependency; declared bounds: `Owned_0=Owner`, `Garden_0=Species`; already supplied: `Garden_0 <- Rose`",
        error.detail,
    )
    assertEquals(
        """
        |argument `Tulip` does not match an available dependency; declared bounds: `Owned_0=Owner`, `Garden_0=Species`; already supplied: `Garden_0 <- Rose` at 1:14
        |Garden<Rose, Tulip>
        |             ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun argumentsOnADependencyFreeClass() {
    val expression = parse<Expression>("Water<Rose>")
    val error =
        assertFailsWith<ExpressionException> {
          table.resolve(expression)
        }

    assertEquals(
        "argument `Rose` does not match an available dependency; declared bounds: none; already supplied: none",
        error.detail,
    )
    assertEquals(
        """
        |argument `Rose` does not match an available dependency; declared bounds: none; already supplied: none at 1:7
        |Water<Rose>
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun parameterizedClassLiteral() {
    // The generic expression grammar accepts this; T4-6 restricts class literals during resolution.
    val expression = parse<Expression>("Class<Garden<Rose>>")
    val error =
        assertFailsWith<ExpressionException> {
          table.resolve(expression)
        }

    assertEquals(
        "a class literal accepts one bare class name; found `Class<Garden<Rose>>`",
        error.detail,
    )
    // Prefer highlighting `<Rose>`; the represented class name `Garden` itself is valid.
    assertEquals(
        """
        |a class literal accepts one bare class name; found `Class<Garden<Rose>>` at 1:7
        |Class<Garden<Rose>>
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun twoClassLiteralArguments() {
    // The generic expression grammar accepts this; T4-6 restricts class literals during resolution.
    val expression = parse<Expression>("Class<Rose, Tulip>")
    val error =
        assertFailsWith<ExpressionException> {
          table.resolve(expression)
        }

    assertEquals(
        "a class literal accepts one bare class name; found `Class<Rose, Tulip>`",
        error.detail,
    )
    // Prefer highlighting `, Tulip`, including the comma that introduces the extra argument.
    assertEquals(
        """
        |a class literal accepts one bare class name; found `Class<Rose, Tulip>` at 1:13
        |Class<Rose, Tulip>
        |            ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun refinedNOTOperand() {
    // The grammar admits refinements here; T8-5 rejects them in a resolved NOT operand.
    val expression = parse<Expression>("Species(NOT Rose(HAS Water))")
    val error =
        assertFailsWith<ExpressionException> {
          table.resolve(expression)
        }

    assertEquals(
        "`NOT` operand cannot contain a refinement: `Species(NOT Rose(HAS Water))`",
        error.detail,
    )
    // Prefer highlighting `(HAS Water)` inside the `NOT` operand, not the valid name `Rose`.
    assertEquals(
        """
        |`NOT` operand cannot contain a refinement: `Species(NOT Rose(HAS Water))` at 1:13
        |Species(NOT Rose(HAS Water))
        |            ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unacceptedGainDefaults() {
    val elaborator = PetElaborator(table)
    val error =
        assertFailsWith<ExpressionException> {
          elaborator.elaborateInput(parse<InstructionTree>("Tray"))
        }

    assertEquals(
        "`Tray` has gain dependency defaults; write `Tray<>` to accept them or provide dependency arguments",
        error.detail,
    )
    assertEquals(
        """
        |`Tray` has gain dependency defaults; write `Tray<>` to accept them or provide dependency arguments at 1:1
        |Tray
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun emptyGainArgumentsWithoutDefaults() {
    val elaborator = PetElaborator(table)
    val error =
        assertFailsWith<ExpressionException> {
          elaborator.elaborateInput(parse<InstructionTree>("Water<>"))
        }

    assertEquals("`Water<>` has no gain dependency defaults to accept", error.detail)
    // Prefer pointing at `<>`; the class name `Water` itself is valid.
    assertEquals(
        """
        |`Water<>` has no gain dependency defaults to accept at 1:1
        |Water<>
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun emptyRemovalArgumentsWithoutDefaults() {
    val elaborator = PetElaborator(table)
    val error =
        assertFailsWith<ExpressionException> {
          elaborator.elaborateInput(parse<InstructionTree>("-Water<>"))
        }

    assertEquals("`Water<>` has no removal dependency defaults to accept", error.detail)
    // Prefer pointing at `<>`; removing `Water` without those brackets is valid syntax.
    assertEquals(
        """
        |`Water<>` has no removal dependency defaults to accept at 1:2
        |-Water<>
        | ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun propertyEvaluationInSubmittedInstruction() {
    val elaborator = PetElaborator(table)
    val error =
        assertFailsWith<PetSyntaxException> {
          elaborator.elaborateInput(parse<InstructionTree>("Water / EVAL Garden.yield"))
        }

    assertEquals(
        "`EVAL` is valid only in a class effect or a submitted metric; it cannot appear in a submitted instruction",
        error.detail,
    )
    assertEquals(
        """
        |`EVAL` is valid only in a class effect or a submitted metric; it cannot appear in a submitted instruction at 1:9
        |Water / EVAL Garden.yield
        |        ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun missingEvaluatedMetricProperty() {
    val elaborator = PetElaborator(table)
    val error =
        assertFailsWith<ExpressionException> {
          elaborator.elaborateMetricInput(
              parse<Metric>("EVAL Garden.missing"),
              cn("Garden").expression,
          )
        }

    assertEquals(
        "class `Garden` has no property `missing`; available properties: `score`, `yield`, `requirement`",
        error.detail,
    )
    assertEquals(
        """
        |class `Garden` has no property `missing`; available properties: `score`, `yield`, `requirement` at 1:13
        |EVAL Garden.missing
        |            ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun requirementUsedAsMetric() {
    val elaborator = PetElaborator(table)
    val error =
        assertFailsWith<ExpressionException> {
          elaborator.elaborateMetricInput(
              parse<Metric>("EVAL Garden.requirement"),
              cn("Garden").expression,
          )
        }

    assertEquals(
        "property `requirement` is not a concrete metric on `Garden`; found `HAS \"Water\"`",
        error.detail,
    )
    // Prefer also showing the declaration `requirement = HAS "Water"` that supplies the wrong
    // property kind.
    assertEquals(
        """
        |property `requirement` is not a concrete metric on `Garden`; found `HAS "Water"` at 1:13
        |EVAL Garden.requirement
        |            ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun abstractMetricProperty() {
    val elaborator = PetElaborator(table)
    val error =
        assertFailsWith<ExpressionException> {
          elaborator.elaborateMetricInput(
              parse<Metric>("EVAL Scored.score"),
              cn("Scored").expression,
          )
        }

    assertEquals(
        "property `score` is not a concrete metric on `Scored`; found `Metric`",
        error.detail,
    )
    // Prefer also showing the abstract declaration `score = Metric`.
    assertEquals(
        """
        |property `score` is not a concrete metric on `Scored`; found `Metric` at 1:13
        |EVAL Scored.score
        |            ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun recursiveMetricProperty() {
    val source = "CLASS Recursive { score = COUNT \"EVAL This.score\" }"
    val table = testClassTable(source)
    val elaborator = PetElaborator(table)
    val error =
        assertFailsWith<ExpressionException> {
          elaborator.elaborateMetricInput(
              parse<Metric>("EVAL Recursive.score"),
              cn("Recursive").expression,
          )
        }

    assertEquals(
        "property `score` is recursive on `Recursive`; expansion: `Recursive.score` -> `Recursive.score`",
        error.detail,
    )
    // Prefer highlighting the whole recursive reference `This.score` and showing the expansion
    // chain.
    assertEquals(
        """
        |property `score` is recursive on `Recursive`; expansion: `Recursive.score` -> `Recursive.score` at 1:44
        |CLASS Recursive { score = COUNT "EVAL This.score" }
        |                                           ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun nestedIdenticalTransform() {
    val error =
        assertFailsWith<ExpressionException> {
          TransformHandler.dispatcher(mapOf("SAME" to TransformHandler { it }))
              .transformInstructionTree(parse("SAME[SAME[Water]]"))
        }

    assertEquals("`SAME` transforms cannot be nested", error.detail)
    assertEquals(
        """
        |`SAME` transforms cannot be nested at 1:6
        |SAME[SAME[Water]]
        |     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownSelectedClass() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise.copy(classSelections = setOf(ClassSelection(cn("Missing"))))
        }

    assertEquals(
        "individual class selections are absent from the premise catalog: `Missing`",
        error.detail,
    )
    assertEquals(
        "individual class selections are absent from the premise catalog: `Missing`",
        error.message,
    )
  }

  @Test
  internal fun contradictorySelectedClass() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise.copy(
              classSelections =
                  setOf(ClassSelection(cn("Water")), ClassSelection(cn("Water"), false))
          )
        }

    assertEquals("duplicate individual class selections: `Water`, `Water`", error.detail)
    assertEquals("duplicate individual class selections: `Water`, `Water`", error.message)
  }

  @Test
  internal fun conditionalIndividualSelection() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise.copy(
              classSelections = setOf(ClassSelection(cn("Water"), requirement = parse("Rose")))
          )
        }

    assertEquals(
        "individual class selections cannot be conditional: `Water` (condition `Rose`)",
        error.detail,
    )
    // Prefer pointing at the submitted condition `Rose`, whose source is available here.
    assertEquals(
        "individual class selections cannot be conditional: `Water` (condition `Rose`)",
        error.message,
    )
  }

  @Test
  internal fun unknownPlayer() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise.copy(playerNames = listOf(cn("Missing")))
        }

    assertEquals("player names must be concrete `Player` classes: `Missing`", error.detail)
    assertEquals("player names must be concrete `Player` classes: `Missing`", error.message)
  }

  @Test
  internal fun ordinaryClassAsPlayer() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise.copy(playerNames = listOf(cn("Water")))
        }

    assertEquals("player names must be concrete `Player` classes: `Water`", error.detail)
    assertEquals("player names must be concrete `Player` classes: `Water`", error.message)
  }

  @Test
  internal fun duplicatePlayer() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise.copy(playerNames = listOf(cn("Player1"), cn("Player1")))
        }

    assertEquals("duplicate player names: `Player1`, `Player1`", error.detail)
    assertEquals("duplicate player names: `Player1`, `Player1`", error.message)
  }

  @Test
  internal fun abstractBootstrapClass() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise.copy(bootstrapClassName = cn("Species"))
        }

    assertEquals(
        "bootstrap class must be a concrete dependency-free catalog class: `Species`",
        error.detail,
    )
    assertEquals(
        "bootstrap class must be a concrete dependency-free catalog class: `Species`",
        error.message,
    )
  }

  @Test
  internal fun dependentPremiseClass() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise.copy(premiseClassName = cn("Garden"))
        }

    assertEquals(
        "premise class must be a concrete dependency-free catalog class: `Garden`",
        error.detail,
    )
    assertEquals(
        "premise class must be a concrete dependency-free catalog class: `Garden`",
        error.message,
    )
  }

  @Test
  internal fun premiseDeclarationCollidesWithCatalog() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise.copy(premiseClassDeclarations = parseClasses("CLASS Rose").toSet())
        }

    assertEquals("premise class names collide with the master table: `Rose`", error.detail)
    // A caret on the premise-local `Rose` declaration would identify the second declaration.
    assertEquals("premise class names collide with the master table: `Rose`", error.message)
  }

  @Test
  internal fun conflictingPremiseDeclarations() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise.copy(
              premiseClassDeclarations = parseClasses("CLASS Local\nABSTRACT CLASS Local").toSet()
          )
        }

    assertEquals("premise contains duplicate class declarations: `Local`", error.detail)
    // A caret on the second `Local` name would identify the conflicting declaration.
    assertEquals("premise contains duplicate class declarations: `Local`", error.message)
  }

  @Test
  internal fun excludedRequiredDependency() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise
              .copy(
                  classSelections =
                      setOf(ClassSelection(cn("Garden")), ClassSelection(cn("Species"), false))
              )
              .classTable
        }

    assertEquals("structural selection conflicts with excluded classes: `Species`", error.detail)
    assertEquals("structural selection conflicts with excluded classes: `Species`", error.message)
  }

  @Test
  internal fun unseatedPlayerRemainsIncluded() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          premise
              .copy(
                  playerNames = listOf(cn("Player1")),
                  classSelections = premise.classSelections + ClassSelection(cn("Player2")),
              )
              .classTable
        }

    assertEquals(
        "inhabited `Player` classes do not match occupied seats: `Player1`, `Player2`",
        error.detail,
    )
    assertEquals(
        "inhabited `Player` classes do not match occupied seats: `Player1`, `Player2`",
        error.message,
    )
  }

  @Test
  internal fun premiseLocalInvalidDefaults() {
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          premise
              .copy(
                  premiseClassDeclarations =
                      parseClasses("CLASS Local { DEFAULT Local<Water> }").toSet()
              )
              .classTable
        }

    assertEquals(
        "invalid defaults for `Local`: argument `Water` does not match an available dependency; declared bounds: none; already supplied: none",
        error.detail,
    )
    assertEquals(
        """
        |invalid defaults for `Local`: argument `Water` does not match an available dependency; declared bounds: none; already supplied: none at 1:29
        |CLASS Local { DEFAULT Local<Water> }
        |                            ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun premiseLocalInvalidDependencyBound() {
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          premise
              .copy(premiseClassDeclarations = parseClasses("CLASS Local<Garden<Water>>").toSet())
              .classTable
        }

    assertEquals(
        "invalid definition for `Local`: argument `Water` does not match an available dependency; declared bounds: `Owned_0=Owner`, `Garden_0=Species`; already supplied: none",
        error.detail,
    )
    assertEquals(
        """
        |invalid definition for `Local`: argument `Water` does not match an available dependency; declared bounds: `Owned_0=Owner`, `Garden_0=Species`; already supplied: none at 1:20
        |CLASS Local<Garden<Water>>
        |                   ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun premiseLocalIllegalSuperclass() {
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          premise
              .copy(premiseClassDeclarations = parseClasses("CLASS Local : Rose").toSet())
              .classTable
        }

    assertEquals(
        "`Local` cannot extend concrete classes: `Rose`; declare the superclass `ABSTRACT` if it is intended to be extended",
        error.detail,
    )
    assertEquals(
        """
        |`Local` cannot extend concrete classes: `Rose`; declare the superclass `ABSTRACT` if it is intended to be extended at 1:15
        |CLASS Local : Rose
        |              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun duplicateConfigurationEntry() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          GameConfig("Water, Water")
        }

    assertEquals("duplicate class selections: `Water`, `Water`", error.detail)
    // A caret on the second `Water` entry would identify the duplicate.
    assertEquals("duplicate class selections: `Water`, `Water`", error.message)
  }

  @Test
  internal fun configurationIncludedAndExcluded() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          GameConfig("Water, -Water")
        }

    assertEquals("duplicate class selections: `Water`, `Water`", error.detail)
    // Prefer highlighting the conflicting second entry `-Water`.
    assertEquals("duplicate class selections: `Water`, `Water`", error.message)
  }

  @Test
  internal fun signedSetupAdjustment() {
    assertEquals(
        mapOf(cn("Water") to -2),
        GameConfig("-2 Water").componentAdjustments,
    )
  }

  @Test
  internal fun zeroSetupAdjustment() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          GameConfig("0 Water")
        }

    assertEquals("invalid setup adjustment entry: `0 Water`", error.detail)
    assertEquals(
        """
        |invalid setup adjustment entry: `0 Water` at 1:1
        |0 Water
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun overflowingSetupAdjustment() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          GameConfig("2147483648 Water")
        }

    assertEquals("setup adjustment is too large: `2147483648 Water`", error.detail)
    // Prefer highlighting only the overflowing digits `2147483648`; the class name is valid.
    assertEquals(
        """
        |setup adjustment is too large: `2147483648 Water` at 1:1
        |2147483648 Water
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun invalidConfigurationClassName() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          GameConfig("water")
        }

    assertEquals("invalid configuration entry `water`: invalid class name: `water`", error.detail)
    assertEquals(
        """
        |invalid configuration entry `water`: invalid class name: `water` at 1:1
        |water
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun invalidPlayerName() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          GameConfig("Water", "player1")
        }

    assertEquals("invalid player names: `player1`", error.detail)
    // Prefer highlighting `player1` in the separately supplied player-name argument.
    assertEquals("invalid player names: `player1`", error.message)
  }

  @Test
  internal fun duplicateConfiguredPlayer() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          GameConfig("Water", "Player1", "Player1")
        }

    assertEquals("duplicate player names: `Player1`, `Player1`", error.detail)
    // Prefer identifying the second `Player1` player-name argument.
    assertEquals("duplicate player names: `Player1`, `Player1`", error.message)
  }

  @Test
  internal fun playerAlsoSelectedAsClass() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          GameConfig("Player1", "Player1")
        }

    assertEquals("player names cannot also be class selections: `Player1`", error.detail)
    // Prefer highlighting the `Player1` class-selection entry that duplicates a player seat.
    assertEquals("player names cannot also be class selections: `Player1`", error.message)
  }

  @Test
  internal fun countedAndOrdinaryEntry() {
    val error =
        assertFailsWith<InvalidGameConfigException> {
          GameConfig("2 Water, Water")
        }

    assertEquals("class names cannot have multiple configuration entries: `Water`", error.detail)
    // Prefer highlighting the second `Water` entry and showing the earlier `2 Water` entry.
    assertEquals("class names cannot have multiple configuration entries: `Water`", error.message)
  }

  @Test
  internal fun nonCountingInvariant() {
    val source = "CLASS Token { HAS Token OR Token(NOT Die) }"
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          Engine.newGame(premise)
        }

    assertEquals(
        "class invariant on `Token` is not a counting requirement: `Token OR Token(NOT Die)`",
        error.detail,
    )
    // Prefer highlighting the complete disjunction `Token OR Token(NOT Die)`, not just its first
    // operand.
    assertEquals(
        """
        |class invariant on `Token` is not a counting requirement: `Token OR Token(NOT Die)` at 1:19
        |CLASS Token { HAS Token OR Token(NOT Die) }
        |                  ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun metricValuedInvariant() {
    val source = "CLASS Token { score = 2; HAS MAX 1 This.score }"
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          Engine.newGame(premise)
        }

    assertEquals(
        "class invariant on `Token` must count one component expression: `MAX 1 This.score`",
        error.detail,
    )
    // Prefer pointing at `score`; the property access makes this a metric instead of a component
    // count.
    assertEquals(
        """
        |class invariant on `Token` must count one component expression: `MAX 1 This.score` at 1:36
        |CLASS Token { score = 2; HAS MAX 1 This.score }
        |                                   ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun dependencyLacksSingleTargetLimit() {
    val source =
        """
        CLASS Token
        CLASS Holder<Token>
        """
            .trimIndent()
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          Engine.newGame(premise)
        }

    assertEquals(
        "dependencies must target types with maximum multiplicity 1; first violation per class:\n  `Holder` -> `Token`",
        error.detail,
    )
    // Prefer pointing at the dependency bound `Token` and showing its missing multiplicity limit.
    assertEquals(
        """
        |dependencies must target types with maximum multiplicity 1; first violation per class:
        |  `Holder` -> `Token` at 2:7
        |CLASS Holder<Token>
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun missingRequiredInitialComponent() {
    val source = "CLASS Token { HAS =1 This }"
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidGameConfigException> {
          Engine.newGame(premise)
        }

    assertEquals(
        "game setup violates required component counts: `Token` (found 0, expected 1)",
        error.detail,
    )
    // Prefer showing the authored invariant `HAS =1 This` that requires a `Token` component.
    assertEquals(
        "game setup violates required component counts: `Token` (found 0, expected 1)",
        error.message,
    )
  }

  @Test
  internal fun bootstrapRequiresAChoice() {
    val source =
        """
        ABSTRACT CLASS Choice {
         CLASS Red
         CLASS Blue
        }
        CLASS Start { This: Choice }
        """
            .trimIndent()
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidGameConfigException> {
          Engine.newGame(premise.withTestSetup("Start"))
        }

    assertEquals(
        "game setup cannot complete: pending tasks:\n5* [Admin] Choice! VIA Start BECAUSE 4",
        error.detail,
    )
    // Prefer pointing at `Choice` in the authored bootstrap effect `This: Choice`.
    assertEquals(
        """
        |game setup cannot complete: pending tasks:
        |5* [Admin] Choice! VIA Start BECAUSE 4
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun bootstrapReachesDie() {
    val source = "CLASS Start { This: Die }"
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidGameConfigException> {
          Engine.newGame(premise.withTestSetup("Start"))
        }

    assertEquals("game setup cannot complete: a `Die` instruction was reached", error.detail)
    assertIs<DeadEndException>(error.cause)
    assertEquals(
        """
        |game setup cannot complete: a `Die` instruction was reached at 1:21
        |CLASS Start { This: Die }
        |                    ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun effectForgetsToAcceptDefaults() {
    val source =
        """
        ABSTRACT CLASS Species { HAS MAX 1 This }
        CLASS Rose : Species
        CLASS Tray<Species> { DEFAULT +Tray<Rose> }
        CLASS Start { This: Tray }
        """
            .trimIndent()
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          Engine.newGame(premise.withTestSetup("Start"))
        }

    assertEquals(
        "invalid effect declared by `Start`: `This: Tray`: `Tray` has gain dependency defaults; write `Tray<>` to accept them or provide dependency arguments",
        error.detail,
    )
    assertEquals(
        """
        |invalid effect declared by `Start`: `This: Tray`: `Tray` has gain dependency defaults; write `Tray<>` to accept them or provide dependency arguments at 4:21
        |CLASS Start { This: Tray }
        |                    ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun effectEvaluatesMissingProperty() {
    val source =
        """
        CLASS Water
        CLASS Start { This: Water / EVAL This.missing }
        """
            .trimIndent()
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          Engine.newGame(premise.withTestSetup("Start"))
        }

    assertEquals(
        "invalid effect declared by `Start`: `This: Water / EVAL This.missing`: class `Start` has no property `missing`; available properties: none",
        error.detail,
    )
    assertEquals(
        """
        |invalid effect declared by `Start`: `This: Water / EVAL This.missing`: class `Start` has no property `missing`; available properties: none at 2:39
        |CLASS Start { This: Water / EVAL This.missing }
        |                                      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun effectEvaluatesWrongPropertyKind() {
    val source =
        """
        CLASS Water
        CLASS Start { requirement = HAS "Water"; This: Water / EVAL This.requirement }
        """
            .trimIndent()
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          Engine.newGame(premise.withTestSetup("Start"))
        }

    assertEquals(
        "invalid effect declared by `Start`: `This: Water / EVAL This.requirement`: property `requirement` is not a concrete metric on `Start`; found `HAS \"Water\"`",
        error.detail,
    )
    // Prefer also showing `requirement = HAS "Water"`, the declaration supplying the wrong kind.
    assertEquals(
        """
        |invalid effect declared by `Start`: `This: Water / EVAL This.requirement`: property `requirement` is not a concrete metric on `Start`; found `HAS "Water"` at 2:66
        |CLASS Start { requirement = HAS "Water"; This: Water / EVAL This.requirement }
        |                                                                 ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun inheritedEffectEvaluatesMissingProperty() {
    val source =
        """
        CLASS Water
        ABSTRACT CLASS Base { This: Water / EVAL This.missing }
        CLASS Start : Base
        """
            .trimIndent()
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          Engine.newGame(premise.withTestSetup("Start"))
        }

    assertEquals(
        "invalid effect declared by `Base`: `This: Water / EVAL This.missing`: class `Start` has no property `missing`; available properties: none",
        error.detail,
    )
    assertIs<ExpressionException>(error.cause)
    assertEquals(
        """
        |invalid effect declared by `Base`: `This: Water / EVAL This.missing`: class `Start` has no property `missing`; available properties: none at 2:47
        |ABSTRACT CLASS Base { This: Water / EVAL This.missing }
        |                                              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun actionEvaluatesMissingProperty() {
    val source =
        """
        CLASS Water
        ABSTRACT CLASS HasActions { HAS MAX 1 This }
        ABSTRACT CLASS ActionSlot { HAS MAX 1 This }
        CLASS Action1 : ActionSlot
        CLASS UseAction<HasActions, ActionSlot>
        CLASS Start : HasActions { -> Water / EVAL This.missing }
        """
            .trimIndent()
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          Engine.newGame(premise.withTestSetup("Start"))
        }

    assertEquals(
        "invalid effect declared by `Start`: `UseAction<This, Action1>: Water / EVAL This.missing`: class `Start` has no property `missing`; available properties: none",
        error.detail,
    )
    assertEquals(
        """
        |invalid effect declared by `Start`: `UseAction<This, Action1>: Water / EVAL This.missing`: class `Start` has no property `missing`; available properties: none at 6:49
        |CLASS Start : HasActions { -> Water / EVAL This.missing }
        |                                                ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun invalidSpecializedTrigger() {
    val source =
        """
        ABSTRACT CLASS Choice
        ABSTRACT CLASS Allowed : Choice
        CLASS Good : Allowed { HAS MAX 1 This }
        CLASS Bad : Choice { HAS MAX 1 This }
        CLASS Box<Allowed>
        CLASS Holder<@Choice> { Box<@Choice>: Ok }
        """
            .trimIndent()
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<ExpressionException> {
          Engine.newGame(premise.withTestSetup("Bad, Holder<Bad>"))
        }

    assertEquals(
        "invalid effect for component `Holder<Bad>`: `Box<Bad>: Ok`: argument `Bad` does not match an available dependency; declared bounds: `Box_0=Allowed`; already supplied: none",
        error.detail,
    )
    assertIs<ExpressionException>(error.cause)
    // Prefer highlighting `@Choice` in the trigger and identifying its substituted value `Bad`.
    assertEquals(
        """
        |invalid effect for component `Holder<Bad>`: `Box<Bad>: Ok`: argument `Bad` does not match an available dependency; declared bounds: `Box_0=Allowed`; already supplied: none at 6:25
        |CLASS Holder<@Choice> { Box<@Choice>: Ok }
        |                        ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun recursivePropertyDeferredUntilEffectFiring() {
    val source =
        """
        ABSTRACT CLASS Species { HAS MAX 1 This }
        CLASS Rose : Species
        CLASS Water
        CLASS Recursive<Species> {
          requirement = HAS "EVAL This.requirement"
          This: EVAL This.requirement: Water
        }
        """
            .trimIndent()
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          Engine.newGame(premise.withTestSetup("Rose, Recursive<Rose>"))
        }

    assertEquals(
        "invalid effect for component `Recursive<Rose>`: `This: (EVAL Recursive<Rose>.requirement: Water!)`: property `requirement` is recursive on `Recursive<Rose>`; expansion: `Recursive<Rose>.requirement` -> `Recursive<Rose>.requirement`",
        error.detail,
    )
    assertIs<ExpressionException>(error.cause)
    // Prefer highlighting the whole recursive reference `This.requirement`.
    assertEquals(
        """
        |invalid effect for component `Recursive<Rose>`: `This: (EVAL Recursive<Rose>.requirement: Water!)`: property `requirement` is recursive on `Recursive<Rose>`; expansion: `Recursive<Rose>.requirement` -> `Recursive<Rose>.requirement` at 5:32
        |  requirement = HAS "EVAL This.requirement"
        |                               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun recursivePropertyDeferredUntilEACHBinding() {
    val source =
        """
        ABSTRACT CLASS Species { HAS MAX 1 This; score = Metric }
        CLASS Rose : Species { score = COUNT "EVAL This.score" }
        CLASS Tulip : Species { score = 1 }
        CLASS Water
        CLASS Start { This: EACH @Species { Water / EVAL @Species.score } }
        """
            .trimIndent()
    val premise = testGamePremise(source, players = 0)
    premise.catalog.classTable
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          Engine.newGame(premise.withTestSetup("Rose, Start"))
        }

    assertEquals(
        "invalid `EACH` body for `Rose`: property `score` is recursive on `Rose`; expansion: `Rose.score` -> `Rose.score`",
        error.detail,
    )
    assertIs<ExpressionException>(error.cause)
    // Prefer highlighting the whole recursive reference `This.score`.
    assertEquals(
        """
        |invalid `EACH` body for `Rose`: property `score` is recursive on `Rose`; expansion: `Rose.score` -> `Rose.score` at 2:49
        |CLASS Rose : Species { score = COUNT "EVAL This.score" }
        |                                                ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun moduleLacksPremiseClass() {
    val source =
        """
        CLASS Rules { HAS Water }
        CLASS Water
        CLASS Setup
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val modules = mapOf(cn("Rules") to emptySet<ClassSelection>())
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections = setOf(ClassSelection(cn("Rules"))),
            premiseClassName = cn("Setup"),
        )
    val error =
        assertFailsWith<InvalidGameConfigException> {
          Engine.newGame(premise.copy(premiseClassName = null))
        }

    assertEquals("a premise with modules must provide a premise class", error.detail)
    assertEquals("a premise with modules must provide a premise class", error.message)
  }

  @Test
  internal fun unsatisfiedModuleInvariant() {
    val source =
        """
        CLASS Rules { HAS Water }
        CLASS Water
        CLASS Setup
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val modules = mapOf(cn("Rules") to emptySet<ClassSelection>())
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections = setOf(ClassSelection(cn("Rules"))),
            premiseClassName = cn("Setup"),
        )
    val error =
        assertFailsWith<InvalidGameConfigException> {
          Engine.newGame(premise)
        }

    assertEquals("game premise fails module invariant: `Water`", error.detail)
    assertEquals(
        """
        |game premise fails module invariant: `Water` at 1:19
        |CLASS Rules { HAS Water }
        |                  ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun moduleConditionReadsProperty() {
    val source =
        """
        CLASS Rules { premiseRequirement = HAS "Water.score" }
        CLASS Water { score = 1 }
        CLASS Setup
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val modules = mapOf(cn("Rules") to emptySet<ClassSelection>())
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections = setOf(ClassSelection(cn("Rules"))),
            premiseClassName = cn("Setup"),
        )
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          Engine.newGame(premise)
        }

    assertEquals("module premise metrics cannot read properties: `Water.score`", error.detail)
    // Prefer highlighting `Water.score`, or pointing directly at `score`, rather than only `Water`.
    assertEquals(
        """
        |module premise metrics cannot read properties: `Water.score` at 1:41
        |CLASS Rules { premiseRequirement = HAS "Water.score" }
        |                                        ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unprovidedCustomInstructionArity() {
    val source =
        """
        CLASS Unimplemented : CustomInstruction
        CLASS Water
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val customClasses = setOf(object : CustomInstruction("Unimplemented") {})
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections =
                setOf(ClassSelection(cn("Unimplemented")), ClassSelection(cn("Water"))),
        )
    val agent = Engine.newGame(premise).testAgent(ADMIN)
    val error =
        assertFailsWith<ExpressionException> {
          agent.runOperation("Unimplemented")
        }

    assertEquals(
        "custom instruction `Unimplemented` has no implementation for 0 dependencies",
        error.detail,
    )
    assertEquals(error.detail, error.message)
  }

  @Test
  internal fun metricOnlyClassUsedAsInstruction() {
    val source =
        """
        CLASS Negative : CustomMetric
        CLASS Water
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val customClasses =
              setOf(
                  object : CustomMetric("Negative") {
                    override fun count(game: GameReader, type: Type): Int = -1
                  }
              )
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections = setOf(ClassSelection(cn("Negative")), ClassSelection(cn("Water"))),
        )
    val agent = Engine.newGame(premise).testAgent(ADMIN)
    val error =
        assertFailsWith<ExpressionException> {
          agent.runOperation("Negative")
        }

    assertEquals(
        "custom metric `Negative` cannot be gained",
        error.detail,
    )
    assertEquals(
        """
        |custom metric `Negative` cannot be gained at 1:1
        |Negative
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unfinishedCustomInstruction() {
    val source =
        """
        CLASS Unfinished : CustomInstruction
        CLASS Water
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val customClasses =
              setOf(
                  object : CustomInstruction("Unfinished") {
                    override fun translate(game: GameReader): InstructionTree =
                        TODO("finish translation")
                  }
              )
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections = setOf(ClassSelection(cn("Unfinished")), ClassSelection(cn("Water"))),
        )
    val agent = Engine.newGame(premise).testAgent(ADMIN)
    val error =
        assertFailsWith<CustomCodeException> {
          agent.runOperation("Unfinished")
        }

    assertEquals(
        "custom instruction failed for `Unfinished`: An operation is not implemented: finish translation",
        error.detail,
    )
    assertIs<NotImplementedError>(error.cause)
    // Prefer showing the `Unfinished` invocation as context for the Kotlin failure in the cause.
    assertEquals(
        "custom instruction failed for `Unfinished`: An operation is not implemented: finish translation",
        error.message,
    )
  }

  @Test
  internal fun unfinishedCustomMetric() {
    val source =
        """
        CLASS UnfinishedMetric : CustomMetric
        CLASS Water
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val customClasses =
              setOf(
                  object : CustomMetric("UnfinishedMetric") {
                    override fun count(game: GameReader, type: Type): Int = TODO("finish count")
                  }
              )
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections =
                setOf(ClassSelection(cn("UnfinishedMetric")), ClassSelection(cn("Water"))),
        )
    val agent = Engine.newGame(premise).testAgent(ADMIN)
    val error =
        assertFailsWith<CustomCodeException> {
          agent.count("UnfinishedMetric")
        }

    assertEquals(
        "custom metric failed for `UnfinishedMetric`: An operation is not implemented: finish count",
        error.detail,
    )
    assertIs<NotImplementedError>(error.cause)
    // Prefer showing the `UnfinishedMetric` query as context for the Kotlin failure in the cause.
    assertEquals(
        "custom metric failed for `UnfinishedMetric`: An operation is not implemented: finish count",
        error.message,
    )
  }

  @Test
  internal fun customInstructionImplementationCrashes() {
    val source =
        """
        CLASS Broken : CustomInstruction
        CLASS Water
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val customClasses =
              setOf(
                  object : CustomInstruction("Broken") {
                    override fun translate(game: GameReader): InstructionTree =
                        error("translator forgot its rule")
                  }
              )
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections = setOf(ClassSelection(cn("Broken")), ClassSelection(cn("Water"))),
        )
    val agent = Engine.newGame(premise).testAgent(ADMIN)
    val error =
        assertFailsWith<CustomCodeException> {
          agent.runOperation("Broken")
        }

    assertEquals("custom instruction failed for `Broken`: translator forgot its rule", error.detail)
    // Prefer showing the `Broken` invocation as context for the Kotlin failure in the cause.
    assertEquals(
        "custom instruction failed for `Broken`: translator forgot its rule",
        error.message,
    )
  }

  @Test
  internal fun customInstructionIsCountedAsAnOrdinaryComponent() {
    val source =
        """
        CLASS InstructionOnly : CustomInstruction
        CLASS Water
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val customClasses =
              setOf(
                  object : CustomInstruction("InstructionOnly") {
                    override fun translate(game: GameReader): InstructionTree = parse("Water")
                  }
              )
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections =
                setOf(ClassSelection(cn("InstructionOnly")), ClassSelection(cn("Water"))),
        )
    val agent = Engine.newGame(premise).testAgent(ADMIN)
    assertEquals(0, agent.count("InstructionOnly"))
    agent.runOperation("InstructionOnly")
    assertEquals(0, agent.count("InstructionOnly"))
    assertEquals(1, agent.count("Water"))
  }

  @Test
  internal fun customInstructionReturnsInvalidPets() {
    val source =
        """
        CLASS InvalidOutput : CustomInstruction
        CLASS Water
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val customClasses =
              setOf(
                  object : CustomInstruction("InvalidOutput") {
                    override fun translate(game: GameReader): InstructionTree = parse("Water<>")
                  }
              )
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections =
                setOf(ClassSelection(cn("InvalidOutput")), ClassSelection(cn("Water"))),
        )
    val agent = Engine.newGame(premise).testAgent(ADMIN)
    val error =
        assertFailsWith<CustomCodeException> {
          agent.runOperation("InvalidOutput")
        }

    assertEquals(
        "custom instruction for `InvalidOutput` returned invalid Pets `Water<>`: `Water<>` has no gain dependency defaults to accept",
        error.detail,
    )
    assertIs<ExpressionException>(error.cause)
    // Prefer pointing at `<>` in the generated Pets; `Water` itself is valid.
    assertEquals(
        """
        |custom instruction for `InvalidOutput` returned invalid Pets `Water<>`: `Water<>` has no gain dependency defaults to accept at 1:1
        |Water<>
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun customMetricInAComponentUnion() {
    val source =
        """
        CLASS Negative : CustomMetric
        CLASS Water
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val customClasses =
              setOf(
                  object : CustomMetric("Negative") {
                    override fun count(game: GameReader, type: Type): Int = -1
                  }
              )
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections = setOf(ClassSelection(cn("Negative")), ClassSelection(cn("Water"))),
        )
    val agent = Engine.newGame(premise).testAgent(ADMIN)
    val error =
        assertFailsWith<ExpressionException> {
          agent.count("Negative OR Water")
        }

    assertEquals(
        "custom metrics cannot be alternatives in an `OR` metric: `Negative`",
        error.detail,
    )
    assertEquals(
        """
        |custom metrics cannot be alternatives in an `OR` metric: `Negative` at 1:1
        |Negative OR Water
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun negativeCustomMetric() {
    val source =
        """
        CLASS Negative : CustomMetric
        CLASS Water
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val customClasses =
              setOf(
                  object : CustomMetric("Negative") {
                    override fun count(game: GameReader, type: Type): Int = -1
                  }
              )
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections = setOf(ClassSelection(cn("Negative")), ClassSelection(cn("Water"))),
        )
    val agent = Engine.newGame(premise).testAgent(ADMIN)
    val error =
        assertFailsWith<CustomCodeException> {
          agent.count("Negative")
        }

    assertEquals("custom metric `Negative` returned -1", error.detail)
    // Prefer pointing at the `Negative` query that invoked the faulty implementation.
    assertEquals("custom metric `Negative` returned -1", error.message)
  }

  @Test
  internal fun removeCustomMetric() {
    val source =
        """
        CLASS Unimplemented : CustomMetric
        CLASS Water
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val customClasses =
              setOf(
                  object : CustomMetric("Unimplemented") {
                    override fun count(game: GameReader, type: Type): Int = 0
                  }
              )
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections =
                setOf(ClassSelection(cn("Unimplemented")), ClassSelection(cn("Water"))),
        )
    val agent = Engine.newGame(premise).testAgent(ADMIN)
    val error =
        assertFailsWith<ExpressionException> {
          agent.runOperation("-Unimplemented")
        }

    assertEquals(
        "custom metric `Unimplemented` cannot be removed",
        error.detail,
    )
    // Prefer pointing at the removal operator `-`, which is unsupported for custom behavior.
    assertEquals(
        """
        |custom metric `Unimplemented` cannot be removed at 1:2
        |-Unimplemented
        | ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownQueriedClass() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.count("Missing")
        }

    assertEquals("no class named `Missing` in the current game", error.detail)
    assertEquals(
        """
        |no class named `Missing` in the current game at 1:1
        |Missing
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun missingQueriedProperty() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.count("Garden.missing")
        }

    assertEquals(
        "class `Garden` has no property `missing`; available properties: `score`, `yield`, `requirement`",
        error.detail,
    )
    assertEquals(
        """
        |class `Garden` has no property `missing`; available properties: `score`, `yield`, `requirement` at 1:8
        |Garden.missing
        |       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun abstractQueriedProperty() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.count("Scored.score")
        }

    assertEquals(
        "property `score` is abstract on `Scored`; query a concrete class that supplies its value",
        error.detail,
    )
    // Prefer also showing the declaration `score = Metric` that leaves this property abstract.
    assertEquals(
        """
        |property `score` is abstract on `Scored`; query a concrete class that supplies its value at 1:8
        |Scored.score
        |       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun metricPropertyWithoutEVAL() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.count("Garden.yield")
        }

    assertEquals(
        "metric property `yield` contains syntax; use `EVAL Garden<Player1>.yield` to evaluate it",
        error.detail,
    )
    // Prefer highlighting `Garden.yield` as a whole, where an enclosing `EVAL` is needed.
    assertEquals(
        """
        |metric property `yield` contains syntax; use `EVAL Garden<Player1>.yield` to evaluate it at 1:8
        |Garden.yield
        |       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun receiverlessQueriedProperty() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.count("score")
        }

    assertEquals("property `score` has no receiver; qualify it with a class name", error.detail)
    assertEquals(
        """
        |property `score` has no receiver; qualify it with a class name at 1:1
        |score
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun evaluatedQueryWithMissingProperty() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.count("EVAL Garden.missing")
        }

    assertEquals(
        "class `Garden` has no property `missing`; available properties: `score`, `yield`, `requirement`",
        error.detail,
    )
    assertEquals(
        """
        |class `Garden` has no property `missing`; available properties: `score`, `yield`, `requirement` at 1:13
        |EVAL Garden.missing
        |            ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun sameQueryAtADifferentLocation() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    assertFailsWith<ExpressionException> { agent.count("Garden.missing") }
    val error =
        assertFailsWith<ExpressionException> {
          agent.count("  Garden.missing")
        }

    assertEquals(
        "class `Garden` has no property `missing`; available properties: `score`, `yield`, `requirement`",
        error.detail,
    )
    assertEquals(
        """
        |class `Garden` has no property `missing`; available properties: `score`, `yield`, `requirement` at 1:10
        |  Garden.missing
        |         ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun rankWithoutASelector() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<PetSyntaxException> {
          agent.count("RANK { Water }")
        }

    assertEquals("`RANK { ... }` requires an enclosing expression refinement", error.detail)
    assertEquals(
        """
        |`RANK { ... }` requires an enclosing expression refinement at 1:1
        |RANK { Water }
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun rankWithoutACandidate() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.count("RANK Player { Water }")
        }

    assertEquals("`RANK` can only be evaluated while testing a concrete `Player`", error.detail)
    assertEquals(
        """
        |`RANK` can only be evaluated while testing a concrete `Player` at 1:1
        |RANK Player { Water }
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownInstructionTransform() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.runOperation("TYPO[Water]")
        }

    assertEquals("unhandled instruction transform: `TYPO[Water!]`", error.detail)
    assertEquals(
        """
        |unhandled instruction transform: `TYPO[Water!]` at 1:1
        |TYPO[Water]
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownMetricTransform() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.count("TYPO[Water]")
        }

    assertEquals("unhandled metric transform: `TYPO[Water]`", error.detail)
    assertEquals(
        """
        |unhandled metric transform: `TYPO[Water]` at 1:1
        |TYPO[Water]
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownRequirementTransform() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.has("TYPO[Water]")
        }

    assertEquals("unhandled requirement transform: `TYPO[Water]`", error.detail)
    assertEquals(
        """
        |unhandled requirement transform: `TYPO[Water]` at 1:1
        |TYPO[Water]
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun nonActorBYTarget() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.runOperation("Water BY Rose")
        }

    assertEquals("`BY` requires an `Actor`; `Rose` is not an `Actor`", error.detail)
    assertEquals(
        """
        |`BY` requires an `Actor`; `Rose` is not an `Actor` at 1:10
        |Water BY Rose
        |         ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun abstractBYTarget() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.runOperation("Water BY Actor")
        }

    assertEquals("`BY` requires one concrete `Actor`; `Actor` is abstract", error.detail)
    assertEquals(
        """
        |`BY` requires one concrete `Actor`; `Actor` is abstract at 1:10
        |Water BY Actor
        |         ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun absentBYTarget() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.runOperation("Water BY AbsentActor")
        }

    assertEquals(
        "`BY` requires a participating `Actor`; `AbsentActor` has no component in this world",
        error.detail,
    )
    // Prefer a caret on the submitted `AbsentActor` target after `BY`.
    assertEquals(
        "`BY` requires a participating `Actor`; `AbsentActor` has no component in this world",
        error.message,
    )
  }

  @Test
  internal fun gainWithMissingDependency() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<DependencyException> {
          agent.runOperation("Garden<Rose>")
        }

    assertEquals("missing dependencies: `Rose`", error.detail)
    // Prefer a caret on `Rose` in the submitted `Garden<Rose>` argument list.
    assertEquals("missing dependencies: `Rose`", error.message)
  }

  @Test
  internal fun removeAbsentComponent() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<LimitsException> {
          agent.runOperation("-Water")
        }

    assertEquals("cannot remove 1 `Water`: maximum available is 0", error.detail)
    // Prefer highlighting the whole removal `-Water`, including its operator.
    assertEquals(
        """
        |cannot remove 1 `Water`: maximum available is 0 at 1:2
        |-Water
        | ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unmetInstructionRequirement() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<RequirementException> {
          agent.runOperation("Water: Seed")
        }

    assertEquals("requirement `Water` is not met for `Seed<Player1>!`", error.detail)
    assertEquals(
        """
        |requirement `Water` is not met for `Seed<Player1>!` at 1:1
        |Water: Seed
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun impossibleInstruction() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<DeadEndException> {
          agent.runOperation("Die")
        }

    assertEquals("a `Die` instruction was reached", error.detail)
    assertEquals(
        """
        |a `Die` instruction was reached at 1:1
        |Die
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun noViableChoice() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<NotNowException> {
          agent.runOperation("-Water OR -Seed")
        }

    assertEquals(
        "no alternative is possible: cannot remove 1 `Water`: maximum available is 0, cannot remove 1 `Seed<Player1>`: maximum available is 0",
        error.detail,
    )
    // Prefer highlighting the entire `-Water OR -Seed` choice; both alternatives fail.
    assertEquals(
        """
        |no alternative is possible: cannot remove 1 `Water`: maximum available is 0, cannot remove 1 `Seed<Player1>`: maximum available is 0 at 1:2
        |-Water OR -Seed
        | ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun gainPastUpperBound() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    agent.runOperation("Rose")
    val error =
        assertFailsWith<LimitsException> {
          agent.runOperation("Rose")
        }

    assertEquals(
        "component count invariant violated: `Rose` (found 2, expected 0..1)",
        error.detail,
    )
    assertEquals(
        """
        |component count invariant violated: `Rose` (found 2, expected 0..1) at 1:1
        |Rose
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun sameTypeTransmutationIsAnInvalidExpression() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.runOperation("Rose FROM Rose")
        }

    assertEquals("a transmutation must change its type: Rose FROM Rose", error.detail)
    assertEquals(
        """
        |a transmutation must change its type: Rose FROM Rose at 1:1
        |Rose FROM Rose
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun abstractDirectChange() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<NotFullySpecifiedException> {
          agent.sneak("X Water")
        }

    assertEquals("instruction is abstract: `X Water!`", error.detail)
    assertEquals(
        """
        |instruction is abstract: `X Water!` at 1:1
        |X Water
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun indirectDirectChange() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<ExpressionException> {
          agent.sneak("Water: Seed")
        }

    assertEquals("sneak accepts only direct changes; found `Water: Seed<Player1>!`", error.detail)
    // Prefer pointing at `:`, which makes this a gated instruction rather than a direct change.
    assertEquals(
        """
        |sneak accepts only direct changes; found `Water: Seed<Player1>!` at 1:1
        |Water: Seed
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun narrowWithoutASelectedTask() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<TaskException> {
          agent.narrowTask("Water")
        }

    assertEquals("`Player1` has no selected task", error.detail)
    assertEquals("`Player1` has no selected task", error.message)
  }

  @Test
  internal fun selectWithoutAMatchingTask() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val error =
        assertFailsWith<TaskException> {
          agent.selectTask("Water")
        }

    assertEquals("no matching task; available tasks: none", error.detail)
    assertEquals("no matching task; available tasks: none", error.message)
  }

  @Test
  internal fun incompatibleTaskNarrowing() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    val task = agent.addTasks("Water OR Seed").single()
    agent.selectTask(task)
    val error =
        assertFailsWith<NarrowingException> {
          agent.narrowTask("Rose")
        }

    assertEquals("`Rose!` does not narrow `Water! OR Seed<Player1>!`", error.detail)
    // Prefer a caret on the submitted narrowing `Rose`, alongside the original task choices.
    assertEquals("`Rose!` does not narrow `Water! OR Seed<Player1>!`", error.message)
  }

  @Test
  internal fun completeWithPendingAbstractTask() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    agent.beginOperation("X Water")
    val error =
        assertFailsWith<NotFullySpecifiedException> {
          agent.completeOperation()
        }

    assertEquals("pending abstract tasks:\n9* [Player1] X Water!", error.detail)
    // Prefer a caret on `X` in the pending authored instruction `X Water`.
    assertEquals(
        """
        |pending abstract tasks:
        |9* [Player1] X Water!
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun otherActorSelectsAssignedTask() {
    val world = Engine.newGame(premise)
    val first = world.testAgent(Player(cn("Player1")))
    val second = world.testAgent(Player(cn("Player2")))
    val task = first.addTasks("X Water").single()
    val error =
        assertFailsWith<TaskException> {
          second.selectTask(task)
        }

    assertEquals(
        "`Player2` cannot queue a task assigned to `Player1`: 9  [Player1] X Water!",
        error.detail,
    )
    assertEquals(
        "`Player2` cannot queue a task assigned to `Player1`: 9  [Player1] X Water!",
        error.message,
    )
  }

  @Test
  internal fun mandatoryExcludedClass() {
    val restricted = premise.copy(classSelections = setOf(ClassSelection(cn("Water"))))
    val error =
        assertFailsWith<DeadEndException> {
          Engine.newGame(restricted).testAgent(Player(cn("Player1"))).runOperation("Rose!")
        }

    assertEquals("mandatory change uses uninhabited type: `Rose`", error.detail)
    // Prefer highlighting `Rose!`, including the mandatory quantifier that prevents skipping it.
    assertEquals(
        """
        |mandatory change uses uninhabited type: `Rose` at 1:1
        |Rose!
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun correctedGameCanGainComponentsAndQueryProperties() {
    val agent = Engine.newGame(premise).testAgent(Player(cn("Player1")))
    agent.runOperation("Rose, Garden<Rose>, 2 Water")
    assertEquals(1, agent.count("Garden<Rose>"))
    assertEquals(2, agent.count("Water"))
    assertEquals(2, agent.count("EVAL Garden.yield"))
    assertEquals(2, agent.count("Garden.score"))
  }

  @Test
  internal fun optionalExcludedClassCanBeSkipped() {
    val restricted = premise.copy(classSelections = setOf(ClassSelection(cn("Water"))))
    val agent = Engine.newGame(restricted).testAgent(Player(cn("Player1")))
    agent.runOperation("Rose.")
    assertEquals(0, agent.count("Rose"))
  }

  @Test
  internal fun correctedPremiseLocalDefaultsCompile() {
    val extended =
        premise
            .copy(
                premiseClassDeclarations = parseClasses("CLASS Local { DEFAULT +Local! }").toSet()
            )
            .classTable
    assertEquals(cn("Local"), extended.getClass(cn("Local")).className)
  }

  @Test
  internal fun implementedCustomInstructionRunsSuccessfully() {
    val source =
        """
        CLASS InstructionOnly : CustomInstruction { This:: Blue; This: Green }
        CLASS Water
        CLASS Green
        CLASS Blue
        """
            .trimIndent()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations = parseClasses(source).toSet()
          override val customClasses =
              setOf(
                  object : CustomInstruction("InstructionOnly") {
                    override fun translate(game: GameReader): InstructionTree = parse("Water")
                  }
              )
        }
    catalog.classTable
    val premise =
        GamePremise(
            catalog,
            classSelections =
                setOf(
                    ClassSelection(cn("InstructionOnly")),
                    ClassSelection(cn("Water")),
                    ClassSelection(cn("Green")),
                    ClassSelection(cn("Blue")),
                ),
        )
    val game = Engine.newGame(premise)
    val agent = game.testAgent(ADMIN)
    agent.runOperation("InstructionOnly")
    assertEquals(1, agent.count("Water"))
    assertEquals(1, agent.count("Green"))
    assertEquals(1, agent.count("Blue"))
    assertEquals(
        1,
        game.events.changesSinceSetup().count {
          it.change.gaining?.className == cn("InstructionOnly")
        },
    )
  }

  @Test
  internal fun sourceSpansStayWithOccurrencesAfterScalingAndRewriting() {
    val authored = parse<Instruction>("TYPO[Water]")
    val task = Task(TaskId(0), ADMIN, instruction = authored, cause = null)
    val scaledError =
        assertFailsWith<ExpressionException> {
          normalizeTask(task.copy(instruction = authored * 2))
        }
    assertEquals(
        """
        |unhandled instruction transform: `TYPO[2 Water]` at 1:1
        |TYPO[Water]
        |^
        """
            .trimMargin(),
        scaledError.message,
    )
    val unchangedError =
        assertFailsWith<ExpressionException> {
          normalizeTask(task.copy(instruction = authored * 1))
        }
    assertEquals(
        """
        |unhandled instruction transform: `TYPO[Water]` at 1:1
        |TYPO[Water]
        |^
        """
            .trimMargin(),
        unchangedError.message,
    )

    // Shared Ok has no unique authored occurrence to render in a diagnostic.
    val zeroError =
        ExpressionException(
            "no authored occurrence",
            sourceLocation = (authored * 0).sourceLocation,
        )
    assertEquals("no authored occurrence", zeroError.message)

    val discardGains =
        object : PetTransformer() {
          override fun transformNode(node: PetNode): PetNode =
              if (node is Instruction.Gain) Instruction.NoOp else transformChildren(node)
        }
    assertEquals(Instruction.NoOp, discardGains.transformInstructionTree(parse("Water OR Rose")))
    // Collapsing this located OR must not attach its source to every later use of shared Ok.
    val rewrittenError =
        ExpressionException(
            "no authored occurrence",
            sourceLocation = parse<InstructionTree>("Ok").sourceLocation,
        )
    assertEquals("no authored occurrence", rewrittenError.message)
  }
}
