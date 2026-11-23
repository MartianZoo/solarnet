package dev.martianzoo.tfm.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Deferral.NOW
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Dispatch
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Player
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.state.tfmCatalog

/**
 * Named Terraforming Mars interactions over the existing Agent task API.
 *
 * A session's importer, REPL and Kotlin callers use the same registered implementations. Routines
 * supply only the requested decisions; model declarations determine costs, permissions and effects.
 * Card placement belongs to the model. Ordinary Agent automatic-execution rules remain in force;
 * Pay pauses participating Players only during its complete allocation and runs their enclosing
 * policies before returning.
 */
public class TfmRoutines(private val agents: Agents) {
  /**
   * Invokes a registered routine with its complete arguments. Failure restores this invocation's
   * game changes, including automatic work; earlier successful calls remain. The Agent and optional
   * releasing Agent belong to this session. Only Pay accepts a releasing Agent.
   *
   * PlayCard and action routines may return with payment or effect choices still pending. ExMachina
   * uses Agent's correction semantics rather than submitting ordinary tasks.
   */
  public fun execute(
      agent: Agent,
      name: String,
      vararg arguments: String,
      releasedBy: Agent? = null,
  ): TaskResult {
    val routine = requireNotNull(registered[name]) { "Unknown routine: $name" }
    require(arguments.size in routine.argumentCount) { "Unexpected arguments for $name" }
    require(releasedBy == null || name == "Pay") { "Only Pay accepts a releasing Agent" }
    return agents.world.timeline.atomic {
      if (routine.startsAction) prepareAction(agent)
      routine.submit(this, agent, arguments.toList(), releasedBy)
    }
  }

  /** Checks a group label's phase without performing any phase work. */
  internal fun checkPhase(name: String) {
    val phase = phaseGroups.getValue(name)
    check(agents[ADMIN].has(phase)) { "$name outside $phase" }
  }

  private fun prepareAction(agent: Agent) {
    if (!agent.has("ActionPhase")) return
    val holder =
        agents.world.actors
            .filterIsInstance<Player>()
            .map { agents[it] }
            .singleOrNull { it.has("Baton") }
    if (holder != null && holder.actor != agent.actor) {
      check(holder.has("MAX 0 ActionScope")) { "Previous action is unfinished" }
      holder.doTask("Ok")
    }
  }

  private fun playCard(agent: Agent, card: String, location: String) {
    val back = cardBack(agents.world.reader.tfmCatalog.card(cn(card)))!!.className
    val granted =
        agent.tasks
            .extract { it }
            .any {
              cn("PlayCard") in it.instruction.descendantsOfType<ClassName>()
            }
    if (!granted) agent.doTask("UseAction<PlayCardAction, Action1>")
    agent.doTask("PlayCard<Class<$back>, Class<$card>, $location>")
  }

  private fun useCardAction(agent: Agent, card: String, number: Int) {
    val classes = agent.reader.classTable
    require(classes.getClass(cn(card)).isSubtypeOf(classes.getClass(cn("ActionCard")))) {
      "UseAction requires an action card; use UsePolicy for the ruling policy"
    }
    useAction(agent, "UseActionOnCardAction", "UseAction<$card, Action$number>")
  }

  private fun useAction(agent: Agent, opener: String, request: String) {
    val instruction = agent.parseAs(InstructionTree::class, request) as InstructionTree
    val granted =
        agent.tasks
            .extract { it }
            .any {
              it.deferral == agent.tasks.earliestDeferral &&
                  instruction.narrows(it.instruction, agent.reader)
            }
    if (!granted) agent.doTask("UseAction<$opener, Action1>")
    agent.doTask(request)
  }

  private fun pay(agent: Agent, args: List<String>, releasedBy: Agent? = null) {
    val amounts = args.map { argument ->
      val gain =
          parse<InstructionTree>(argument) as? Gain
              ?: error("Pay requires positive, fixed resource amounts")
      require(gain.quantifier == null || gain.quantifier == MANDATORY) {
        "Pay requires exact amounts, without ? or *"
      }
      val resource = gain.scaledEx.expression.toString()
      val expression = gain.scaledEx.expression
      val held =
          expression.arguments.size == 1 &&
              agent.reader.classTable
                  .getClass(expression.className)
                  .isSubtypeOf(agent.reader.classTable.getClass(cn("CardResource")))
      require(resource in setOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat") || held) {
        "Unsupported payment resource: $resource"
      }
      val amount = gain.scaledEx.scalar.toString().toIntOrNull()
      require(amount != null && amount > 0) { "Pay requires a positive integer amount" }
      val payment =
          if (held) "PayFromCard<${expression.arguments.single()}>" else "Pay<Class<$resource>>"
      Triple(resource, amount, payment)
    }
    fun allocate() {
      fun releases() =
          releasedBy
              ?.tasks
              ?.extract { task ->
                val dispatch = task.instruction as? Dispatch
                if (dispatch?.deferral == NOW && dispatch.actor == agent.actor.expression)
                    task to ((dispatch.body as? Then)?.first ?: dispatch.body)
                else null
              }
              .orEmpty()
              .filterNotNull()
      fun declineUnused() {
        val paid = agent.count("Owed") == 0
        // Optional sources (e.g. unused Helion heat) can still block the zero-MC remainder.
        fun unused(instruction: InstructionTree): Boolean =
            (paid || NoOp.narrows(instruction, agent.reader)) &&
                instruction.descendantsOfType<Change>().any {
                  it.gaining?.className in setOf(cn("Pay"), cn("PayFromCard"))
                }
        // On another player's turn, payment offers arrive inside NOW releases too.
        releases()
            .filter { unused(it.second) }
            .sortedBy { if (NoOp.narrows(it.second, agent.reader)) 0 else 1 }
            .forEach { (task, _) ->
              releasedBy!!.doTask(task.id)
              agent.doTask("Ok")
            }
        agent.tasks
            .extract { it }
            .filter { unused(it.instruction) }
            .sortedBy { if (NoOp.narrows(it.instruction, agent.reader)) 0 else 1 }
            .forEach { agent.doTask("Ok", it.id) }
      }
      for ((resource, amount, payment) in amounts) {
        if (payment.startsWith("Pay<") && agent.count("Owed<Class<$resource>>") > 0) declineUnused()
        val request = "$amount $payment FROM $resource"
        if (releasedBy == null) agent.doTask(request)
        else {
          val instruction = agent.parseAs(InstructionTree::class, request) as InstructionTree
          val offer = releases().singleOrNull { instruction.narrows(it.second, agent.reader) }
          if (offer != null) {
            releasedBy.doTask(offer.first.id)
            agent.doTask(request)
          } else agent.doTask(request, releasedBy)
        }
      }
      check(agent.count("Owed") == 0) { "Pay did not cover the bill" }
      declineUnused()
    }

    agent.doWithAutoExecPolicy(NONE) {
      if (releasedBy == null) allocate() else releasedBy.doWithAutoExecPolicy(NONE) { allocate() }
    }
    agent.autoExecNow()
  }

  /**
   * A fixed routine's argument contract.
   *
   * @property argumentCount allowed argument counts after parsing.
   */
  public class Routine
  internal constructor(
      public val argumentCount: IntRange,
      internal val startsAction: Boolean = false,
      private val instructionGroup: Boolean = false,
      internal val submit: (TfmRoutines, Agent, List<String>, Agent?) -> Unit,
  ) {
    /** Parses this routine's argument text; instruction-group arguments retain their commas. */
    public fun arguments(text: String): List<String> =
        if (text.isBlank()) emptyList()
        else if (instructionGroup) listOf(text.trim()) else splitArguments(text)
  }

  public companion object {
    /** The complete fixed set of executable directive names, shared by all callers. */
    public val registered: Map<String, Routine> =
        mapOf(
            "PlayCard" to
                Routine(1..2, startsAction = true) { r, a, args, _ ->
                  r.playCard(a, args[0], args.getOrElse(1) { "Hand" })
                },
            "Pay" to Routine(1..Int.MAX_VALUE) { r, a, args, sender -> r.pay(a, args, sender) },
            "SelectTask" to
                Routine(1..1, instructionGroup = true) { _, a, args, _ ->
                  a.selectTask(args.single())
                },
            "ExMachina" to
                Routine(1..1, instructionGroup = true) { _, a, args, _ ->
                  a.exMachina(args.single())
                },
            "UseAction1" to
                Routine(1..1, startsAction = true) { r, a, args, _ ->
                  r.useCardAction(a, args.single(), 1)
                },
            "UseAction2" to
                Routine(1..1, startsAction = true) { r, a, args, _ ->
                  r.useCardAction(a, args.single(), 2)
                },
            "UseStandardProject" to
                Routine(1..1, startsAction = true) { r, a, args, _ ->
                  val project = args.single()
                  r.useAction(a, "UseStandardProjectAction", "UseAction<$project, Action1>")
                },
            "SendDelegate0" to action("UseAction<LobbyAction, Action1>"),
            "SendDelegate5" to action("UseAction<PaidLobbyAction, Action1>"),
            "Trade1" to action("UseAction<TradeAction, Action1>"),
            "Trade2" to action("UseAction<TradeAction, Action2>"),
            "Trade3" to action("UseAction<TradeAction, Action3>"),
            "UsePolicy" to action("UseAction<UsePolicyAction, Action1>"),
            "ConvertHeat" to action("UseAction<ConvertHeatAction, Action1>"),
            "ConvertPlants" to action("UseAction<ConvertPlantsAction, Action1>"),
            "ClaimMilestone" to action("UseAction<ClaimMilestoneAction, Action1>"),
            "FundAward" to action("UseAction<FundAwardAction, Action1>"),
            "DoRequiredActions" to action("DoRequiredActions"),
            "Pass" to action("Pass"),
        )

    /** Archive group labels and the phase each checks; their bodies always remain explicit. */
    internal val phaseGroups: Map<String, String> =
        mapOf(
            "Setup" to "SetupPhase",
            "Research" to "ResearchPhase",
            "Production" to "ProductionPhase",
            "WorldGovernment" to "SolarPhase",
            "GlobalEvent" to "SolarPhase",
        )

    private fun action(request: String): Routine =
        Routine(0..0, startsAction = true) { _, a, _, _ -> a.doTask(request) }

    /**
     * Splits text on outer commas while preserving nested Pets argument syntax. Blank text has no
     * arguments; empty members and mismatched or unclosed delimiters fail.
     */
    internal fun splitArguments(text: String): List<String> {
      if (text.isBlank()) return emptyList()
      val closing = mutableListOf<Char>()
      var start = 0
      val result = mutableListOf<String>()
      text.forEachIndexed { i, ch ->
        when (ch) {
          '<' -> closing += '>'
          '(' -> closing += ')'
          '[' -> closing += ']'
          '>',
          ')',
          ']' -> {
            require(closing.lastOrNull() == ch) { "Mismatched argument delimiter in $text" }
            closing.removeAt(closing.lastIndex)
          }
          ',' ->
              if (closing.isEmpty()) {
                result += text.substring(start, i).trim()
                start = i + 1
              }
        }
      }
      require(closing.isEmpty()) { "Unclosed argument delimiter in $text" }
      result += text.substring(start).trim()
      require(result.none { it.isEmpty() }) { "Empty argument in $text" }
      return result
    }
  }
}
