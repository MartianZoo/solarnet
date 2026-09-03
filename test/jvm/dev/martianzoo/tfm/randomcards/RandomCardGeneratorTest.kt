package dev.martianzoo.tfm.randomcards

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RandomCardGeneratorTest {
  @Test
  internal fun generatedCardsComposeAndExerciseStructuredPets() {
    val cards = RandomCardGenerator(seed = 8675309).generate(count = 40)
    val nodes = cards.flatMap { it.allNodes }.flatMap { it.descendantsOfType<PetNode>() }
    val instructionNodes =
        cards
            .flatMap { card ->
              card.authoredEffects.map { it.instruction } +
                  card.authoredActions.map { it.instruction }
            }
            .flatMap { it.descendantsOfType<PetNode>() }

    cards shouldHaveSize 40
    cards.map { it.className }.distinct() shouldHaveSize 40
    cards.all { !it.abstract } shouldBe true
    (nodes.count { it is Expression.Refinement } >= 20) shouldBe true
    (nodes.count { it is Instruction.Then || it is Instruction.Gated || it is Instruction.Per } >=
        12) shouldBe true
    nodes.any { it is Requirement.And || it is Requirement.Or } shouldBe true

    val skippableGates =
        instructionNodes
            .filterIsInstance<Instruction.Or>()
            .filter { choice -> choice.instructions.any { it is Instruction.NoOp } }
            .flatMap { choice -> choice.instructions.filterIsInstance<Instruction.Gated>() }
    skippableGates.isNotEmpty() shouldBe true
    instructionNodes.filterIsInstance<Instruction.Then>().all { sequence ->
      sequence.stages.all { it is Instruction.Remove }
    } shouldBe true
  }
}
