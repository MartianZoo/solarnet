package dev.martianzoo.agent

import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.state.Actor

/**
 * A [World] together with a stable [Agent] for each Actor. Clients hold this rather than a World
 * and a separate collection of Agents, so an Agent is always paired with the World it acts on.
 * Calls through these Agents coordinate their changes and subsequent task handling.
 *
 * [initialPolicy] supplies each Actor's initial automatic-execution policy, defaulting to
 * [AutoExecPolicy.EAGER]. It is evaluated once per Actor. Construction does not execute pending
 * tasks; subsequent Agent commands use the supplied policies.
 */
public open class Agents(
    public val world: World,
    // TODO(ENGINE_CONTRACT): use explicit Player choices by default and mandatory Admin handling.
    // The existing per-Actor callback can enable automatic Player work and disable Admin work.
    initialPolicy: (Actor) -> AutoExecPolicy = { AutoExecPolicy.EAGER },
) {
  /** Opt-in task recording, including Player autoexecution; see [TaskLog] for supported scope. */
  public val taskLog: TaskLog = TaskLog(world)

  private val autoExecLoop = AutoExecLoop(world, taskLog)
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
   * World stays unchanged; mutating it later invalidates the speculative World. Application guards
   * are absent, so card movements can remain anonymous. Recording and completion callbacks begin
   * fresh. The returned session can be reset and reused without rebuilding its Agents.
   */
  // TODO(ENGINE_CONTRACT): investigate removing speculative sessions and their reset protocol.
  // The proposed contract does not select this capability; establish a required use before keeping
  // it.
  public fun overlay(): SpeculativeAgents =
      SpeculativeAgents(Engine.overlay(world)) { actor -> get(actor).autoExecPolicy }

  /**
   * A fully independent copy, including this World's complete event history. This also materializes
   * a speculative World's event suffix into the new World's ordinary history. Application guards
   * are not copied, so card movements in the copy can remain anonymous.
   */
  public fun fork(): Agents = withWorld(Engine.fork(world))

  private fun withWorld(otherWorld: World): Agents =
      Agents(otherWorld) { actor -> get(actor).autoExecPolicy }
}
