package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.agenttestsupport.testAgents as retainedTestAgents
import dev.martianzoo.engine.World
import dev.martianzoo.generated.ActionCard
import dev.martianzoo.generated.CardFront
import dev.martianzoo.generated.Class as PetsClass
import dev.martianzoo.generated.CorporationCard
import dev.martianzoo.generated.GlobalParameter
import dev.martianzoo.generated.Milestone
import dev.martianzoo.generated.Owned
import dev.martianzoo.generated.Player
import dev.martianzoo.generated.PreludeCard
import dev.martianzoo.generated.ProjectCard
import dev.martianzoo.generated.ResourceCard
import dev.martianzoo.generated.StandardAction
import dev.martianzoo.generated.StandardProject
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition
import dev.martianzoo.tfm.canon.TfmClasses.TILE
import dev.martianzoo.tfm.canon.cardResourceType
import dev.martianzoo.tfm.canon.tfmCatalog
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

internal abstract class TfmTest {
  protected var game: World
    get() = agents.world
    set(value) {
      agents = Agents(value)
    }

  protected lateinit var agents: Agents
    private set

  protected fun World.testAgents(): Agents =
      if (this@TfmTest::agents.isInitialized && this === agents.world) agents
      else retainedTestAgents()

  protected fun World.testAgent(actor: Actor): dev.martianzoo.agent.Agent = testAgents()[actor]

  protected fun <A : Actor> World.testTfm(actor: A): TfmGameplay<A> = testAgents().tfm(actor)

  protected val admin: TfmGameplay<Actor>
    get() = agents.tfm(ADMIN)

  protected fun TaskResult.expect(string: String) = TestHelpers.assertNetChanges(this, game, string)

  protected fun <T> OperationScope.doWithoutAutoExec(
      agent: TfmGameplay<*>,
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

  protected fun TfmGameplay<*>.placeTile(row: Int, column: Int): TaskResult =
      doTask(tilePlacement(reader, pendingTasks(), row, column))

  protected fun OperationScope.placeTile(row: Int, column: Int) {
    doTask(tilePlacement(reader, tasks.extract { it }, row, column))
  }

  protected fun TfmGameplay<*>.addCardResources(card: ClassName, count: Int? = null): TaskResult =
      doTask(cardResources(reader, pendingTasks(), card, count))

  protected fun OperationScope.addCardResources(card: ClassName, count: Int? = null) {
    doTask(cardResources(reader, tasks.extract { it }, card, count))
  }

  protected fun <P : Player> TfmGameplay<P>.addCardResources(
      card: ResourceCard<P, *, *>,
      count: Int? = null,
  ): TaskResult = addCardResources(card.expression.className, count)

  protected fun <P : Player> TypedOperationBody<P>.addCardResources(
      card: ResourceCard<P, *, *>,
      count: Int? = null,
  ) {
    addCardResources(card.expression.className, count)
  }

  protected fun TfmGameplay<*>.wgt(choice: String): TaskResult = doTask("$choice! BY Admin")

  protected fun TfmGameplay<*>.wgt(choice: GlobalParameter): TaskResult = wgt(choice.toString())

  protected fun OperationScope.wgt(choice: String) {
    doTask("$choice! BY Admin")
  }

  protected fun OperationScope.wgt(choice: GlobalParameter) {
    wgt(choice.toString())
  }

  protected fun <P : Player> TfmGameplay<P>.stdAction(
      action: StandardAction,
      which: Int = 1,
      body: TypedOperationBody<P>.() -> Unit = {},
  ): TaskResult = stdAction(action.toString(), which, body = typedBody(body))

  protected fun <P : Player> TfmGameplay<P>.stdProject(
      project: StandardProject,
      body: TypedOperationBody<P>.() -> Unit = {},
  ): TaskResult = stdProject(project.toString(), body = typedBody(body))

  protected fun TfmGameplay<*>.claimMilestone(milestone: PetsClass.Root<Milestone<*>>): TaskResult =
      claimMilestone(milestone.name)

  protected fun <P : Player> TypedOperationBody<P>.doTask(component: Owned<P>) {
    doTask(component.toString())
  }

  protected fun <P : Player> TfmGameplay<P>.playPrelude(
      card: CardFront<P, PetsClass<PreludeCard<*, *>>>,
      body: TypedOperationBody<P>.() -> Unit = {},
  ): TaskResult = playPrelude(card.expression.className, typedBody(body))

  protected fun <P : Player> TfmGameplay<P>.playProject(
      card: CardFront<P, PetsClass<ProjectCard<*, *>>>,
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      body: TypedOperationBody<P>.() -> Unit = {},
  ): TaskResult =
      playProject(
          card.expression.className,
          megacredits,
          steel,
          titanium,
          body = typedBody(body),
      )

  protected fun <P : Player> TypedOperationBody<P>.playProject(
      card: CardFront<P, PetsClass<ProjectCard<*, *>>>,
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      body: TypedOperationBody<P>.() -> Unit = {},
  ) {
    with(gameplay) {
      this@playProject.playProject(
          card.expression.className,
          megacredits,
          steel,
          titanium,
          body = typedBody(body),
      )
    }
  }

  protected fun <P : Player> TfmGameplay<P>.cardAction1(
      card: ActionCard<P, *>,
      body: TypedOperationBody<P>.() -> Unit = {},
  ): TaskResult = cardAction1(card.expression.className, typedBody(body))

  protected fun <P : Player> TfmGameplay<P>.cardAction1(
      card: ActionCard<P, *>,
      x: Int,
      body: TypedOperationBody<P>.() -> Unit = {},
  ): TaskResult = cardAction1(card.expression.className, x, typedBody(body))

  protected fun <P : Player> TfmGameplay<P>.cardAction2(
      card: ActionCard<P, *>,
      body: TypedOperationBody<P>.() -> Unit = {},
  ): TaskResult = cardAction2(card.expression.className, typedBody(body))

  protected fun <P : Player> TfmGameplay<P>.cardAction2(
      card: ActionCard<P, *>,
      x: Int,
      body: TypedOperationBody<P>.() -> Unit = {},
  ): TaskResult = cardAction2(card.expression.className, x, typedBody(body))

  protected fun <P : Player> TypedOperationBody<P>.cardAction1(
      card: ActionCard<P, *>,
      body: TypedOperationBody<P>.() -> Unit = {},
  ) {
    with(gameplay) {
      this@cardAction1.cardAction1(card.expression.className, typedBody(body))
    }
  }

  protected fun <P : Player> TypedOperationBody<P>.cardAction1(
      card: ActionCard<P, *>,
      x: Int,
      body: TypedOperationBody<P>.() -> Unit = {},
  ) {
    with(gameplay) {
      this@cardAction1.cardAction1(card.expression.className, x, typedBody(body))
    }
  }

  protected fun <P : Player> TypedOperationBody<P>.cardAction2(
      card: ActionCard<P, *>,
      body: TypedOperationBody<P>.() -> Unit = {},
  ) {
    with(gameplay) {
      this@cardAction2.cardAction2(card.expression.className, typedBody(body))
    }
  }

  protected fun TfmGameplay<*>.declineTask(): TaskResult {
    return doTask("Ok")
  }

  protected fun TfmGameplay<*>.declineTask(instruction: String): TaskResult {
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

  protected fun <A : HasExpression> TfmGameplay<A>.playCorp(
      cardName: ClassName,
      body: TfmGameplay<A>.() -> Unit = {},
  ): TaskResult {
    val player = this
    return inTurn {
      playCorp(cardName)
      player.body()
    }
  }

  protected fun <P : Player> TfmGameplay<P>.playCorp(
      card: CardFront<P, PetsClass<CorporationCard<*, *>>>,
      body: TfmGameplay<P>.() -> Unit,
  ): TaskResult = playCorp(card.expression.className, body)

  protected fun <P : Player> TfmGameplay<P>.playCorp(
      card: CardFront<P, PetsClass<CorporationCard<*, *>>>,
      buyCards: Int,
      body: TypedOperationBody<P>.() -> Unit = {},
  ): TaskResult = playCorp(card.expression.className, buyCards, typedBody(body))

  private fun <P : Player> TfmGameplay<P>.typedBody(
      body: TypedOperationBody<P>.() -> Unit
  ): OperationBlock = {
    val operation = this
    val gameplay = this@typedBody
    object : TypedOperationBody<P>, OperationScope by operation {
          override val gameplay: TfmGameplay<P> = gameplay
        }
        .body()
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
                  reader
                      .resolve(resourceType.expression)
                      .rootClass
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

  private fun TfmGameplay<*>.pendingTasks(): List<Task> =
      game.tasks.extract { it }.filter { it.assignee == actor }
}
