package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Gameplay.OperationLayer
import dev.martianzoo.engine.Timeline
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.ApiUtils.getPlayerOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Two modes for driving the Terraforming Mars game-phase sequence: [Auto] uses a coroutine to
 * advance phases automatically; [Manual] exposes each phase transition as an explicit public method
 * for tests that need to drive the game step-by-step.
 */
public object TfmWorkflow {

  /**
   * Exposes each game-phase transition as a simple method call. No coroutine machinery: each method
   * fires the engine op and returns immediately. The caller is then responsible for performing all
   * resulting player actions before calling the next phase method.
   *
   * Player action helpers ([TfmGameplay.playProject] etc.) self-grant turns via
   * [OperationLayer.inTurn] when no task is already pending, so no explicit turn-granting is
   * needed.
   */
  public class Manual(private val game: World) {

    internal val engineOps: OperationLayer = game.gameplay(ENGINE) as OperationLayer

    /**
     * Starts fully effectful game setup; unlike later phases, setup has no prior Phase to remove.
     */
    public fun setupPhase(): TaskResult = engineOps.beginManual("SetupPhase")

    public fun corporationPhase(): TaskResult = engineOps.manual("CorporationPhase FROM Phase")

    public fun preludePhase(): TaskResult = engineOps.manual("PreludePhase FROM Phase")

    public fun actionPhase(): TaskResult = engineOps.manual("ActionPhase FROM Phase")

    public fun productionPhase(): TaskResult = engineOps.manual("ProductionPhase FROM Phase")

    /** Enters the universal Solar phase unless the game ended after production. */
    public fun solarPhase(): TaskResult? =
        if (engineOps.has("LastCall")) null else engineOps.beginManual("SolarPhase FROM Phase")

    public fun generation(): TaskResult = engineOps.beginManual("Generation")

    public fun finalGreeneryPhase(): TaskResult = engineOps.manual("FinalGreeneryPhase FROM Phase")

    public fun researchPhase(body: BodyLambda = {}): TaskResult =
        engineOps.manual("ResearchPhase FROM Phase", body)

    public fun endPhase(): TaskResult = engineOps.manual("EndPhase FROM Phase")
  }

  /**
   * Orchestrates the full Terraforming Mars game flow using a single coroutine, so each phase can
   * be written as straight-line sequential code.
   *
   * The coroutine suspends whenever the game has outstanding tasks (choosing cards, placing tiles,
   * etc.), and resumes once the task queue drains. Synchronization uses [resumeSignal], a
   * [Channel.RENDEZVOUS] channel: [Channel.trySend] only succeeds when a [Channel.receive] is
   * already waiting, so signals fired during automatic engine-owned phases are dropped rather than
   * queued, preventing spurious wakeups.
   */
  public class Auto(private val game: World) {

    private val m = Manual(game)
    private val engineOps: OperationLayer
      get() = m.engineOps

    /** Human players in seat order, excluding ENGINE. */
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

    internal val isRunning: Boolean
      get() = workflowJob?.isActive == true

    /**
     * Checkpoint saved just before the workflow's most recent [OperationLayer.beginManual] call.
     * Non-null only while the coroutine is suspended waiting for those tasks to drain. [shutdown]
     * rolls back to this point to undo the pending workflow task.
     */
    private var shutdownCheckpoint: Timeline.Checkpoint? = null

    init {
      game.onAtomicComplete = { if (game.isIdle()) resumeSignal.trySend(Unit) }
    }

    /**
     * Launches the game-flow coroutine and returns `this` for chaining. An [Auto] instance can be
     * launched only once.
     *
     * [Dispatchers.Unconfined] is used so the coroutine resumes synchronously in whichever thread
     * delivers the next [resumeSignal], avoiding unnecessary thread hops.
     */
    public fun launch(): Auto {
      check(workflowJob == null) { "Workflow has already been launched" }
      workflowJob =
          workflowScope.launch(start = CoroutineStart.LAZY) {
            try {
              runGame()
            } finally {
              game.onAtomicComplete = {}
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
    internal fun shutdown() {
      game.onAtomicComplete = {}
      lifecycleJob.cancel()
      resumeSignal.cancel()
      shutdownCheckpoint?.let { game.timeline.rollBack(it) }
      shutdownCheckpoint = null
    }

    /** Orchestrates the complete game from its committed pre-setup baseline to finish. */
    private suspend fun runGame() {
      m.setupPhase()
      awaitTasksDrained()
      corporationPhase()
      if (hasComponent("PreludeExpansion")) preludePhase()
      while (true) {
        if (engineOps.count("Generation") > 1) researchPhase()
        actionPhase()
        productionPhase()
        if (!solarPhase()) break
        generation()
      }
      if (hasComponent("SoloMode")) {
        engineOps.manual("SoloVictoryCheck")
        if (!engineOps.has("Victory<${players.single()}>")) return
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
      engineOps.beginManual("ProductionPhase FROM Phase")
      letPlayerFinish()
    }

    private suspend fun solarPhase(): Boolean {
      if (m.solarPhase() == null) return false
      letPlayerFinish()
      return true
    }

    private suspend fun generation() {
      m.generation()
      letPlayerFinish()
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
      engineOps.beginManual("ResearchPhase FROM Phase")
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
      val firstPlayer = getPlayerOwner(game.reader, token)
      val firstPlayerIndex = players.indexOf(firstPlayer)
      return players.drop(firstPlayerIndex) + players.take(firstPlayerIndex)
    }

    private fun opsFor(player: Player) = game.gameplay(player) as OperationLayer

    private fun hasPassed(player: Player) = opsFor(player).has("Pass")

    private fun hasComponent(className: String): Boolean =
        game.classTable.isActive(cn(className)) && game.reader.getComponents(className).isNotEmpty()

    private suspend fun grantFirstActionTo(player: Player) {
      shutdownCheckpoint = game.timeline.checkpoint()
      opsFor(player).beginManual("NewTurn!")
      if (!game.isIdle()) resumeSignal.receive()
      shutdownCheckpoint = null
    }

    private suspend fun grantSecondActionTo(player: Player) {
      shutdownCheckpoint = game.timeline.checkpoint()
      opsFor(player).beginManual("SecondAction")
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
