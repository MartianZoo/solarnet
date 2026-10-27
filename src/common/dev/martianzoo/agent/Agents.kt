package dev.martianzoo.agent

import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.SpeculativeWorld
import dev.martianzoo.engine.World
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.state.Actor

/**
 * A [World] together with a stable [Agent] for each Actor. Clients hold this rather than a World
 * and a separate collection of Agents, so an Agent is always paired with the World it acts on.
 * Calls through these Agents coordinate their changes and subsequent task handling.
 *
 * [initialPolicy] supplies each Player's initial automatic-execution policy, defaulting to
 * [AutoExecPolicy.EAGER]. It is evaluated once per Player; Admin always uses mandatory eager
 * handling. Construction does not execute pending tasks; subsequent Agent commands handle them.
 */
public open class Agents(
    public val world: World,
    // TODO: migrate Players to a suitable smart policy; the user deferred default-policy cleanup.
    initialPolicy: (Actor) -> AutoExecPolicy = { AutoExecPolicy.EAGER },
) {
  /** Opt-in task recording, including Player autoexecution; see [TaskLog] for supported scope. */
  public val taskLog: TaskLog = TaskLog(world)

  /**
   * Optional application check for this session; fresh sessions and speculative copies have none.
   */
  public var transactionGuard: AgentTransactionGuard? = null
    private set

  /**
   * Installs this session's application check once. Direct engine calls and other independently
   * constructed sessions bypass it. Install before submitting commands which need validation. A
   * speculative session cannot install a guard; fork a complete World to start tracked play.
   */
  public fun installTransactionGuard(guard: AgentTransactionGuard) {
    check(world !is SpeculativeWorld) {
      "a speculative session cannot install an Agent transaction guard"
    }
    check(transactionGuard == null) { "An Agent transaction guard is already installed" }
    transactionGuard = guard
  }

  internal fun validateApplication() {
    transactionGuard?.validate()
  }

  private val autoExecLoop = AutoExecLoop(world, taskLog, ::validateApplication)
  private val elaborator = PetElaborator(world.classTable)

  private val agents: Map<Actor, Agent> =
      world.actors.associateWith {
        AgentImpl(
            world,
            world.actorEngine(it),
            elaborator,
            autoExecLoop,
            taskLog,
            ::validateApplication,
            if (it == Actor.ADMIN) AutoExecPolicy.EAGER else initialPolicy(it),
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
  // TODO(ENGINE_CONTRACT): no required use is established. Nothing in production constructs these
  // sessions; the engine's own trial Worlds do not use them. Removing this surface is available,
  // but it deletes the overlay-invalidation and card-anonymity scenarios that now cover it.
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
