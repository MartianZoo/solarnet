package dev.martianzoo.tfm.web.gameviewer

import dev.martianzoo.engine.Agent.Companion.parse
import dev.martianzoo.engine.Agent.OperationBody
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.GameRecording
import dev.martianzoo.engine.World
import dev.martianzoo.engine.exMachina
import dev.martianzoo.engine.recording
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.Task.TaskId
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.canon.TfmClasses.TILE
import dev.martianzoo.tfm.canon.cardResourceType
import dev.martianzoo.tfm.canon.tfmCatalog
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.fake.FakeCanon

public abstract class RecordedGame {
  protected lateinit var game: World

  protected val admin: TfmGameplay
    get() = game.tfm(dev.martianzoo.pets.data.Actor.ADMIN)

  protected abstract val config: GameConfig
  protected open val catalog: TfmCatalog by lazy {
    if (cn("FakeStuffBundle") in config.includedClassNames) {
      TfmCatalog.compose(Canon, FakeCanon)
    } else {
      Canon
    }
  }
  protected open val inputOnlySynonyms: List<Pair<String, String>> = CLASS_SYNONYMS

  public fun record(): GameRecording = record({}, {})

  internal fun record(
      onGameConstructed: () -> Unit,
      onReplayCompleted: () -> Unit,
  ): GameRecording {
    game = Engine.newGame(catalog.gamePremise(config), inputOnlySynonyms = inputOnlySynonyms)
    onGameConstructed()
    play()
    onReplayCompleted()
    return game.recording()
  }

  protected abstract fun play()

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
      doTask(tilePlacement(reader, pendingTasks(), row, column))

  protected fun OperationBody.placeTile(row: Int, column: Int) {
    doTask(tilePlacement(reader, tasks.extract { it }, row, column))
  }

  protected fun TfmGameplay.addCardResources(card: ClassName, count: Int? = null): TaskResult =
      doTask(cardResources(reader, pendingTasks(), card, count))

  protected fun OperationBody.addCardResources(card: ClassName, count: Int? = null) {
    doTask(cardResources(reader, tasks.extract { it }, card, count))
  }

  protected fun TfmGameplay.wgt(choice: String): TaskResult = doTask("$choice! BY Admin")

  protected fun OperationBody.wgt(choice: String) {
    doTask("$choice! BY Admin")
  }

  protected fun TfmGameplay.declineTask(): TaskResult {
    return doTask("Ok")
  }

  protected fun TfmGameplay.declineTask(instruction: String): TaskResult {
    val taskId = singleDeclinableTaskId(pendingTasks(), reader, instruction)
    return doTask("Ok", taskId)
  }

  protected fun OperationBody.declineTask() {
    doTask("Ok")
  }

  protected fun OperationBody.declineTask(instruction: String) {
    val taskId = singleDeclinableTaskId(tasks.extract { it }, reader, instruction)
    doTask("Ok", taskId)
  }

  protected fun TfmGameplay.exMachina(adjustment: String) {
    game.exMachina(this, adjustment)
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
    require(count == null || count > 0)
    val resourceType = requireNotNull(cardResourceType(reader.tfmCatalog.card(card)))
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
      task.instruction == game.agent(task.assignee).parse<Instruction>(instruction) &&
          (NoOp.narrows(task.instruction, reader) ||
              task.instruction.descendantsOfType<NoOp>().isNotEmpty())
    }
    require(matches.size == 1)
    return matches.single().id
  }

  private fun TfmGameplay.pendingTasks(): List<Task> =
      game.tasks.extract { it }.filter { it.assignee == actor }

  private companion object {
    val CLASS_SYNONYMS =
        listOf(
            "M" to "MC",
            "S" to "Steel",
            "T" to "Titanium",
            "P" to "Plant",
            "E" to "Energy",
            "H" to "Heat",
            "TR" to "TerraformRating",
            "VP" to "VictoryPoint",
        )
  }
}
