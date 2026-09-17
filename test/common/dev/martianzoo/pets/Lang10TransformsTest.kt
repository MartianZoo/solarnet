package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.TransformNode
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.types.testCatalog
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Section 10 of `docs/pets-language-spec.md`: marking a subtree for a named rewrite. */
internal class Lang10TransformsTest {

  private val identity = TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { it }))

  // L10-1 The shape, and which nodes accept a block

  @Test
  internal fun `L10-1 a block is an all-caps kind, brackets and one node`() {
    (parse<InstructionTree>("PROD[Plant]") as Instruction.Transform).transformKind shouldBe "PROD"
    (parse<Metric>("PROD[Plant]") as Metric.Transform).transformKind shouldBe "PROD"
    (parse<Requirement>("PROD[Plant]") as Requirement.Transform).transformKind shouldBe "PROD"
    (parse<Action.Cost>("PROD[Plant]") as Action.Cost.Transform).transformKind shouldBe "PROD"
    (parse<Effect.Trigger>("PROD[Plant]") as Effect.Trigger.Transform).transformKind shouldBe "PROD"
  }

  @Test
  internal fun `L10-1 an expression is not a node that accepts a block`() {
    shouldThrow<PetSyntaxException> { parse<dev.martianzoo.pets.ast.Expression>("PROD[Plant]") }
  }

  // L10-2 Every mark names a kind its Catalog defines

  @Test
  internal fun `L10-2 a Catalog must define every transform kind its source uses`() {
    shouldThrow<PetException> {
          testCatalog("CLASS Result\nCLASS Marked { This: LATER[Result] }").classTable
        }
        .message
        .orEmpty() shouldContain "transform kind `LATER`"
  }

  @Test
  internal fun `L10-2 premise source must also use defined transform kinds`() {
    val catalog = testCatalog("CLASS Result")

    shouldThrow<PetException> {
          GamePremise(
                  catalog = catalog,
                  modules = emptySet(),
                  classSelections = emptySet(),
                  initialComponentTypes = emptySet(),
                  premiseClassDeclarations =
                      parseClasses("CLASS LocalMarked { This: LATER[Result] }").toSet(),
              )
              .classTable
        }
        .message
        .orEmpty() shouldContain "transform kind `LATER`"
  }

  @Test
  internal fun `L10-2 one pass leaves another pass's kind in place`() {
    identity.transformInstructionTree(parse("MARK[LATER[Plant]]")).toString() shouldBe
        "LATER[Plant]"
  }

  @Test
  internal fun `L10-2 a handler may still decline to rewrite its own block`() {
    TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { null }))
        .transformInstructionTree(parse("MARK[Plant]"))
        .toString() shouldBe "MARK[Plant]"
  }

  @Test
  internal fun `L10-2 preserved blocks must be handled before semantic operations`() {
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

  // L10-3 A handler rewrites only inside its block

  @Test
  internal fun `L10-3 a handler rewrites only inside its own block`() {
    val handler = TransformHandler { inner ->
      PetNode.replacer(cn("Inside"), cn("Rewritten")).transformWithoutKindCheck(inner)
    }

    TransformHandler.dispatcher(mapOf("MARK" to handler))
        .transformInstructionTree(parse("MARK[Inside], Inside"))
        .toString() shouldBe "Rewritten, Inside"
  }

  @Test
  internal fun `L10-3 a handler must return the same kind of Pets`() {
    shouldThrow<PetSyntaxException> {
      TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { parse<Metric>("Different") }))
          .transformInstructionTree(parse("MARK[Plant]"))
    }
  }

  @Test
  internal fun `L10-3 a block expanding into several instructions splices into its group`() {
    identity.transformInstructionTree(parse("MARK[Plant, Heat], Steel")).toString() shouldBe
        "Plant, Heat, Steel"
  }

  // L10-4 Trigger blocks

  @Test
  internal fun `L10-4 a trigger block wraps only a gain or removal`() {
    parse<Effect>("PROD[Plant]: Heat").toString() shouldBe "PROD[Plant]: Heat"
    parse<Effect>("PROD[-Plant]: Heat").toString() shouldBe "PROD[-Plant]: Heat"
    parse<Effect>("PROD[X Plant]: Heat").toString() shouldBe "PROD[X Plant]: Heat"
    shouldThrow<PetSyntaxException> { parse<Effect>("PROD[Plant OR Heat]: Steel") }
    shouldThrow<PetSyntaxException> { parse<Effect>("PROD[Plant BY Anyone]: Steel") }
    parse<Effect>("PROD[Plant] OR PROD[Heat]: Steel").toString() shouldBe
        "PROD[Plant] OR PROD[Heat]: Steel"
  }

  // L10-5 Same-kind nesting

  @Test
  internal fun `L10-5 nesting a block of the same kind is representable but not processable`() {
    parse<InstructionTree>("PROD[PROD[Plant]]").toString() shouldBe "PROD[PROD[Plant]]"
    shouldThrow<PetSyntaxException> {
      identity.transformInstructionTree(parse("MARK[MARK[Plant]]"))
    }
    shouldThrow<IllegalArgumentException> {
      TransformNode.wrap(parse<InstructionTree>("PROD[Plant], Heat"), "PROD")
    }
  }

  @Test
  internal fun `L10-5 blocks of different kinds nest freely`() {
    roundTrip<InstructionTree>("PROD[LATER[Plant]]")

    val both =
        TransformHandler.dispatcher(
            mapOf("MARK" to TransformHandler { it }, "OTHER" to TransformHandler { it })
        )
    both.transformInstructionTree(parse("MARK[OTHER[Plant]]")).toString() shouldBe "Plant"
  }
}
