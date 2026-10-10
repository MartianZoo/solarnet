package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TransformHandlerTest {
  @Test
  internal fun handlerOnlyRewritesInsideItsMarkedSyntax() {
    val handler = TransformHandler { inner, _ ->
      PetNode.replacer(cn("Inside"), cn("Rewritten")).transformWithoutKindCheck(inner)
    }
    val dispatcher = TransformHandler.dispatcher(mapOf("MARK" to handler))

    dispatcher.transformInstructionTree(parse("MARK[Inside], Inside")).toString() shouldBe
        "Rewritten, Inside"
  }

  @Test
  internal fun transformedGroupIsSplicedIntoItsSurroundingGroup() {
    val dispatcher =
        TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { inner, _ -> inner }))

    dispatcher
        .transformInstructionTree(parse("MARK[Inside, AlsoInside], Outside"))
        .toString() shouldBe "Inside, AlsoInside, Outside"
  }

  @Test
  internal fun transformedSequenceIsSplicedIntoItsSurroundingSequence() {
    val dispatcher =
        TransformHandler.dispatcher(
            mapOf(
                "MARK" to
                    TransformHandler { _, _ -> parse<InstructionTree>("Inside THEN AlsoInside") }
            )
        )

    dispatcher.transformInstructionTree(parse("Outside THEN MARK[Ignored]")).toString() shouldBe
        "Outside THEN Inside THEN AlsoInside"
  }

  @Test
  internal fun cardinalityChangingTransformRequiresTheInstructionTreeEntryPoint() {
    val dispatcher =
        TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { inner, _ -> inner }))
    val source = parse<Instruction>("MARK[Inside, AlsoInside]")

    shouldThrow<IllegalStateException> { dispatcher.transformInstruction(source) }
    dispatcher.transformInstructionTree(source).toString() shouldBe "Inside, AlsoInside"
  }

  @Test
  internal fun unregisteredTransformIsLeftForALaterPass() {
    val dispatcher = TransformHandler.dispatcher(emptyMap())

    dispatcher.transformInstructionTree(parse<InstructionTree>("LATER[Inside]")).toString() shouldBe
        "LATER[Inside]"
  }

  @Test
  internal fun handlerCanPreserveItsMarkedSyntax() {
    val dispatcher = TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { _, _ -> null }))

    dispatcher.transformInstructionTree(parse("MARK[Inside]")).toString() shouldBe "MARK[Inside]"
  }

  @Test
  internal fun sameTransformKindCannotBeNested() {
    val dispatcher =
        TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { inner, _ -> inner }))

    shouldThrow<ExpressionException> {
      dispatcher.transformInstructionTree(parse<InstructionTree>("MARK[MARK[Inside]]"))
    }
  }

  @Test
  internal fun idempotentTransformsStillComposeAcrossDifferentKinds() {
    val heat =
        object : TransformHandler {
          override val idempotent = true

          override fun transform(inner: PetNode, scope: TransformHandler.Scope): PetNode =
              PetNode.replacer(cn("Plant"), cn("Heat")).transformWithoutKindCheck(inner)
        }
    val steel = TransformHandler { inner, _ ->
      PetNode.replacer(cn("Heat"), cn("Steel")).transformWithoutKindCheck(inner)
    }
    val dispatcher = TransformHandler.dispatcher(mapOf("HEAT" to heat, "STEEL" to steel))

    dispatcher.transformInstructionTree(parse("HEAT[STEEL[HEAT[Plant]]]")).toString() shouldBe
        "Steel"
  }

  @Test
  internal fun handlerMustReturnTheSamePetsFamily() {
    val dispatcher =
        TransformHandler.dispatcher(
            mapOf("MARK" to TransformHandler { _, _ -> parse<Metric>("Different") })
        )

    shouldThrow<IllegalStateException> {
      dispatcher.transformInstructionTree(parse<InstructionTree>("MARK[Inside]"))
    }
  }
}
