package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.types.loadTypes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PetElaboratorTest {
  private val table =
      loadTypes(
          """
          ABSTRACT CLASS Player : Owner, Actor
          CLASS Player1 : Player
          CLASS Pulse : Atomized
          CLASS Token<Owner> : Owned
          ABSTRACT CLASS Scored {
            score = Metric
          }
          CLASS Score : Scored {
            score = COUNT "Pulse"
          }
          ABSTRACT CLASS Rule {
            This: UNWRAP[2 Pulse, Token]
          }
          CLASS ConcreteRule : Rule
          CLASS ContextRule : Owned {
            This: This, Token
          }
          CLASS NarrowedHolder : Owned<Me@Player> {
            This: Token
          }
          CLASS ExplicitTriggerRule { Pulse BY Me@Player: Token, Token<Me@Player> }
          CLASS OrRule { Pulse OR Token: Token }
          ABSTRACT CLASS Area
          ABSTRACT CLASS LandArea : Area
          CLASS ContextualTile<Area> : Owned {
            DEFAULT +ContextualTile<LandArea>
          }
          ABSTRACT CLASS AreaRule : Area {
            This: ContextualTile<This>
          }
          ABSTRACT CLASS HolderRule : Owner {
            This: ContextualTile<This>
          }
          ABSTRACT CLASS Choice {
            CLASS RedChoice
            CLASS BlueChoice
          }
          CLASS SequentialRule {
            This: Selected@Choice THEN -Choice(NOT Selected@Choice)
          }
          CLASS TransmutingRule {
            This: Choice(NOT Source@Choice) FROM Source@Choice
          }
          """
              .trimIndent(),
          transformHandlerFactories =
              mapOf(
                  "UNWRAP" to { _ -> TransformHandler { transformed -> transformed } },
                  "ORDER" to
                      { _ ->
                        TransformHandler { transformed ->
                          if (transformed is Metric.Eval) parse<Metric>("Pulse")
                          else parse<Metric>("Token")
                        }
                      },
              ),
      )
  private val elaborator = PetElaborator(table)
  private val player1 = parse<Expression>("Player1")

  @Test
  internal fun explicitTriggerMeOwnsBareResultsWithoutAnotherTriggerBinding() {
    elaborator.classEffects(table.getClass(parse("ExplicitTriggerRule"))) shouldBe
        listOf(parse<Effect>("Pulse BY Me@Player: Token<Me@Player>!, Token<Me@Player>!"))
  }

  @Test
  internal fun mixedOrTriggerCannotImplicitlyBindOneMeForEveryArm() {
    shouldThrow<InvalidPetDefinitionException> {
      elaborator.classEffects(table.getClass(parse("OrRule")))
    }
  }

  @Test
  internal fun inputElaborationAppliesTheCompleteAuthoredSyntaxPackage() {
    val source = parse<InstructionTree>("2 Pulse, UNWRAP[Token]")

    elaborator.elaborateInput(source, player1) shouldBe
        parse<InstructionTree>("Pulse!, Pulse!, Token<Player1>!")
  }

  @Test
  internal fun resolvingACachedPredicateRetainsTheCurrentVariablesScope() {
    val source = "Token<Player(HAS @Choice)>! THEN @Choice!"
    elaborator.elaborateInput(parse<InstructionTree>(source), player1)
    val second = elaborator.elaborateInput(parse<InstructionTree>(source), player1) as Then
    val variable = second.typeVariables.variables.single()

    val bound =
        second.typeVariables
            .bind(
                mapOf(variable to table.resolve(parse<Expression>("BlueChoice"))),
                table,
            )
            .transformInstruction(second)

    bound shouldBe
        elaborator.elaborateInput(
            parse<InstructionTree>("Token<Player(HAS BlueChoice)>! THEN BlueChoice!"),
            player1,
        )
  }

  @Test
  internal fun ordinaryInputRejectsPropertyEvaluationWhileMetricInputExpandsIt() {
    val source = parse<Metric>("EVAL Score.score")

    shouldThrow<PetSyntaxException> { elaborator.elaborateInput(source, player1) }
    elaborator.elaborateMetricInput(source, player1.expression, player1) shouldBe
        parse<Metric>("Pulse")
  }

  @Test
  internal fun metricInputRejectsTransformsAroundPropertyEvaluations() {
    shouldThrow<ExpressionException> {
          elaborator.elaborateMetricInput(
              parse("ORDER[EVAL Score.score]"),
              player1.expression,
              player1,
          )
        }
        .detail shouldBe "transform blocks cannot contain EVAL"
  }

  @Test
  internal fun classEffectsApplyTheCompleteDeclarationPackage() {
    val concreteRule = table.getClass(parse("ConcreteRule"))

    elaborator.classEffects(concreteRule) shouldBe
        listOf(parse("This BY Me@Player: Pulse!, Pulse!, Token<Me@Player>!"))
  }

  @Test
  internal fun classEffectsApplyDefaultsAgainstTheirClassContext() {
    elaborator.classEffects(table.getClass(parse("AreaRule"))).single() shouldBe
        parse<Effect>("This BY Me@Player: ContextualTile<Me@Player, This>!")
    elaborator.classEffects(table.getClass(parse("HolderRule"))).single() shouldBe
        parse<Effect>("This: ContextualTile<This, LandArea>!")
    elaborator.classEffects(table.getClass(parse("NarrowedHolder"))).single().toString() shouldBe
        "This: Token<Me@Player>!"
  }

  @Test
  internal fun customInstructionOutputUsesTheSameExecutableElaborationRules() {
    val source = parse<InstructionTree>("2 Pulse, UNWRAP[Token]")

    elaborator.elaborateCustomInstruction(source, player1) shouldBe
        parse<InstructionTree>("Pulse!, Pulse!, Token<Player1>!")
  }

  @Test
  internal fun effectSpecializationClosesTheWholeComponentContext() {
    val contextRule = table.getClass(parse("ContextRule"))
    val componentType = table.resolve(parse<Expression>("ContextRule<Player1>"))
    val classEffect = elaborator.classEffects(contextRule).single()

    elaborator.specializeEffect(
        contextRule.defaultType,
        componentType,
        classEffect,
        componentType.expressionFull,
    ) shouldBe parse<Effect>("This: ContextRule<Player1>, Token<Player1>!")
  }

  @Test
  internal fun effectSpecializationPreservesVariablesOwnedByANestedSequence() {
    val rule = table.getClass(parse("SequentialRule"))
    val effect = elaborator.classEffects(rule).single()

    elaborator.specializeEffect(
        rule.defaultType,
        rule.defaultType,
        effect,
        rule.defaultType.expressionFull,
    ) shouldBe parse<Effect>("This: Selected@Choice! THEN -Choice(NOT Selected@Choice)!")
  }

  @Test
  internal fun effectSpecializationPreservesVariablesOwnedByATransmutation() {
    val rule = table.getClass(parse("TransmutingRule"))
    val effect = elaborator.classEffects(rule).single()
    ((effect.instruction as Transmute).typeVariables.isEmpty) shouldBe false

    elaborator.specializeEffect(
        rule.defaultType,
        rule.defaultType,
        effect,
        rule.defaultType.expressionFull,
    ) shouldBe parse<Effect>("This: Choice(NOT Source@Choice) FROM Source@Choice!")
  }
}
