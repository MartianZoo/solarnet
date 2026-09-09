package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.ClassLoader
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.testCatalog
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PetElaboratorTest {
  private val catalog: Catalog = run {
    val baseCatalog =
        testCatalog(
            petsText =
                """
                ABSTRACT CLASS Player : Owner, Actor
                CLASS Player1 : Player
                CLASS Pulse : Atomized
                CLASS Token<Anyone> : Owned<Anyone>
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
                CLASS ContextRule : Owned<Anyone> {
                  This: This, Token
                }
                ABSTRACT CLASS Area
                ABSTRACT CLASS LandArea : Area
                CLASS ContextualTile<Area> : Owned<Anyone> {
                  DEFAULT +ContextualTile<LandArea>
                }
                ABSTRACT CLASS AreaRule : Area {
                  This: ContextualTile<This>
                }
                ABSTRACT CLASS OwnerRule : Owner {
                  This: ContextualTile<This>
                }
                """
                    .trimIndent(),
        )
    object : Catalog by baseCatalog {
      override val transformHandlerFactories: Map<String, (ClassTable) -> TransformHandler> =
          mapOf("UNWRAP" to { _ -> TransformHandler { transformed -> transformed } })

      override val classTable: ClassTable by lazy { ClassLoader(this).loadEverything() }
    }
  }
  private val table = catalog.classTable
  private val elaborator = PetElaborator(table)
  private val player1 = Player(parse("Player1"))
  private val vocabulary =
      Vocabulary.create(
          catalog,
          activeClassNames = table.allClassNames,
          inputOnlySynonyms = listOf("Chip" to "Token"),
      )

  @Test
  internal fun inputElaborationAppliesTheCompleteAuthoredSyntaxPackage() {
    val source = parse<InstructionTree>("2 Pulse, UNWRAP[Chip]")

    elaborator.elaborateInput(source, vocabulary, player1) shouldBe
        parse<InstructionTree>("Pulse!, Pulse!, Token<Player1>!")
  }

  @Test
  internal fun ordinaryInputRejectsPropertyEvaluationWhileMetricInputExpandsIt() {
    val source = parse<Metric>("EVAL Score.score")

    shouldThrow<PetSyntaxException> { elaborator.elaborateInput(source, vocabulary, player1) }
    elaborator.elaborateMetricInput(source, vocabulary, player1.expression, player1) shouldBe
        parse<Metric>("Pulse")
  }

  @Test
  internal fun classEffectsApplyTheCompleteDeclarationPackage() {
    val concreteRule = table.getClass(parse("ConcreteRule"))

    elaborator.classEffects(concreteRule) shouldBe
        listOf(parse("This BY Owner: Pulse!, Pulse!, Token<Owner>!"))
  }

  @Test
  internal fun classEffectsApplyDefaultsAgainstTheirClassContext() {
    elaborator.classEffects(table.getClass(parse("AreaRule"))).single() shouldBe
        parse<Effect>("This BY Owner: ContextualTile<Owner, This>!")
    elaborator.classEffects(table.getClass(parse("OwnerRule"))).single() shouldBe
        parse<Effect>("This: ContextualTile<This, LandArea>!")
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
        player1,
    ) shouldBe parse<Effect>("This: ContextRule<Player1>, Token<Player1>!")
  }
}
