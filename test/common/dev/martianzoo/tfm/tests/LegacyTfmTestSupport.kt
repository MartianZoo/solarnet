package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgents as retainedTestAgents
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.canon.TfmClasses.TILE
import dev.martianzoo.tfm.canon.cardResourceType
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.state.ApiUtils.mapDefinition
import dev.martianzoo.tfm.state.tfmCatalog

/** Existing helper-based scenarios only; explicit replays use plain Agents. */
internal interface LegacyTfmTestSupport {
  val game: World
  val agents: Agents

  // Keep calls such as game.testTfm(player) on this fixture's recorded Agents.
  fun World.testAgents(): Agents = if (this === game) agents else retainedTestAgents()

  fun World.testAgent(actor: Actor): Agent = testAgents()[actor]

  fun World.testTfm(actor: Actor): TfmGameplay = testAgents().tfm(actor)

  fun keepStartingProjects(vararg counts: Int) {
    val players = game.actors.filterIsInstance<dev.martianzoo.state.Player>()
    require(counts.size == players.size) { "Provide a project count per player" }
    players.zip(counts.toList()).forEach { (player, count) ->
      agents.tfm(player).keepStartingProjects(count)
    }
  }

  fun <T> OperationScope.doWithoutAutoExec(
      agent: Agent,
      body: OperationScope.() -> T,
  ): T {
    val previousAutoExecPolicy = agent.autoExecPolicy
    if (agent.actor != ADMIN) agent.autoExecPolicy = NONE
    return try {
      body()
    } finally {
      agent.autoExecPolicy = previousAutoExecPolicy
    }
  }

  fun Agent.placeTile(row: Int, column: Int): TaskResult =
      doTask(tilePlacement(reader, pendingTasks(), row, column))

  fun OperationScope.placeTile(row: Int, column: Int) {
    doTask(tilePlacement(reader, tasks.extract { it }, row, column))
  }

  fun Agent.addCardResources(card: ClassName, count: Int? = null): TaskResult =
      doTask(cardResources(reader, pendingTasks(), card, count))

  fun OperationScope.addCardResources(card: ClassName, count: Int? = null) {
    doTask(cardResources(reader, tasks.extract { it }, card, count))
  }

  fun Agent.wgt(choice: String): TaskResult = doTask("$choice! BY Admin")

  fun OperationScope.wgt(choice: String) {
    doTask("$choice! BY Admin")
  }

  /**
   * Declines this actor's offered task by passing the baton when offered, as final greenery does,
   * and otherwise with `Ok`.
   */
  fun Agent.declineTask(): TaskResult {
    val batonPass =
        pendingTasks().firstNotNullOfOrNull { task ->
          task.instruction
              .descendantsOfType<Instruction.Gain>()
              .firstOrNull { it.gaining.className == cn("PassBaton") }
              ?.let { task.copy(instruction = it) }
        }
    return if (batonPass != null) selectTask(batonPass) else doTask("Ok")
  }

  fun Agent.declineTask(instruction: String): TaskResult {
    val taskId = singleDeclinableTaskId(pendingTasks(), reader, instruction)
    return doTask("Ok", taskId)
  }

  fun OperationScope.declineTask() {
    doTask("Ok")
  }

  fun OperationScope.declineTask(instruction: String) {
    val taskId = singleDeclinableTaskId(tasks.extract { it }, reader, instruction)
    doTask("Ok", taskId)
  }

  fun TfmGameplay.playCorp(
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
      reader: dev.martianzoo.state.GameReader,
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
      reader: dev.martianzoo.state.GameReader,
      tasks: List<Task>,
      card: ClassName,
      count: Int?,
  ): String {
    require(count == null || count > 0) { "Card-resource count must be positive" }
    val resourceType =
        requireNotNull(cardResourceType(reader.tfmCatalog.card(card))) {
          "$card does not hold card resources"
        }
    val resourceClass = reader.resolve(resourceType.expression).rootClass
    val cardClass = reader.resolve(card.expression).rootClass
    val gain =
        tasks
            .flatMap { it.instruction.descendantsOfType<Gain>() }
            .single {
              val gaining = reader.resolve(it.gaining)
              (count == null || it.count == ActualScalar(count)) &&
                  resourceClass.isSubtypeOf(gaining.rootClass) &&
                  cardClass.isSubtypeOf(gaining.typeDependencies.last().boundType.rootClass)
            }
    val arguments = gain.gaining.arguments.toMutableList()
    if (arguments.isEmpty()) arguments += card.expression
    else arguments[arguments.lastIndex] = card.expression
    val revisedExpression = Expression(resourceType, arguments, argumentsSpecified = true)
    return "$gain".replace("${gain.gaining}", "$revisedExpression").removeSuffix("?")
  }

  private fun singleDeclinableTaskId(
      tasks: List<Task>,
      reader: dev.martianzoo.state.GameReader,
      instruction: String,
  ): TaskId {
    val matches = tasks.filter { task ->
      task.instruction == agents[task.assignee].parse<Instruction>(instruction) &&
          (NoOp.narrows(task.instruction, reader) ||
              task.instruction.descendantsOfType<NoOp>().isNotEmpty())
    }
    require(matches.size == 1) {
      "Expected exactly one task narrowable to Ok matching `$instruction`, found ${matches.size}"
    }
    return matches.single().id
  }

  private fun Agent.pendingTasks(): List<Task> =
      game.tasks.extract { it }.filter { it.assignee == actor }
}
