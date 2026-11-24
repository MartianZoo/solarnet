package dev.martianzoo.agent

import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.SpeculativeWorld
import dev.martianzoo.engine.World
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.SystemClasses
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Player
import dev.martianzoo.state.TaskResult

/**
 * A [World] together with a stable [Agent] for each Player. Clients hold this rather than a World
 * and a separate collection of Agents, so an Agent is always paired with the World it acts on.
 * Calls through these Agents coordinate their changes and subsequent task handling.
 *
 * [initialPolicy] supplies each Player's initial automatic-execution policy, defaulting to
 * [AutoExecPolicy.NONE]. It is evaluated once per Player; the engine handles mandatory Admin work.
 * Construction does not execute pending tasks; subsequent Agent commands handle them.
 *
 * [routines] supplies this session's named client interactions. Generic sessions have none.
 * Definitions are retained by [fork] and must use the receiving session supplied to [Routine].
 */
public class Agents(
    public val world: World,
    routines: Map<String, Routine> = emptyMap(),
    initialPolicy: (Player) -> AutoExecPolicy = { AutoExecPolicy.NONE },
) {
  internal val routines: Map<String, Routine> = routines.toMap()

  /**
   * Optional application check for this session; fresh sessions and speculative copies have none.
   */
  public var transactionGuard: AgentTransactionGuard? = null
    private set

  /**
   * Installs this session's application check once. Direct engine calls and other independently
   * constructed sessions bypass it unless called inside this session's restoring call. Install
   * before submitting commands which need validation. A speculative session cannot install a guard;
   * fork a complete World to start tracked play.
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

  private val autoExecLoop = AutoExecLoop(world, ::validateApplication)
  private val elaborator = PetElaborator(world.classTable)

  private val agents: Map<Player, Agent> =
      world.actors.filterIsInstance<Player>().associateWith {
        AgentImpl(
            this,
            world.actorEngine(it),
            elaborator,
            autoExecLoop,
            ::validateApplication,
            initialPolicy(it),
        )
      }

  /** This world's Agent for [player]. Admin has no Agent. */
  public operator fun get(player: Player): Agent = agents.getValue(player)

  /**
   * Applies an administrative correction with this session's input check, without running pending
   * tasks. [changes] must contain fully elaborated concrete instructions. A correction cannot leave
   * additional unfinished cleanup work. Any failure restores the correction; returned changes and
   * surviving tasks cover the entire correction.
   */
  public fun exMachina(changes: InstructionGroup): TaskResult =
      world.timeline.atomic {
        val cleanup = SystemClasses.MUST_CLEAN_UP.expression
        val before = world.reader.getComponents(cleanup).entries.associate { it.key to it.value }
        world.actorEngine(Actor.ADMIN).exMachina(changes)
        validateApplication()
        if (
            world.reader.getComponents(cleanup).entries.any { (type, count) ->
              count > (before[type] ?: 0)
            }
        ) {
          throw NotNowException("exMachina cannot leave new unfinished work")
        }
      }

  /**
   * Runs [block], eligible Admin work, and configured Player policies in a restoring call. The
   * session's application check is scoped to this call, including nested engine commands and
   * individual Admin attempts. A failure restores every transaction in the call. The result covers
   * all retained changes and surviving spawned tasks. Nested calls defer optional Player handling
   * to the outermost call; they still finish eligible Admin work and validate their input. Starting
   * this call inside an already-open attributed engine transaction throws [IllegalStateException]
   * before [block] runs; put this session call outside the transaction.
   */
  public fun transact(block: () -> Unit = {}): TaskResult =
      world
          .actorEngine(Actor.ADMIN)
          .transact(
              afterChanges = autoExecLoop::run,
              validateApplication = ::validateApplication,
              block = block,
          )

  /**
   * A fully independent copy, including this World's complete event history. This also materializes
   * a speculative World's event suffix into the new World's ordinary history. Application guards
   * are not copied, so card movements in the copy can remain anonymous.
   */
  public fun fork(): Agents = withWorld(Engine.fork(world))

  private fun withWorld(otherWorld: World): Agents =
      Agents(otherWorld, routines) { actor -> get(actor).autoExecPolicy }
}
