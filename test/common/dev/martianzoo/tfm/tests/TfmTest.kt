package dev.martianzoo.tfm.tests

import dev.martianzoo.engine.Agent.Companion.parse
import dev.martianzoo.engine.Agent.OperationBody
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition
import dev.martianzoo.tfm.canon.TfmClasses.TILE
import dev.martianzoo.tfm.canon.cardResourceType
import dev.martianzoo.tfm.canon.tfmCatalog
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

internal abstract class TfmTest {
  protected lateinit var game: World

  protected val engine: TfmGameplay
    get() = game.tfm(ENGINE)

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

  private fun OperationBody.buyCards(count: Int) {
    require(count in 0..1) { "an individual card offer contains one selected card" }
    if (
        tasks
            .extract { it }
            .filter { task ->
              task.instruction.descendantsOfType<Change>().any { change ->
                change.gaining?.let { gaining ->
                  gaining.className == cn("ProjectCard") &&
                      cn("Selecting") in gaining.descendantsOfType<ClassName>()
                } == true
              }
            }
            .singleOrNull() != null
    ) {
      doTask("ProjectCard<Selecting>")
    }
    doTask(if (count == 0) "-ProjectCard<Selecting>" else "Ok")
    if (tasks.extract { it }.any { it.instruction.toString().startsWith("BuySelectedCards") }) {
      doTask("BuySelectedCards")
    }
    if (count > 0) {
      while (tasks.extract { it }.any { it.instruction.toString().startsWith("BuyCard<") }) {
        doTask("BuyCard / ProjectCard<Selecting>")
      }
      if (
          tasks
              .extract { it }
              .any {
                it.instruction.toString().let { text ->
                  text.startsWith("Invoice<") && "CardPurchase" in text
                }
              }
      ) {
        doTask("Invoice<CardPurchase, Action1>")
      }
      doTask("Pay<Class<MC>> FROM MC / Owed<>")
    }
  }

  protected fun TfmGameplay.placeTile(row: Int, column: Int): TaskResult =
      doTask(tilePlacement(reader, pendingTasks(), row, column))

  protected fun OperationBody.placeTile(row: Int, column: Int) {
    doTask(tilePlacement(reader, tasks.extract { it }, row, column))
  }

  protected fun TfmGameplay.addCardResources(card: ClassName, count: Int? = null): TaskResult =
      doTask(cardResources(reader, pendingTasks(), card, count))

  protected fun OperationBody.addCardResources(card: ClassName, count: Int? = null) {
    doTask(cardResources(reader, tasks.extract { it }, card, count))
  }

  protected fun TfmGameplay.wgt(choice: String): TaskResult = doTask("$choice! BY Engine")

  protected fun OperationBody.wgt(choice: String) {
    doTask("$choice! BY Engine")
  }

  protected fun TfmGameplay.assignWildTag(card: ClassName, tag: String): TaskResult =
      doTask("$tag<WildTagUse<$card>>")

  protected fun TfmGameplay.assignWildTag(tag: String): TaskResult =
      doTask(wildTagAssignment(pendingTasks(), tag))

  protected fun OperationBody.assignWildTag(card: ClassName, tag: String) {
    doTask("$tag<WildTagUse<$card>>")
  }

  protected fun OperationBody.assignWildTag(tag: String) {
    doTask(wildTagAssignment(tasks.extract { it }, tag))
  }

  protected fun TfmGameplay.declineTask(): TaskResult {
    val taskNumber = singleDeclinableTaskNumber(pendingTasks(), reader)
    return doTask("Ok", taskNumber)
  }

  protected fun TfmGameplay.declineTask(instruction: String): TaskResult {
    val taskNumber = singleDeclinableTaskNumber(pendingTasks(), reader, instruction)
    return doTask("Ok", taskNumber)
  }

  protected fun OperationBody.declineTask() {
    val taskNumber = singleDeclinableTaskNumber(tasks.extract { it }, reader)
    doTask("Ok", taskNumber)
  }

  protected fun OperationBody.declineTask(instruction: String) {
    val taskNumber = singleDeclinableTaskNumber(tasks.extract { it }, reader, instruction)
    doTask("Ok", taskNumber)
  }

  protected fun TfmGameplay.playCorp(
      cardName: ClassName,
      body: TfmGameplay.() -> Unit,
  ): TaskResult {
    val player = this
    return inTurn {
      doTask("PlayCard<Class<CorporationCard>, Class<$cardName>>")
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
                  (it.gaining.className == resourceType ||
                      it.gaining.className == cn("CardResource"))
            }
    val arguments = gain.gaining.arguments.toMutableList()
    if (arguments.isEmpty()) arguments += card.expression
    else arguments[arguments.lastIndex] = card.expression
    val revisedExpression = Expression(resourceType, arguments, argumentsSpecified = true)
    return "$gain".replace("${gain.gaining}", "$revisedExpression").removeSuffix("?")
  }

  private fun singleDeclinableTaskNumber(
      tasks: List<Task>,
      reader: dev.martianzoo.pets.api.GameReader,
      instruction: String? = null,
  ): Int {
    val matches =
        tasks.withIndex().filter { (_, task) ->
          (instruction == null ||
              task.instruction == game.agent(task.assignee).parse<Instruction>(instruction)) &&
              (NoOp.narrows(task.instruction, reader) ||
                  task.instruction.descendantsOfType<NoOp>().isNotEmpty())
        }
    require(matches.size == 1) {
      val qualifier = instruction?.let { " matching `$it`" } ?: ""
      "Expected exactly one task narrowable to Ok$qualifier, found ${matches.size}"
    }
    return matches.single().index + 1
  }

  private fun wildTagAssignment(tasks: List<Task>, tag: String): String {
    val use =
        tasks
            .asSequence()
            .flatMap { it.instruction.descendantsOfType<Expression>() }
            .firstOrNull { it.className == cn("WildTagUse") }
            ?: throw IllegalArgumentException("No pending wild-tag task")
    val card = requireNotNull(use.arguments.lastOrNull()?.className)
    return "$tag<WildTagUse<$card>>"
  }

  private fun TfmGameplay.pendingTasks(): List<Task> =
      game.tasks.extract { it }.filter { it.assignee == actor }
}
