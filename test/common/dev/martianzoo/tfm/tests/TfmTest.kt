package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.Task.TaskId
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition
import dev.martianzoo.tfm.canon.TfmClasses.TILE
import dev.martianzoo.tfm.canon.cardResourceType
import dev.martianzoo.tfm.canon.tfmCatalog
import dev.martianzoo.tfm.engine.*

internal abstract class TfmTest {
  protected lateinit var game: World

  protected val admin: TfmGameplay
    get() = game.testTfm(ADMIN)

  protected fun TaskResult.expect(string: String) = TestHelpers.assertNetChanges(this, game, string)

  protected fun <T> OperationScope.doWithoutAutoExec(
      agent: TfmGameplay,
      body: OperationScope.() -> T,
  ): T {
    val previousAutoExecPolicy = agent.autoExecPolicy
    agent.autoExecPolicy = NONE
    return try {
      body()
    } finally {
      agent.autoExecPolicy = previousAutoExecPolicy
    }
  }

  protected fun TfmGameplay.placeTile(row: Int, column: Int): TaskResult =
      doTask(tilePlacement(reader, pendingTasks(), row, column))

  protected fun OperationScope.placeTile(row: Int, column: Int) {
    doTask(tilePlacement(reader, tasks.extract { it }, row, column))
  }

  protected fun TfmGameplay.addCardResources(card: ClassName, count: Int? = null): TaskResult =
      doTask(cardResources(reader, pendingTasks(), card, count))

  protected fun OperationScope.addCardResources(card: ClassName, count: Int? = null) {
    doTask(cardResources(reader, tasks.extract { it }, card, count))
  }

  protected fun TfmGameplay.wgt(choice: String): TaskResult = doTask("$choice! BY Admin")

  protected fun OperationScope.wgt(choice: String) {
    doTask("$choice! BY Admin")
  }

  protected fun TfmGameplay.declineTask(): TaskResult {
    return doTask("Ok")
  }

  protected fun TfmGameplay.declineTask(instruction: String): TaskResult {
    val taskId = singleDeclinableTaskId(pendingTasks(), reader, instruction)
    return doTask("Ok", taskId)
  }

  protected fun OperationScope.declineTask() {
    doTask("Ok")
  }

  protected fun OperationScope.declineTask(instruction: String) {
    val taskId = singleDeclinableTaskId(tasks.extract { it }, reader, instruction)
    doTask("Ok", taskId)
  }

  protected fun TfmGameplay.playCorp(
      cardName: ClassName,
      body: TfmGameplay.() -> Unit = {},
  ): TaskResult {
    val player = this
    return inTurn {
      playCorp(cardName)
      player.body()
    }
  }

  private fun tilePlacement(
      reader: dev.martianzoo.pets.api.GameReader,
      tasks: List<Task>,
      row: Int,
      column: Int,
  ): String {
    val area =
        mapDefinition(reader).areas.singleOrNull { it.row == row && it.column == column }
            ?: throw IllegalArgumentException("No map area at row $row, column $column")
    val tileType = reader.resolve(TILE.expression)
    return tasks
        .mapNotNull { task ->
          task.instruction.descendantsOfType<Gain>().firstOrNull { gain ->
            reader.resolve(gain.gaining).narrows(tileType, reader)
          }
        }
        .map { "${it.gaining.className}<${area.className}>" }
        .distinct()
        .single()
  }

  private fun cardResources(
      reader: dev.martianzoo.pets.api.GameReader,
      tasks: List<Task>,
      card: ClassName,
      count: Int?,
  ): String {
    require(count == null || count > 0) { "Card-resource count must be positive" }
    val resourceType =
        requireNotNull(cardResourceType(reader.tfmCatalog.card(card))) {
          "$card does not hold card resources"
        }
    val gain =
        tasks
            .flatMap { it.instruction.descendantsOfType<Gain>() }
            .single {
              (count == null || it.count == ActualScalar(count)) &&
                  reader.catalog.classTable
                      .getClass(resourceType)
                      .isSubtypeOf(reader.resolve(it.gaining).rootClass)
            }
    val arguments = gain.gaining.arguments.toMutableList()
    if (arguments.isEmpty()) arguments += card.expression
    else arguments[arguments.lastIndex] = card.expression
    val revisedExpression = Expression(resourceType, arguments, argumentsSpecified = true)
    return "$gain".replace("${gain.gaining}", "$revisedExpression").removeSuffix("?")
  }

  private fun singleDeclinableTaskId(
      tasks: List<Task>,
      reader: dev.martianzoo.pets.api.GameReader,
      instruction: String,
  ): TaskId {
    val matches = tasks.filter { task ->
      task.instruction == game.testAgent(task.assignee).parse<Instruction>(instruction) &&
          (NoOp.narrows(task.instruction, reader) ||
              task.instruction.descendantsOfType<NoOp>().isNotEmpty())
    }
    require(matches.size == 1) {
      "Expected exactly one task narrowable to Ok matching `$instruction`, found ${matches.size}"
    }
    return matches.single().id
  }

  private fun TfmGameplay.pendingTasks(): List<Task> =
      game.tasks.extract { it }.filter { it.assignee == actor }
}
