package dev.martianzoo.agent

import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Task

/**
 * A [World] together with the Agents that act on it: exactly one stable [Agent] per Actor, sharing
 * one autoexecution loop. Clients hold this rather than a World and a separate collection of
 * Agents, so an Agent can never be paired with a World it does not act on.
 *
 * [safeTask] is the game's proof that performing a concrete pending task now preserves every
 * completed outcome and Player choice; the [AutoExecPolicy.SAFE] policy performs only tasks it
 * accepts, besides a sole ordinary task. It receives the current World rather than closing over
 * [world], because forks and overlays reuse it. [initialPolicy] supplies each Actor's initial
 * policy, defaulting to [AutoExecPolicy.EAGER]; it is evaluated once per Actor. Construction does
 * not execute pending tasks.
 */
public open class Agents(
    public val world: World,
    private val safeTask: (World, Task) -> Boolean = { _, _ -> false },
    initialPolicy: (Actor) -> AutoExecPolicy = { AutoExecPolicy.EAGER },
) {
  /** Opt-in task recording, including Player autoexecution; see [TaskLog] for supported scope. */
  public val taskLog: TaskLog = TaskLog(world)

  private val autoExecLoop = AutoExecLoop(world, taskLog, safeTask)
  private val elaborator = PetElaborator(world.classTable)

  private val agents: Map<Actor, Agent> =
      world.actors.associateWith {
        AgentImpl(
            world,
            world.actorEngine(it),
            elaborator,
            autoExecLoop,
            taskLog,
            initialPolicy(it),
        )
      }

  /** This world's Agent for [actor]. */
  public operator fun get(actor: Actor): Agent = agents.getValue(actor)

  /**
   * A speculative session with fresh Agents and the same current per-Actor policies. The source
   * World stays unchanged; mutating it later invalidates the speculative World. Agent transaction
   * guards are absent, so card movements can remain anonymous. Recording and completion callbacks
   * begin fresh. The returned session can be reset and reused without rebuilding its Agents.
   */
  public fun overlay(): SpeculativeAgents =
      SpeculativeAgents(Engine.overlay(world), safeTask) { actor -> get(actor).autoExecPolicy }

  /**
   * A fully independent copy, including this World's complete event history. This also materializes
   * a speculative World's event suffix into the new World's ordinary history. Agent transaction
   * guards are not copied, so card movements in the copy can remain anonymous.
   */
  public fun fork(): Agents = withWorld(Engine.fork(world))

  private fun withWorld(otherWorld: World): Agents =
      Agents(otherWorld, safeTask) { actor -> get(actor).autoExecPolicy }
}
