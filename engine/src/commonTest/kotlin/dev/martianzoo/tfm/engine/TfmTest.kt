package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Task
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Gameplay.OperationBody
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.api.ApiUtils.mapDefinition
import dev.martianzoo.tfm.api.tfmAuthority
import dev.martianzoo.tfm.data.TfmClasses.TILE
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

abstract class TfmTest {
  protected lateinit var game: World

  protected val engine: TfmGameplay
    get() = game.tfm(ENGINE)

  protected fun TaskResult.expect(string: String) = TestHelpers.assertNetChanges(this, game, string)

  protected fun OperationBody.buyCards(count: Int) {
    doTask(if (count == 0) "Ok" else "$count BuyCard")
    if (count > 0) {
      if (
          tasks
              .extract { it }
              .any {
                it.instruction.toString().let { text ->
                  text.startsWith("Invoice<") && "BuyCards" in text
                }
              }
      ) {
        doTask("Invoice<BuyCards, First>")
      }
      doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<>")
    }
  }

  protected fun TfmGameplay.placeTile(row: Int, column: Int): TaskResult =
      doTask(tilePlacement(reader, pendingTasks(), row, column))

  protected fun OperationBody.placeTile(row: Int, column: Int) {
    doTask(tilePlacement(reader, tasks.extract { it }, row, column))
  }

  protected fun TfmGameplay.addCardResources(card: ClassName): TaskResult =
      doTask(cardResources(reader, pendingTasks(), card))

  protected fun OperationBody.addCardResources(card: ClassName) {
    doTask(cardResources(reader, tasks.extract { it }, card))
  }

  protected fun TfmGameplay.wgt(choice: String): TaskResult = doTask("$choice! BY Engine")

  protected fun OperationBody.wgt(choice: String) {
    doTask("$choice! BY Engine")
  }

  protected fun TfmGameplay.assignWildTag(card: ClassName, tag: String): TaskResult =
      doTask("$tag<WildTagUse<$card>>")

  protected fun OperationBody.assignWildTag(card: ClassName, tag: String) {
    doTask("$tag<WildTagUse<$card>>")
  }

  protected fun TfmGameplay.declineTask(): TaskResult {
    requireSingleDeclinableTask(pendingTasks(), reader)
    return doTask("Ok")
  }

  protected fun OperationBody.declineTask() {
    requireSingleDeclinableTask(tasks.extract { it }, reader)
    doTask("Ok")
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
      reader: dev.martianzoo.api.GameReader,
      tasks: List<Task>,
      row: Int,
      column: Int,
  ): String {
    val area =
        mapDefinition(reader).areas.singleOrNull { it.row == row && it.column == column }
            ?: throw IllegalArgumentException("No map area at row $row, column $column")
    val tileType = reader.resolve(TILE.expression)
    val task = tasks.single { task ->
      task.instruction.descendantsOfType<Gain>().any { gain ->
        reader.resolve(gain.gaining).narrows(tileType, reader)
      }
    }
    val gain =
        task.instruction.descendantsOfType<Gain>().first { gain ->
          reader.resolve(gain.gaining).narrows(tileType, reader)
        }
    return "${gain.gaining.className}<${area.className}>"
  }

  private fun cardResources(
      reader: dev.martianzoo.api.GameReader,
      tasks: List<Task>,
      card: ClassName,
  ): String {
    val resourceType =
        requireNotNull(reader.tfmAuthority.card(card).resourceType) {
          "$card does not hold card resources"
        }
    val gain =
        tasks
            .flatMap { it.instruction.descendantsOfType<Gain>() }
            .single {
              it.gaining.className == resourceType || it.gaining.className == cn("CardResource")
            }
    val arguments = gain.gaining.arguments.toMutableList()
    if (arguments.isEmpty()) arguments += card.expression
    else arguments[arguments.lastIndex] = card.expression
    val revisedExpression = Expression(resourceType, arguments, argumentsSpecified = true)
    return "$gain".replace("${gain.gaining}", "$revisedExpression").removeSuffix("?")
  }

  private fun requireSingleDeclinableTask(
      tasks: List<Task>,
      reader: dev.martianzoo.api.GameReader,
  ) {
    val declinable = tasks.count { task ->
      NoOp.narrows(task.instruction, reader) ||
          task.instruction.descendantsOfType<NoOp>().isNotEmpty()
    }
    require(declinable == 1) { "Expected exactly one task narrowable to Ok, found $declinable" }
  }

  private fun TfmGameplay.pendingTasks(): List<Task> =
      game.tasks.extract { it }.filter { it.assignee == actor }
}
