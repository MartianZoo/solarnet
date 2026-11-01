package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.agenttestsupport.testAgents as retainedTestAgents
import dev.martianzoo.engine.World
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

internal abstract class TfmTest : LegacyTfmTestSupport {
  final override var game: World
    get() = agents.world
    set(value) {
      agents = Agents(value)
    }

  final override lateinit var agents: Agents
    private set

  override fun World.testAgents(): Agents =
      if (this@TfmTest::agents.isInitialized && this === agents.world) agents
      else retainedTestAgents()

  // These member extensions must shadow imported helpers and retain this fixture's Agents.
  override fun World.testAgent(actor: Actor): dev.martianzoo.agent.Agent = testAgents()[actor]

  override fun World.testTfm(actor: Actor): TfmGameplay = testAgents().tfm(actor)

  protected val admin: TfmGameplay
    get() = agents.tfm(ADMIN)

  protected fun TaskResult.expect(string: String) = TestHelpers.assertNetChanges(this, game, string)

  protected fun Agent.auditGainsSince(eventCursor: Int): Int =
      game.events.changesSince(eventCursor).count { it.change.gaining?.type == resolve("Audit") }
}
