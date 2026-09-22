package dev.martianzoo.tfm.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.Player
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.TaskResult
import dev.martianzoo.state.toComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Two ways to coordinate Terraforming Mars phases: [Automatic] sequences their remaining player
 * work while Pets scopes advance the workflow; [Stepwise] exposes explicit phase transitions for
 * tests that need to drive the game step-by-step.
 */
public object TfmWorkflow {

  /**
   * Exposes each game-phase transition as a simple method call. No coroutine machinery: each method
   * fires the Admin operation and returns immediately. The caller is then responsible for
   * performing all resulting player actions before calling the next phase method.
   *
   * Player action helpers ([TfmGameplay.playProject] etc.) self-grant turns via [Agent.inTurn] when
   * no task is already pending, so no explicit turn-granting is needed.
   */
  public class Stepwise(agents: Agents) {

    internal val adminOps: Agent = agents[ADMIN]

    /** Starts fully effectful game setup by replacing the initial bootstrap phase. */
    public fun setupPhase(): TaskResult = adminOps.beginOperation("SetupPhase FROM Phase")

    public fun corporationPhase(): TaskResult = adminOps.runOperation("CorporationPhase FROM Phase")

    public fun preludePhase(): TaskResult = adminOps.runOperation("PreludePhase FROM Phase")

    public fun actionPhase(): TaskResult = adminOps.runOperation("ActionPhase FROM Phase")

    public fun productionPhase(): TaskResult = adminOps.runOperation("ProductionPhase FROM Phase")

    /** Enters the universal Solar phase unless the game ended after production. */
    public fun solarPhase(): TaskResult? =
        if (adminOps.has("GameEndBarrier")) adminOps.beginOperation("SolarPhase FROM Phase")
        else null

    /** Enters World Government Terraforming, the first expansion-specific Solar phase. */
    public fun venusSolarPhase(): TaskResult = adminOps.beginOperation("VenusSolarPhase FROM Phase")

    /** Enters colony-track production, after World Government Terraforming when both are active. */
    public fun coloniesSolarPhase(): TaskResult =
        adminOps.beginOperation("ColoniesSolarPhase FROM Phase")

    /** Enters the Turmoil operation after every other active expansion-specific Solar phase. */
    public fun turmoilSolarPhase(): TaskResult =
        adminOps.beginOperation("TurmoilSolarPhase FROM Phase")

    public fun finalGreeneryPhase(): TaskResult =
        adminOps.runOperation("FinalGreeneryPhase FROM Phase")

    public fun researchPhase(body: OperationBlock = {}): TaskResult =
        adminOps.runOperation("ResearchPhase FROM Phase", body)

    public fun endPhase(): TaskResult = adminOps.runOperation("End FROM Phase")
  }

  /**
   * Coordinates the full Terraforming Mars game flow using a single coroutine, so each phase's
   * player work can be written as straight-line sequential code. Pets scopes own phase transitions.
   *
   * The coroutine suspends whenever the game has outstanding tasks (choosing cards, placing tiles,
   * etc.), and resumes once the task queue drains. Synchronization uses [resumeSignal], a
   * [Channel.RENDEZVOUS] channel: [Channel.trySend] only succeeds when a [Channel.receive] is
   * already waiting, so signals fired during automatic Admin-controlled phases are dropped rather
   * than queued, preventing spurious wakeups.
   */
  public class Automatic(private val agents: Agents) {

    private val game: World = agents.world
    private val m = Stepwise(agents)
    private val adminOps: Agent
      get() = m.adminOps

    /** Human players in seat order, excluding ADMIN. */
    private val players: List<Player> = game.actors.filterIsInstance<Player>()

    /**
     * RENDEZVOUS channel that signals the workflow coroutine to resume after all tasks drain. Only
     * fires when [Channel.receive] is already waiting, so signals during automatic phases are
     * silently dropped.
     */
    private val resumeSignal = Channel<Unit>(Channel.RENDEZVOUS)

    /** Parent job for all work owned by this workflow. */
    private val lifecycleJob = Job()
    private val workflowScope = CoroutineScope(lifecycleJob + Dispatchers.Unconfined)
    private var workflowJob: Job? = null

    // TODO: Contract temporary tfm-tests lifecycle seams.
    public val isRunning: Boolean
      get() = workflowJob?.isActive == true

    /**
     * Checkpoint saved just before the workflow's most recent [Agent.beginOperation] call. Non-null
     * only while the coroutine is suspended waiting for those tasks to drain. [shutdown] rolls back
     * to this point to undo the pending workflow task.
     */
    private var shutdownCheckpoint: Checkpoint? = null

    init {
      game.onTransactionComplete = { if (game.isIdle()) resumeSignal.trySend(Unit) }
    }

    /**
     * Launches the game-flow coroutine and returns `this` for chaining. An [Automatic] instance can
     * be launched only once.
     *
     * [Dispatchers.Unconfined] is used so the coroutine resumes synchronously in whichever thread
     * delivers the next [resumeSignal], avoiding unnecessary thread hops.
     */
    public fun launch(): Automatic {
      check(workflowJob == null) { "Workflow has already been launched" }
      workflowJob =
          workflowScope.launch(start = CoroutineStart.LAZY) {
            try {
              runGame()
            } finally {
              game.onTransactionComplete = {}
            }
          }
      workflowJob!!.start()
      return this
    }

    /**
     * Stops the workflow cleanly and cancels its coroutine. If the coroutine is suspended waiting
     * for a player to handle a workflow-created task (NewTurn or SecondAction), that task is rolled
     * back so the queue is empty and the game is ready for a manual phase transition.
     */
    public fun shutdown() {
      game.onTransactionComplete = {}
      lifecycleJob.cancel()
      resumeSignal.cancel()
      shutdownCheckpoint?.let { game.timeline.rollBack(it) }
      shutdownCheckpoint = null
      if (adminOps.has("WorkflowStarted")) adminOps.sneak("-WorkflowStarted")
    }

    /** Coordinates the complete game from its committed bootstrap state to finish. */
    private suspend fun runGame() {
      adminOps.beginOperation("WorkflowStarted")
      awaitTasksDrained()
      corporationPhase()
      adminOps.runOperation("-CorporationPhaseScope")
      completePreludePhase()
      while (hasComponent("ActionPhase")) actionPhase()
      if (!hasComponent("FinalGreeneryPhase")) return
      finalGreeneryPhase()
    }

    private suspend fun corporationPhase() {
      for (player in players) grantFirstActionTo(player)
    }

    private suspend fun completePreludePhase() {
      for (player in players) {
        // The retained cards are the setup fact; custom and replay setups need not retain two.
        repeat(opsFor(player).count("PreludeCard")) {
          grantFirstActionTo(player)
          if (!hasComponent("PreludePhase")) return
        }
      }
    }

    private suspend fun finalGreeneryPhase() {
      for (player in rotatedByFirstPlayer()) {
        var placedGreenery: Boolean
        do {
          val greeneryCount = opsFor(player).count("GreeneryTile<$player>")
          grantFirstActionTo(player)
          placedGreenery = opsFor(player).count("GreeneryTile<$player>") > greeneryCount
        } while (placedGreenery)
      }
      adminOps.runOperation("-FinalGreeneryPhaseScope")
    }

    private suspend fun actionPhase() {
      val generation = adminOps.count("Generation")
      val active = ArrayDeque(rotatedByFirstPlayer())
      while (active.isNotEmpty()) {
        val player = active.first()
        grantFirstActionTo(player)
        if (actionPhaseEnded(generation)) return
        if (hasPassed(player)) {
          active.removeFirst()
        } else {
          if (active.size > 1) {
            grantSecondActionTo(player)
            if (actionPhaseEnded(generation)) return
          }
          // SecondAction cannot choose Pass, but an action such as Red Appeasement can grant it.
          if (hasPassed(player)) active.removeFirst() else active.addLast(active.removeFirst())
        }
      }
    }

    private fun actionPhaseEnded(generation: Int): Boolean =
        !hasComponent("ActionPhase") || adminOps.count("Generation") != generation

    private fun rotatedByFirstPlayer(): List<Player> {
      val token = game.reader.getComponents("StartToken").single()
      val ownerName = token.toComponent().owner?.className
      val firstPlayer = players.single { it.className == ownerName }
      val firstPlayerIndex = players.indexOf(firstPlayer)
      return players.drop(firstPlayerIndex) + players.take(firstPlayerIndex)
    }

    private fun opsFor(player: Player) = agents[player]

    private fun hasPassed(player: Player) = opsFor(player).has("Pass")

    private fun hasComponent(className: String): Boolean =
        game.classTable.isInhabited(cn(className)) &&
            game.reader.getComponents(className).isNotEmpty()

    private suspend fun grantFirstActionTo(player: Player) {
      shutdownCheckpoint = game.timeline.checkpoint()
      opsFor(player).beginOperation("NewTurn!")
      if (!game.isIdle()) resumeSignal.receive()
      shutdownCheckpoint = null
    }

    private suspend fun grantSecondActionTo(player: Player) {
      shutdownCheckpoint = game.timeline.checkpoint()
      opsFor(player).beginOperation("SecondAction")
      if (!game.isIdle()) resumeSignal.receive()
      shutdownCheckpoint = null
    }

    private suspend fun awaitTasksDrained() {
      game.timeline.commit()
      if (!game.isIdle()) resumeSignal.receive()
    }
  }
}
