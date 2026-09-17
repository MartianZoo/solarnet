package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Task.Selection.DELEGATED
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskNormalizationTest {
  private val premise =
      testGamePremise(
          """
          CLASS Plant
          CLASS Heat
          CLASS Steel
          CLASS Energy
          """,
          players = 2,
      )
  private val world = GameWorld(premise)
  private val queues = TaskQueues(world, premise.classTable)

  @Test
  internal fun `editing a task applies admission normalization without changing its lifecycle`() {
    val original = addTask("X Plant THEN X Heat")

    queues.editTask(
        original.copy(
            actor = PLAYER2,
            selection = DELEGATED,
            instruction = parse<Instruction>("3 Plant THEN 3 Heat"),
        )
    )

    val edited = world.tasks.getTaskData(original.id)
    edited.id shouldBe original.id
    edited.controller shouldBe PLAYER1
    edited.actor shouldBe PLAYER2
    edited.selection shouldBe DELEGATED
    edited.instruction shouldBe parse<Instruction>("3 Plant")
    edited.then shouldBe instructionGroup("3 Heat")
  }

  @Test
  internal fun `editing preserves an existing continuation as a separate variable scope`() {
    val original = addTask("Plant THEN X Heat")

    queues.editTask(original.copy(instruction = parse<Instruction>("X Steel THEN Energy")))

    val edited = world.tasks.getTaskData(original.id)
    edited.instruction shouldBe parse<Instruction>("X Steel THEN Energy")
    edited.then shouldBe instructionGroup("X Heat")
  }

  @Test
  internal fun `terminal normalization composes without inventing no-op tasks`() {
    queues.addTasks(instructionGroup("Die? BY Player1"), PLAYER1, cause = null) shouldBe emptyList()
    queues.addTasks(instructionGroup("Die? / Plant"), PLAYER1, cause = null) shouldBe emptyList()
    queues.addTasks(instructionGroup("EACH Player { Die? }"), PLAYER1, cause = null) shouldBe
        emptyList()

    val leading = addTask("Die? THEN Plant")
    leading.instruction shouldBe parse<Instruction>("Plant")
    leading.then shouldBe null

    val trailing = addTask("Heat THEN Die?")
    trailing.instruction shouldBe parse<Instruction>("Heat")
    trailing.then shouldBe null
  }

  @Test
  internal fun `a no-op choice and a gated no-op remain meaningful`() {
    addTask("Die? OR Plant").instruction shouldBe parse<Instruction>("Ok OR Plant")
    addTask("Plant: Die?").instruction shouldBe parse<Instruction>("Plant: Ok")
  }

  private fun addTask(instruction: String) =
      queues.addTasks(instructionGroup(instruction), PLAYER1, cause = null).single().task

  private fun instructionGroup(instruction: String): InstructionGroup =
      InstructionGroup.of(parse<InstructionTree>(instruction))
}
