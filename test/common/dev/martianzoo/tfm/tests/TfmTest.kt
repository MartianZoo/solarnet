package dev.martianzoo.tfm.tests

import dev.martianzoo.engine.Agent.Companion.parse
import dev.martianzoo.engine.Agent.OperationBody
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.GameReader
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
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

internal abstract class TfmTest {
  protected lateinit var game: World

  protected val admin: TfmGameplay
    get() = game.tfm(ADMIN)

  protected fun TaskResult.expect(string: String) = TestHelpers.assertNetChanges(this, game, string)

  protected fun <T> OperationBody.doWithoutAutoExec(
      agent: TfmGameplay,
      body: OperationBody.() -> T,
  ): T {
    val previousAutoExecMode = agent.autoExecMode
    agent.autoExecMode = NONE
    return try {
      body()
    } finally {
      agent.autoExecMode = previousAutoExecMode
    }
  }

  protected fun TfmGameplay.placeTile(row: Int, column: Int): TaskResult =
      doTask(tilePlacement(reader, tasks.extract { it }, row, column))

  protected fun OperationBody.placeTile(row: Int, column: Int) =
      doTask(tilePlacement(reader, tasks.extract { it }, row, column))

  protected fun TfmGameplay.addCardResources(card: ClassName, count: Int? = null): TaskResult =
      doTask(cardResources(reader, tasks.extract { it }, card, count))

  protected fun OperationBody.addCardResources(card: ClassName, count: Int? = null) =
      doTask(cardResources(reader, tasks.extract { it }, card, count))

  protected fun TfmGameplay.wgt(choice: String): TaskResult = doTask("$choice! BY Admin")

  protected fun OperationBody.wgt(choice: String) = doTask("$choice! BY Admin")

  protected fun TfmGameplay.declineTask(): TaskResult = doTask("Ok")

  protected fun TfmGameplay.declineTask(instruction: String): TaskResult =
      doTask("Ok", singleDeclinableTaskId(tasks.extract { it }, reader, instruction))

  protected fun OperationBody.declineTask() = doTask("Ok")

  protected fun OperationBody.declineTask(instruction: String) =
      doTask("Ok", singleDeclinableTaskId(tasks.extract { it }, reader, instruction))

  private fun tilePlacement(
      reader: GameReader,
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
      reader: GameReader,
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
      reader: GameReader,
      instruction: String,
  ): TaskId {
    val matches = tasks.filter { task ->
      task.instruction == game.tfm(task.assignee).parse<Instruction>(instruction) &&
          (NoOp.narrows(task.instruction, reader) ||
              task.instruction.descendantsOfType<NoOp>().isNotEmpty())
    }
    require(matches.size == 1) {
      "Expected exactly one task narrowable to Ok matching `$instruction`, found ${matches.size}"
    }
    return matches.single().id
  }
}
