package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ActionTree
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.EffectTree
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.ClassLoader
import dev.martianzoo.pets.types.PremiseClassTable
import dev.martianzoo.pets.types.loadTypes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Section 8 of `docs/pets-language-spec.md`: marking a subtree for a named rewrite. */
internal class Lang08TransformsTest {

  private val identity =
      TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { inner, _ -> inner }))

  // L8-1 The shape, and which nodes accept a block

  @Test
  internal fun `L8-1 a block is an all-caps kind, brackets and one node`() {
    (parse<InstructionTree>("PROD[Plant]") as Instruction.Transform).transformKind shouldBe "PROD"
    (parse<Metric>("PROD[Plant]") as Metric.Transform).transformKind shouldBe "PROD"
    (parse<Requirement>("PROD[Plant]") as Requirement.Transform).transformKind shouldBe "PROD"
    (parse<Action.Cost>("PROD[Plant]") as Action.Cost.Transform).transformKind shouldBe "PROD"
    (parse<Effect.Trigger>("PROD[Plant]") as Effect.Trigger.Transform).transformKind shouldBe "PROD"
  }

  @Test
  internal fun `L8-1 an expression is not a node that accepts a block`() {
    shouldThrow<PetSyntaxException> { parse<dev.martianzoo.pets.ast.Expression>("PROD[Plant]") }
  }

  @Test
  internal fun `L8-1 whole effect and action marks preserve their complete syntax`() {
    val effect = parse<EffectTree>("MARK[Plant: Heat]")
    val action = parse<ActionTree>("MARK[Plant -> Heat]")
    roundTrip<EffectTree>(effect.toString())
    roundTrip<ActionTree>(action.toString())
    identity.transformEffect(effect) shouldBe parse<Effect>("Plant: Heat")
    identity.transformActionTree(action) shouldBe parse<Action>("Plant -> Heat")
    shouldThrow<IllegalStateException> {
      PetTransformer.noOp().transformEffect(effect)
    }
  }

  @Test
  internal fun `L8-5 repeated OWN is idempotent`() {
    langElaborator
        .elaborateInput(parse<InstructionTree>("OWN[OWN[Plant]]"), player1)
        .toString() shouldBe "Plant<Player1>!"
  }

  // L8-2 Every mark names a kind its Catalog defines

  @Test
  internal fun `L8-2 a Catalog must define every transform kind its source uses`() {
    shouldThrow<InvalidPetDefinitionException> {
          loadTypes("CLASS Result\nCLASS Marked { This: LATER[Result] }")
        }
        .message
        .orEmpty() shouldContain "transform kind `LATER`"
  }

  @Test
  internal fun `L8-2 premise source must also use defined transform kinds`() {
    val universe = loadTypes("CLASS Result")

    shouldThrow<InvalidPetDefinitionException> {
          ClassLoader.forPremise(
              premiseTable =
                  PremiseClassTable(
                      universe,
                      parseClasses("CLASS LocalMarked { This: LATER[Result] }").toSet(),
                  ),
              roots = setOf(),
          )
        }
        .message
        .orEmpty() shouldContain "transform kind `LATER`"
  }

  @Test
  internal fun `L8-2 one pass leaves another pass's kind in place`() {
    identity.transformInstructionTree(parse("MARK[LATER[Plant]]")).toString() shouldBe
        "LATER[Plant]"
  }

  @Test
  internal fun `L8-2 a handler may still decline to rewrite its own block`() {
    TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { _, _ -> null }))
        .transformInstructionTree(parse("MARK[Plant]"))
        .toString() shouldBe "MARK[Plant]"
  }

  @Test
  internal fun `L8-2 preserved blocks must be handled before semantic operations`() {
    shouldThrow<ExpressionException> {
      parse<Metric>("LATER[Plant]").evaluate({ 0 }, { 0 }, { 0 }, { 0 })
    }
    shouldThrow<ExpressionException> {
      parse<Requirement>("LATER[Plant]").isMetBy { 0 }
    }

    val instruction = parse<Instruction>("LATER[Plant]")
    shouldThrow<ExpressionException> { instruction.isAbstract(langWorld) }
    shouldThrow<ExpressionException> { instruction.ensureNarrows(instruction, langWorld) }
  }

  // L8-3 A handler rewrites only inside its block

  @Test
  internal fun `L8-3 a handler rewrites only inside its own block`() {
    val handler = TransformHandler { inner, _ ->
      PetNode.replacer(cn("Inside"), cn("Rewritten")).transformWithoutKindCheck(inner)
    }

    TransformHandler.dispatcher(mapOf("MARK" to handler))
        .transformInstructionTree(parse("MARK[Inside], Inside"))
        .toString() shouldBe "Rewritten, Inside"
  }

  @Test
  internal fun `L8-3 a handler must return the same kind of Pets`() {
    shouldThrow<IllegalStateException> {
      TransformHandler.dispatcher(
              mapOf("MARK" to TransformHandler { _, _ -> parse<Metric>("Different") })
          )
          .transformInstructionTree(parse("MARK[Plant]"))
    }
  }

  @Test
  internal fun `L8-3 a block expanding into several instructions splices into its group`() {
    identity.transformInstructionTree(parse("MARK[Plant, Heat], Steel")).toString() shouldBe
        "Plant, Heat, Steel"
  }

  // L8-4 Trigger blocks

  @Test
  internal fun `L8-4 a trigger block wraps only a gain or removal`() {
    parse<Effect>("PROD[Plant]: Heat").toString() shouldBe "PROD[Plant]: Heat"
    parse<Effect>("PROD[-Plant]: Heat").toString() shouldBe "PROD[-Plant]: Heat"
    parse<Effect>("PROD[X Plant]: Heat").toString() shouldBe "PROD[X Plant]: Heat"
    shouldThrow<PetSyntaxException> { parse<Effect>("PROD[Plant OR Heat]: Steel") }
    shouldThrow<PetSyntaxException> { parse<Effect>("PROD[Plant BY Actor]: Steel") }
    parse<Effect>("PROD[Plant] OR PROD[Heat]: Steel").toString() shouldBe
        "PROD[Plant] OR PROD[Heat]: Steel"
  }

  // L8-5 Same-kind nesting

  @Test
  internal fun `L8-5 nesting a block of the same kind is representable but not processable`() {
    parse<InstructionTree>("PROD[PROD[Plant]]").toString() shouldBe "PROD[PROD[Plant]]"
    shouldThrow<ExpressionException> {
      identity.transformInstructionTree(parse("MARK[MARK[Plant]]"))
    }
  }

  @Test
  internal fun `L8-5 blocks of different kinds nest freely`() {
    roundTrip<InstructionTree>("PROD[LATER[Plant]]")

    val both =
        TransformHandler.dispatcher(
            mapOf(
                "MARK" to TransformHandler { inner, _ -> inner },
                "OTHER" to TransformHandler { inner, _ -> inner },
            )
        )
    both.transformInstructionTree(parse("MARK[OTHER[Plant]]")).toString() shouldBe "Plant"
  }

  @Test
  internal fun `L8-6 deferred transforms retain their composition order`() {
    fun rename(kind: String, from: String, to: String): TransformHandler =
        TransformHandler { inner, scope ->
          object : TransformHandler.Rewriter(kind, scope) {
                override fun rewrite(node: PetNode): PetNode =
                    if (node == cn(from)) cn(to) else transformChildren(node)
              }
              .transformWithoutKindCheck(inner)
        }
    val table =
        loadTypes(
            """
          CLASS Plant
          CLASS Heat
          CLASS Steel
          CLASS Rule {
            score = COUNT "Plant"
            mixed = COUNT "HEAT[Plant] - Plant"
          }
        """,
            transformHandlerFactories =
                mapOf(
                    "HEAT" to { _ -> rename("HEAT", "Plant", "Heat") },
                    "STEEL" to { _ -> rename("STEEL", "Heat", "Steel") },
                ),
        )
    val elaborator = PetElaborator(table)
    elaborator.elaborateMetricInput(parse("STEEL[HEAT[EVAL Rule.score]]"), parse("Rule")) shouldBe
        parse<Metric>("Steel")
    elaborator.elaborateMetricInput(parse("HEAT[STEEL[EVAL Rule.score]]"), parse("Rule")) shouldBe
        parse<Metric>("Heat")
    shouldThrow<ExpressionException> {
      elaborator.elaborateMetricInput(parse("HEAT[HEAT[EVAL Rule.score]]"), parse("Rule"))
    }
    shouldThrow<ExpressionException> {
      elaborator.elaborateMetricInput(parse("HEAT[EVAL Rule.mixed]"), parse("Rule"))
    }
  }

  @Test
  internal fun `L8-6 properties receive defaults before their own transforms at every use`() {
    val table =
        loadTypes(
            """
          ABSTRACT CLASS Area {
            CLASS First
            CLASS Second
          }
          CLASS Plant<Area> { DEFAULT Plant<First> }
          CLASS Heat<Area> { DEFAULT Heat<Second> }
          CLASS Rule { score = COUNT "HEAT[Plant]" }
        """,
            transformHandlerFactories =
                mapOf(
                    "HEAT" to
                        { _ ->
                          TransformHandler { inner, _ ->
                            PetNode.replacer(cn("Plant"), cn("Heat"))
                                .transformWithoutKindCheck(inner)
                          }
                        }
                ),
        )
    val elaborator = PetElaborator(table)
    listOf("EVAL Rule.score", "OWN[EVAL Rule.score]").forEach { source ->
      elaborator.elaborateMetricInput(parse(source), parse("Rule")) shouldBe
          parse<Metric>("Heat<First>")
    }
  }
}
