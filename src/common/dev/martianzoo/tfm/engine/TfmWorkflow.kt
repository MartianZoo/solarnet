package dev.martianzoo.tfm.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.engine.Timeline
import dev.martianzoo.engine.World
import dev.martianzoo.engine.toComponent
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.TaskResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Two ways to drive the Terraforming Mars game-phase sequence: [Automatic] uses a coroutine to
 * advance phases automatically; [Stepwise] exposes each phase transition as an explicit public
 * method for tests that need to drive the game step-by-step.
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
  public class Stepwise(agents: Map<Actor, Agent>) {

    internal val adminOps: Agent = agents.getValue(ADMIN)

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

    public fun finalGreeneryPhase(): TaskResult =
        adminOps.runOperation("FinalGreeneryPhase FROM Phase")

    public fun researchPhase(body: OperationBlock = {}): TaskResult =
        adminOps.runOperation("ResearchPhase FROM Phase", body)

    public fun endPhase(): TaskResult = adminOps.runOperation("End FROM Phase")
  }

  /**
   * Orchestrates the full Terraforming Mars game flow using a single coroutine, so each phase can
   * be written as straight-line sequential code.
   *
   * The coroutine suspends whenever the game has outstanding tasks (choosing cards, placing tiles,
   * etc.), and resumes once the task queue drains. Synchronization uses [resumeSignal], a
   * [Channel.RENDEZVOUS] channel: [Channel.trySend] only succeeds when a [Channel.receive] is
   * already waiting, so signals fired during automatic Admin-controlled phases are dropped rather
   * than queued, preventing spurious wakeups.
   */
  public class Automatic(
      private val game: World,
      private val agents: Map<Actor, Agent>,
  ) {

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
    private var shutdownCheckpoint: Timeline.Checkpoint? = null

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
    }

    /** Orchestrates the complete game from its committed bootstrap state to finish. */
    private suspend fun runGame() {
      m.setupPhase()
      awaitTasksDrained()
      corporationPhase()
      if (hasComponent("PreludeExpansion")) preludePhase()
      while (true) {
        actionPhase()
        productionPhase()
        if (!solarPhase()) break
        researchPhase()
      }
      if (hasComponent("SoloMode")) {
        if (!adminOps.has("Victory<${players.single()}>")) return
      }
      finalGreeneryPhase()
      m.endPhase()
    }

    private suspend fun corporationPhase() {
      m.corporationPhase()
      for (player in players) grantFirstActionTo(player)
    }

    // TODO: This is slightly inconsistent with the action-phase turn model; revisit.
    private suspend fun preludePhase() {
      m.preludePhase()
      for (player in players) {
        grantFirstActionTo(player)
        grantFirstActionTo(player)
      }
    }

    private suspend fun productionPhase() {
      adminOps.beginOperation("ProductionPhase FROM Phase")
      letPlayerFinish()
    }

    private suspend fun solarPhase(): Boolean {
      if (m.solarPhase() == null) {
        adminOps.runOperation("CheckGameEnd")
        awaitTasksDrained()
        return false
      }
      letPlayerFinish()
      return true
    }

    private suspend fun finalGreeneryPhase() {
      m.finalGreeneryPhase()
      for (player in rotatedByFirstPlayer()) {
        var placedGreenery: Boolean
        do {
          val greeneryCount = opsFor(player).count("GreeneryTile<$player>")
          grantFirstActionTo(player)
          placedGreenery = opsFor(player).count("GreeneryTile<$player>") > greeneryCount
        } while (placedGreenery)
      }
    }

    private suspend fun researchPhase() {
      adminOps.beginOperation("ResearchPhase FROM Phase")
      letPlayerFinish()
    }

    private suspend fun actionPhase() {
      m.actionPhase()
      val active = ArrayDeque(rotatedByFirstPlayer())
      while (active.isNotEmpty()) {
        val player = active.first()
        grantFirstActionTo(player)
        if (hasPassed(player)) {
          active.removeFirst()
        } else {
          if (active.size > 1) grantSecondActionTo(player)
          active.addLast(active.removeFirst())
        }
      }
    }

    private fun rotatedByFirstPlayer(): List<Player> {
      val token = game.reader.getComponents("StartToken").single()
      val ownerName = token.toComponent().owner?.className
      val firstPlayer = players.single { it.className == ownerName }
      val firstPlayerIndex = players.indexOf(firstPlayer)
      return players.drop(firstPlayerIndex) + players.take(firstPlayerIndex)
    }

    private fun opsFor(player: Player) = agents.getValue(player)

    private fun hasPassed(player: Player) = opsFor(player).has("Pass")

    private fun hasComponent(className: String): Boolean =
        game.classTable.isActive(cn(className)) && game.reader.getComponents(className).isNotEmpty()

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

    private suspend fun letPlayerFinish() = awaitTasksDrained()
  }
}
