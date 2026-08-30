package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TransformHandlerTest {
  @Test
  internal fun handlerOnlyRewritesInsideItsMarkedSyntax() {
    val handler = TransformHandler { inner ->
      PetNode.replacer(cn("Inside"), cn("Rewritten")).transformWithoutKindCheck(inner)
    }
    val dispatcher = TransformHandler.dispatcher(mapOf("MARK" to handler))

    dispatcher.transformInstructionTree(parse("MARK[Inside], Inside")).toString() shouldBe
        "Rewritten, Inside"
  }

  @Test
  internal fun transformedGroupIsSplicedIntoItsSurroundingGroup() {
    val dispatcher = TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { it }))

    dispatcher
        .transformInstructionTree(parse("MARK[Inside, AlsoInside], Outside"))
        .toString() shouldBe "Inside, AlsoInside, Outside"
  }

  @Test
  internal fun unregisteredTransformIsPreservedForAnotherStage() {
    val dispatcher = TransformHandler.dispatcher(emptyMap())

    dispatcher.transformInstructionTree(parse<InstructionTree>("LATER[Inside]")).toString() shouldBe
        "LATER[Inside]"
  }

  @Test
  internal fun handlerCanPreserveItsMarkedSyntax() {
    val dispatcher = TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { null }))

    dispatcher.transformInstructionTree(parse("MARK[Inside]")).toString() shouldBe "MARK[Inside]"
  }

  @Test
  internal fun sameTransformKindCannotBeNested() {
    val dispatcher = TransformHandler.dispatcher(mapOf("MARK" to TransformHandler { it }))

    shouldThrow<PetSyntaxException> {
      dispatcher.transformInstructionTree(parse<InstructionTree>("MARK[MARK[Inside]]"))
    }
  }

  @Test
  internal fun handlerMustReturnTheSamePetsFamily() {
    val dispatcher =
        TransformHandler.dispatcher(
            mapOf("MARK" to TransformHandler { parse<Metric>("Different") })
        )

    shouldThrow<PetSyntaxException> {
      dispatcher.transformInstructionTree(parse<InstructionTree>("MARK[Inside]"))
    }
  }
}
