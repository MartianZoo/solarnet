package dev.martianzoo.tfm.web.gameviewer

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.createAgents
import dev.martianzoo.agent.exMachina
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.GameRecording
import dev.martianzoo.engine.World
import dev.martianzoo.engine.recording
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
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
  internal lateinit var agents: Map<Actor, Agent>
    private set

  protected val admin: TfmGameplay
    get() = game.tfm(agents, Actor.ADMIN)

  /** Returns gameplay for the Player occupying the one-based [seat]. */
  protected fun player(seat: Int): TfmGameplay {
    require(seat > 0) { "seat numbers begin at 1" }
    val player = game.actors.filterIsInstance<Player>().getOrNull(seat - 1)
    requireNotNull(player) { "no Player occupies seat $seat" }
    return game.tfm(agents, player)
  }

  protected abstract val config: GameConfig
  /** Pets declarations for concrete Players with sourced per-seat setup rules. */
  protected open val playerClassPets: String = ""
  protected open val catalog: TfmCatalog by lazy {
    if (cn("FakeStuffBundle") in config.includedClassNames) {
      TfmCatalog.compose(Canon, FakeCanon)
    } else {
      Canon
    }
  }

  public fun record(): GameRecording = record({}, {})

  internal fun record(
      onGameConstructed: () -> Unit,
      onReplayCompleted: () -> Unit,
  ): GameRecording {
    val premise = catalog.gamePremise(config, parseClasses(playerClassPets))
    game = Engine.newGame(premise)
    agents = createAgents(game)
    onGameConstructed()
    play()
    onReplayCompleted()
    return game.recording()
  }

  protected abstract fun play()

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

  protected fun TfmGameplay.exMachina(adjustment: String) {
    game.exMachina(agents, this, adjustment)
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
      task.instruction == agents.getValue(task.assignee).parse<Instruction>(instruction) &&
          (NoOp.narrows(task.instruction, reader) ||
              task.instruction.descendantsOfType<NoOp>().isNotEmpty())
    }
    require(matches.size == 1)
    return matches.single().id
  }

  private fun TfmGameplay.pendingTasks(): List<Task> =
      game.tasks.extract { it }.filter { it.assignee == actor }
}
