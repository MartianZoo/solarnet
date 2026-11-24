package dev.martianzoo.tfm.engine

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.TaskMatcher
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.GameRecording
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId

/**
 * Renders a Terraforming Mars event recording using its original compact configuration. The archive
 * contains explicit player tasks, corrections, setup declarations, and any tracked card movements.
 * Does not record or replay Agent calls. Construct a fresh exporter for each [export] call;
 * unsupported event shapes fail rather than silently dropping choices.
 */
public class GameArchiveExporter(
    private val recording: GameRecording,
    config: GameConfig,
) {
  private val config = config.copy(playerNames = recording.premise.playerNames)
  private val defaultPremise = recording.premise.catalog.gamePremise(this.config)
  private val localRules =
      recording.premise.premiseClassDeclarations.filter {
        it.className != recording.premise.premiseClassName &&
            it !in defaultPremise.premiseClassDeclarations
      }

  private data class Step(
      val actor: Actor,
      val instruction: InstructionTree,
      val phase: String,
      val generation: Int,
      val scope: Int?,
      val turn: Int,
      val solarEvent: Boolean,
      val correction: Int? = null,
      val paymentOffer: Boolean = false,
      val request: TaskId? = null,
      val available: Set<TaskId> = emptySet(),
      val amount: Boolean = false,
      val selection: Boolean = false,
      val skipped: Boolean = false,
      val turnDecline: Boolean = false,
      val context: ClassName? = null,
      val beforePreparation: GameWorld? = null,
      val executionStart: Int? = null,
  )

  private data class Block(
      val actor: Actor,
      var name: String,
      val turn: Int,
      val lines: MutableList<String> = mutableListOf(),
  )

  private val world = GameWorld(recording.premise, customClasses = TfmEngine.customClasses)
  private val classes = world.classTable
  private val events = recording.events

  /** Produces the human-readable archive; does not mutate or execute the source game. */
  public fun export(): String {
    require(config.playerNames == recording.premise.playerNames)
    val recorded = simplifyPreparations(readSteps())
    val steps = recorded.filterIndexed { index, step ->
      val next = recorded.getOrNull(index + 1)
      // A different player's next action already tells the importer to decline this second action.
      // Keep the explicit decline when the archive stops here or other work intervenes.
      !(step.turnDecline &&
          next != null &&
          next.actor != step.actor &&
          next.phase == "ActionPhase" &&
          (next.scope != null || (next.instruction as? Change)?.gaining?.className == cn("Pass")))
    }
    val blocks = mutableListOf<Block>()
    var index = 0
    while (index < steps.size) {
      val first = steps[index]
      val end =
          (index + 1..steps.size).first { next ->
            next == steps.size || !sameBlock(first, steps[next])
          }
      val chunk = steps.subList(index, end)
      if (first.phase != "ResearchPhase" && (first.scope != null || first.correction != null)) {
        blocks += renderBlock(chunk, steps)
      } else {
        chunk.groupBy { it.actor }.values.forEach { blocks += renderBlock(it, steps) }
      }
      index = end
    }
    val usedEvents =
        if (classes.findClass(cn("GlobalEventDrawn")) == null) 0
        else world.reader.count(world.reader.resolve(cn("GlobalEventDrawn").expression))
    return buildString {
      appendLine("Config {")
      appendLine("    ${config.playerNames.joinToString()}")
      val categories =
          listOf("MarsMap", "Module", "CardFront", "ColonyTile", "Milestone", "Award", "Other")
      val selections =
          config.includedClassNames.groupBy { name ->
            categories.firstOrNull { isSubtype(name.expression, it) } ?: "Other"
          }
      for (category in categories) {
        val entries =
            (if (category == "CardFront") config.includedCardBundles.map { "$it:cards" }
            else emptyList()) + selections[category].orEmpty().map { it.toString() }
        if (entries.isNotEmpty()) appendLine("    ${entries.joinToString()}")
      }
      localRules
          .filter { rule ->
            recording.premise.classSelections.any {
              it.included && it.className == rule.className
            } &&
                rule.className !in config.playerNames &&
                rule.className !in config.includedClassNames
          }
          .forEach { appendLine("    ${it.className}") }
      config.componentAdjustments.forEach { (name, count) -> appendLine("    $count $name") }
      config.excludedClassNames.forEach { appendLine("    -$it") }
      config.excludedCardBundles.forEach { appendLine("    -$it:cards") }
      appendLine("}")
      appendLine()
      appendLine("GlobalEventDeck {")
      recording.premise.drawOrder.take(usedEvents).forEach { appendLine("    $it") }
      appendLine("}")
      if (localRules.isNotEmpty()) {
        appendLine()
        appendLine("SetupRules {")
        localRules.forEach { rule ->
          rule.toString().lineSequence().forEach { appendLine("    $it") }
        }
        appendLine("}")
      }
      val cards = ArchiveCards.export(recording)
      if (cards.isNotEmpty()) {
        appendLine()
        appendLine("Cards {")
        cards.forEach { appendLine("    $it") }
        appendLine("}")
      }
      var previous: Block? = null
      for (block in blocks.filter { it.name.isNotEmpty() || it.lines.isNotEmpty() }) {
        if (
            previous == null ||
                block.turn != previous.turn ||
                block.actor != previous.actor ||
                block.turn == 0
        )
            appendLine()
        if (block.name == "pass") {
          check(block.lines.isEmpty()) { "Unexpected effects following $block" }
          appendLine("${block.actor}: ${block.name}")
        } else if (block.name == "exMachina") {
          appendLine("${block.actor}: exMachina(${block.lines.joinToString()})")
        } else {
          appendLine("${block.actor}${if (block.name.isEmpty()) "" else ": ${block.name}"} {")
          block.lines.forEach { appendLine("    $it") }
          appendLine("}")
        }
        previous = block
      }
    }
  }

  private fun sameBlock(a: Step, b: Step): Boolean =
      when {
        // Outside actions, explicit releases must precede their recipients even when grouping by
        // Actor.
        a.scope == null &&
            (a.instruction is Instruction.Dispatch || b.instruction is Instruction.Dispatch) ->
            false
        // Research is a complete per-player sequence, including the tasks created by purchasing.
        // Keep that player's draw, decisions, payment, and corrections together in this generation.
        a.phase == "ResearchPhase" -> b.phase == a.phase && b.generation == a.generation
        a.scope != null &&
            b.correction != null &&
            a.actor == b.actor &&
            a.turn == b.turn &&
            a.phase == b.phase -> true
        a.scope != null || b.scope != null -> a.scope == b.scope
        a.correction != null || b.correction != null -> a.correction == b.correction
        a.phase == "ActionPhase" || a.phase == "FinalGreeneryPhase" -> false
        else ->
            b.request in a.available &&
                a.phase == b.phase &&
                a.generation == b.generation &&
                a.solarEvent == b.solarEvent
      }

  private fun readSteps(): List<Step> {
    val owners = mutableMapOf<TaskId, Actor>()
    val original = mutableMapOf<TaskId, InstructionTree>()
    val selectedSystemSteps = mutableSetOf<TaskId>()
    val beforeNoOp = mutableMapOf<TaskId, InstructionTree>()
    val available = mutableMapOf<TaskId, List<Task>>()
    val beforeChoice = mutableMapOf<TaskId, GameWorld>()
    var transactionStart = 0
    val requests = mutableMapOf<TaskId, TaskId>()
    var released: Pair<TaskId, List<Task>>? = null
    var phase = "BootstrapPhase"
    var generation = 0
    var scope: Int? = null
    var turn = 0
    val result = mutableListOf<Step>()
    for (event in events) {
      if (event.ordinal == 0 || event.transaction != events[event.ordinal - 1].transaction)
          transactionStart = event.ordinal
      when (event) {
        is TaskAddedEvent -> {
          original[event.task.id] = event.task.instruction
          owners[event.task.id] = event.task.assignee
          if (event.task.selected) {
            requests[event.task.id] = released?.first ?: event.task.id
            available[event.task.id] = released?.second ?: (world.tasks.extract { it } + event.task)
            released = null
          }
        }
        is TaskEditedEvent -> {
          val actor = owners[event.task.id]
          val invoked = (event.task.instruction as? Change)?.gaining?.className
          // These archive macros already choose and execute their invocation as the block opener.
          val invocation = invoked in setOf(cn("UseAction"), cn("PlayCard"))
          if (
              actor is Player &&
                  !event.oldTask.selected &&
                  event.task.selected &&
                  (event.oldTask.instruction.descendantsOfType<Instruction.Then>().isNotEmpty() ||
                      event.oldTask.instruction
                          .descendantsOfType<Instruction.Each>()
                          .isNotEmpty() ||
                      event.oldTask.instruction is Instruction.Or ||
                      world.tasks
                          .extract { it }
                          .any {
                            it.instruction.descendantsOfType<Instruction.Each>().isNotEmpty()
                          })
          )
              beforeChoice[event.task.id] = world.fork()
          if (
              actor is Player &&
                  !invocation &&
                  event.task.selected &&
                  event.task.then != null &&
                  event.task.then != event.oldTask.then &&
                  event.oldTask.instruction.descendantsOfType<Instruction.Then>().isNotEmpty()
          ) {
            // Choosing a linked alternative must survive independently of its first executed step.
            if (event.task.assignee == ADMIN) selectedSystemSteps += event.task.id
            val continuation = event.task.then!!
            val returned = continuation.instructions.singleOrNull() as? Instruction.Dispatch
            // A System first step transfers to Admin and wraps its continuation to return it
            // to the Player. Select the original choice, before that execution-only dispatch.
            val following =
                if (
                    event.task.assignee == ADMIN &&
                        event.oldTask.assignee == actor &&
                        returned?.actor == actor.expression
                )
                    returned.body
                else continuation
            val followingStep =
                (following as? InstructionGroup)?.instructions?.singleOrNull() ?: following
            val chosen = Instruction.Then.createTree(listOf(event.task.instruction, followingStep))
            val previous = result.lastOrNull()
            if (
                previous?.selection == true &&
                    previous.request == event.task.id &&
                    events.subList(previous.executionStart!!, event.ordinal).none {
                      it is ChangeEvent
                    }
            )
                result.removeAt(result.lastIndex)
            result +=
                Step(
                    actor,
                    chosen,
                    phase,
                    generation,
                    scope,
                    turn,
                    isGlobalEvent(event.task),
                    selection = true,
                    request = event.task.id,
                    beforePreparation = beforeChoice[event.task.id],
                    executionStart = transactionStart,
                )
          }
          val before = beforeChoice[event.task.id]
          if (
              actor is Player &&
                  before != null &&
                  !invocation &&
                  event.task.selected &&
                  event.task.then == null &&
                  event.task.instruction is Change &&
                  !event.task.instruction.needsNarrowing(world.reader) &&
                  requiresExplicitSelection(before, actor, event.task)
          ) {
            result +=
                Step(
                    actor,
                    event.task.instruction,
                    phase,
                    generation,
                    scope,
                    turn,
                    isGlobalEvent(event.task),
                    selection = true,
                    request = event.task.id,
                    executionStart = transactionStart,
                )
          }
          // A THEN continuation can arrive before removal. It cannot join the original total.
          if (!event.oldTask.selected && event.task.selected)
              available[event.task.id] = world.tasks.extract { it }
          if (owners[event.task.id] == ADMIN && event.task.assignee is Player)
              owners[event.task.id] = event.task.assignee
          if (event.oldTask.instruction != NoOp)
              beforeNoOp[event.task.id] = event.oldTask.instruction
        }
        is TaskRemovedEvent -> {
          val actor = owners.remove(event.task.id)!!
          if (
              actor is Player && phase != "BootstrapPhase" && event.task.id !in selectedSystemSteps
          ) {
            check(event.task.selected) { "Cannot export a dropped player task: ${event.task}" }
            var instruction = event.task.instruction
            val actionDecline =
                instruction == NoOp &&
                    scope == null &&
                    original[event.task.id]?.descendantsOfType<Expression>()?.any {
                      it.className in setOf(cn("UseAction"), cn("DoRequiredActions"))
                    } == true
            if (instruction == NoOp && !actionDecline) {
              val previous = beforeNoOp[event.task.id]
              val change =
                  when (previous) {
                    is Change -> previous
                    is Instruction.Per -> previous.inner as? Change
                    else -> null
                  }
              if (previous is Instruction.By) instruction = previous
              else if (change != null)
                  instruction =
                      if (change.quantifier == Instruction.Quantifier.AMAP) previous!!
                      else change * 0
            }
            if (event.task.assignee != actor && instruction is Change) {
              val resolved = instruction
              val target = resolved.gaining ?: resolved.removing!!
              if (!isSubtype(target, "System")) {
                val originalChange =
                    original[event.task.id]?.descendantsOfType<Change>()?.singleOrNull()
                if (originalChange?.quantifier == Instruction.Quantifier.AMAP)
                    instruction =
                        Change.change(
                            resolved.gaining,
                            resolved.removing,
                            originalChange.count.toString().toInt(),
                            Instruction.Quantifier.AMAP,
                        )
                instruction =
                    Instruction.By(instruction as Instruction, event.task.assignee.expression)
              }
            }
            val paymentOffer =
                listOfNotNull(event.task.instruction, beforeNoOp[event.task.id]).any { tree ->
                  tree.descendantsOfType<Expression>().any {
                    it.className in setOf(cn("Pay"), cn("PayFromCard"))
                  }
                }
            val pending = available[event.task.id].orEmpty()
            val earliest = pending.minOfOrNull { it.deferral }
            val eligible = pending.filter { it.deferral == earliest }
            result +=
                Step(
                    actor,
                    instruction,
                    phase,
                    generation,
                    scope,
                    turn,
                    isGlobalEvent(event.task),
                    paymentOffer = paymentOffer,
                    beforePreparation =
                        beforeChoice[event.task.id].takeIf {
                          instruction is Instruction.Each || instruction is InstructionGroup
                        },
                    selection = instruction is InstructionGroup && !event.skipped,
                    skipped = event.skipped,
                    executionStart = transactionStart,
                    turnDecline = actionDecline && phase == "ActionPhase" && !event.skipped,
                    request = requests[event.task.id] ?: event.task.id,
                    available = eligible.map { it.id }.toSet(),
                    context =
                        event.task.cause?.context?.className?.takeIf { context ->
                          eligible.any {
                            it.id != event.task.id &&
                                it.assignee == actor &&
                                it.cause?.context?.className != context &&
                                overlaps(instruction, it.instruction)
                          }
                        },
                    amount =
                        original[event.task.id].let { initial ->
                          val change =
                              when (initial) {
                                is Change -> initial
                                is Instruction.Per -> initial.inner as? Change
                                else -> null
                              }
                          val resolved = instruction as? Change
                          change != null &&
                              resolved != null &&
                              change.quantifier == Instruction.Quantifier.MANDATORY &&
                              change.gaining == resolved.gaining &&
                              change.removing == resolved.removing
                        },
                )
          }
          if (
              actor is Player &&
                  event.task.assignee is Player &&
                  actor != event.task.assignee &&
                  phase != "BootstrapPhase"
          ) {
            // BY transfers leave a selected task for the recipient; both submissions are explicit.
            val release = result.last()
            result +=
                release.copy(
                    actor = event.task.assignee,
                    instruction = event.task.instruction,
                    amount = false,
                )
          }
          if (
              !event.skipped &&
                  (event.task.instruction as? Instruction.Dispatch)?.deferral ==
                      dev.martianzoo.pets.ast.Deferral.NOW
          ) {
            released = event.task.id to available[event.task.id].orEmpty()
          }
          selectedSystemSteps.remove(event.task.id)
          original.remove(event.task.id)
          beforeNoOp.remove(event.task.id)
          available.remove(event.task.id)
          beforeChoice.remove(event.task.id)
          requests.remove(event.task.id)
        }
        is ChangeEvent -> {
          val gain = event.change.gaining
          val loss = event.change.removing
          if (gain != null && isSubtype(gain.expression, "Phase")) phase = gain.className.toString()
          if (gain?.className == cn("Generation")) generation += event.change.count
          if (gain?.className == cn("ActionScope")) scope = event.ordinal
          if (loss?.className == cn("ActionScope")) scope = null
          if (gain?.className == cn("Baton")) turn = event.ordinal
          if (loss?.className == cn("Baton") && gain?.className != cn("Baton")) turn = 0
          if (event.cause == null && event.actor is Player && gain?.className != cn("Audit")) {
            val text = event.change.toString().removePrefix("+")
            result +=
                Step(
                    event.actor,
                    parse(text),
                    phase,
                    generation,
                    scope,
                    turn,
                    false,
                    event.transaction!!.serial,
                )
          }
        }
      }
      world.apply(event)
    }
    check(scope == null) { "Cannot export an unfinished action" }
    check(world.reader.count(world.reader.resolve(parse<Expression>("PaymentScope"))) == 0) {
      "Cannot export an unfinished payment"
    }
    check(world.tasks.selectedTask() == null) { "Cannot export an unfinished selection" }
    return result
  }

  // This is a conservative rendering choice, not task matching. Extra qualifiers are harmless;
  // the importer delegates their meaning and all legality checks to Agent.doTask.
  private fun overlaps(request: InstructionTree, other: InstructionTree): Boolean {
    val change = request as? Change ?: return true
    val targets = listOfNotNull(change.gaining, change.removing)
    return other.descendantsOfType<Change>().any { candidate ->
      listOfNotNull(candidate.gaining, candidate.removing).any { offered ->
        targets.any { target ->
          isSubtype(target, offered.className.toString()) ||
              isSubtype(offered, target.className.toString())
        }
      }
    }
  }

  private fun isGlobalEvent(task: Task): Boolean {
    var cause = task.cause
    while (cause != null) {
      if (cause.context.descendantsOfType<Expression>().any { isSubtype(it, "GlobalEvent") })
          return true
      cause = (events[cause.triggerEvent] as ChangeEvent).cause
    }
    return false
  }

  private fun isSubtype(expression: Expression, name: String): Boolean =
      classes.findClass(cn(name))?.let { classes.getClass(expression.className).isSubtypeOf(it) } ==
          true

  private fun renderBlock(steps: List<Step>, allSteps: List<Step>): Block {
    val first = steps.first()
    val label =
        when {
          first.phase == "ResearchPhase" -> "Research"
          first.correction != null -> "exMachina"
          first.scope != null -> ""
          first.phase == "SetupPhase" -> "Setup"
          first.phase == "ProductionPhase" -> "Production"
          first.phase == "SolarPhase" -> if (first.solarEvent) "GlobalEvent" else "WorldGovernment"
          else -> ""
        }
    val block = Block(first.actor, label, first.turn)
    fun line(text: String) {
      block.lines += text
    }
    fun action(text: String) {
      if (block.name.isEmpty()) block.name = text else line(text)
    }
    val payments = mutableListOf<Pair<String, Int>>()
    var paymentActor = block.actor
    var zeroPayment: String? = null
    fun flushPayment() {
      if (payments.isNotEmpty()) {
        val prefix = if (paymentActor == block.actor) "" else "$paymentActor: "
        line("${prefix}pay(${payments.joinToString { (resource, count) -> "$count $resource" }})")
        payments.clear()
      } else zeroPayment?.let(::line)
      zeroPayment = null
    }
    val correction = mutableListOf<Step>()
    fun flushCorrection() {
      if (correction.isEmpty()) return
      val actor = correction.first().actor
      val prefix = if (actor == block.actor) "" else "$actor: "
      line("${prefix}exMachina(${correction.joinToString { render(it.instruction, it.actor) }})")
      correction.clear()
    }
    for (step in combineAdjacentAmounts(steps, allSteps)) {
      if (correction.isNotEmpty() && correction.first().correction != step.correction)
          flushCorrection()
      if (step.correction != null && block.name != "exMachina") {
        flushPayment()
        correction += step
        continue
      }
      if (block.name == "exMachina") {
        line(render(step.instruction, step.actor))
        continue
      }
      val instruction = step.instruction
      if (step.skipped) {
        flushPayment()
        val prefix = if (step.actor == block.actor) "" else "${step.actor}: "
        val source = step.context?.let { " VIA $it" } ?: ""
        line("${prefix}exMachinaDropTask(${render(instruction, step.actor)})$source")
        continue
      }
      if (step.selection) {
        flushPayment()
        val prefix = if (step.actor == block.actor) "" else "${step.actor}: "
        line("${prefix}selectTask(${render(instruction, step.actor)})")
        continue
      }
      val change = instruction as? Change
      val gain =
          change?.gaining?.let {
            if (it.className in setOf(cn("UseAction"), cn("PlayCard")))
                world.reader.resolve(it).expressionFull
            else it
          }
      val loss = change?.removing
      val target = gain?.className?.toString()
      if (gain != null && gain.className in setOf(cn("Pay"), cn("PayFromCard"))) {
        if (step.actor != paymentActor) flushPayment()
        paymentActor = step.actor
        if (loss != null && change.count.toString().toInt() > 0) {
          payments +=
              render(Change.change(loss, null, 1, quantifier = null), step.actor) to
                  change.count.toString().toInt()
        } else if (loss != null) {
          val prefix = if (step.actor == block.actor) "" else "${step.actor}: "
          zeroPayment = prefix + render(instruction, step.actor)
        }
        continue
      }
      if (instruction == NoOp && step.paymentOffer) continue
      flushPayment()
      when (target) {
        "UseAction" -> {
          val provider = gain.arguments[1]
          val action = provider.className.toString()
          val slot = gain.arguments[2].className.toString()
          when {
            action in
                setOf("PlayCardAction", "UseActionOnCardAction", "UseStandardProjectAction") -> Unit
            isSubtype(provider, "StandardAction") ->
                action(
                    "action($action" +
                        (if (slot == "Action1") "" else ", " + slot.removePrefix("Action")) +
                        ")"
                )
            isSubtype(provider, "StandardProject") -> action("useStandardProject($action)")
            isSubtype(provider, "ActionCard") -> action("use" + slot + "($action)")
            else ->
                action(
                    (if (step.actor == block.actor) "" else "${step.actor}: ") +
                        render(instruction, step.actor)
                )
          }
        }
        "PlayCard" -> {
          val card = gain.arguments[2].arguments.single().className.toString()
          val location = gain.arguments[3].toString()
          val name = "playCard($card${if (location == "Hand") "" else ", $location"})"
          action(name)
        }
        "Pass",
        "DoRequiredActions" -> {
          val name = target.replaceFirstChar { it.lowercase() }
          if (block.name.isEmpty()) block.name = name else line(name)
        }
        else -> {
          val text = render(instruction, step.actor) + (step.context?.let { " VIA $it" } ?: "")
          line(if (step.actor == block.actor) text else "${step.actor}: $text")
        }
      }
    }
    flushCorrection()
    flushPayment()
    return block
  }

  private fun requiresExplicitSelection(before: GameWorld, actor: Player, task: Task): Boolean {
    val matcher = TaskMatcher(before)
    val ordinary =
        try {
          matcher.match(actor, task.instruction)
        } catch (_: TaskException) {
          null
        }
    if (
        ordinary?.task?.id == task.id &&
            ordinary.preparations.isEmpty() &&
            ordinary.instruction == task.instruction
    )
        return false
    val selection =
        try {
          matcher.match(actor, task.instruction, expandContainers = false)
        } catch (_: TaskException) {
          return false
        }
    return selection?.task?.id == task.id && selection.instruction == task.instruction
  }

  private fun simplifyPreparations(steps: List<Step>): List<Step> = buildList {
    var keepRequestSeparate = false
    for ((index, step) in steps.withIndex()) {
      if (canInferPreparation(step, steps.getOrNull(index + 1))) {
        keepRequestSeparate = true
      } else {
        // The proof is for this exact request, before any quantity aggregation in rendering.
        add(if (keepRequestSeparate) step.copy(amount = false) else step)
        keepRequestSeparate = false
      }
    }
  }

  private fun canInferPreparation(step: Step, next: Step?): Boolean {
    if (step.skipped) return false
    val before = step.beforePreparation ?: return false
    if (
        next == null ||
            next.actor != step.actor ||
            next.selection ||
            next.correction != null ||
            next.paymentOffer ||
            next.beforePreparation != null
    )
        return false
    val invocation = (next.instruction as? Change)?.gaining?.className
    if (invocation in setOf(cn("UseAction"), cn("PlayCard"), cn("Pay"), cn("PayFromCard")))
        return false
    val start = next.executionStart ?: return false
    // Retain the original timing whenever intervening execution could change the interpretation.
    if (
        events.subList(before.nextOrdinal, maxOf(before.nextOrdinal, start)).any {
          it is ChangeEvent ||
              (it is dev.martianzoo.state.GameEvent.TaskEvent && it.task.assignee == ADMIN)
        }
    )
        return false
    val matcher = TaskMatcher(before)
    val match =
        try {
          // Agent tries a whole pending total before ordinary matching. That interpretation must
          // not replace the particular recorded child on which this omission proof depends.
          if (
              matcher.matchingAmounts(
                  next.instruction,
                  false,
                  next.context,
                  before.tasks
                      .extract { it }
                      .filter { it.assignee == step.actor }
                      .map { it to it.instruction },
              ) != null
          )
              return false
          matcher.match(step.actor, render(next.instruction, next.actor), next.context)
        } catch (_: TaskException) {
          return false
        } ?: return false
    if (match.task.id != step.request) return false
    return if (step.selection && step.instruction !is InstructionGroup) {
      next.request == step.request &&
          match.preparations.isEmpty() &&
          match.instruction.narrows(step.instruction, before.reader) &&
          step.instruction.narrows(match.instruction, before.reader)
    } else {
      (step.instruction is Instruction.Each || step.instruction is InstructionGroup) &&
          match.preparations.firstOrNull() == step.instruction
    }
  }

  private fun combineAdjacentAmounts(steps: List<Step>, allSteps: List<Step>): List<Step> {
    val explicit = steps.filter { step ->
      val dispatch = step.instruction as? Instruction.Dispatch
      if (dispatch != null && !step.skipped) {
        check(dispatch.deferral == dev.martianzoo.pets.ast.Deferral.NOW) {
          "Unsupported player dispatch: $dispatch"
        }
      }
      // Nested recipient responses release their work through the shared matcher. Outside an
      // action, retain each release explicitly before the recipient's phase block.
      dispatch == null || step.skipped || step.scope == null
    }
    fun target(change: Change) = change.gaining ?: change.removing
    fun compatible(first: Step, next: Step): Boolean {
      val a = first.instruction as? Change ?: return false
      val b = next.instruction as? Change ?: return false
      return first.actor == next.actor &&
          first.scope == next.scope &&
          first.phase == next.phase &&
          first.generation == next.generation &&
          first.correction == null &&
          next.correction == null &&
          first.amount &&
          next.amount &&
          next.request in first.available &&
          !(a.gaining != null && a.removing != null) &&
          !(b.gaining != null && b.removing != null) &&
          target(a) == target(b) &&
          a.quantifier == Instruction.Quantifier.MANDATORY &&
          a.quantifier == b.quantifier
    }
    val result = mutableListOf<Step>()
    var index = 0
    while (index < explicit.size) {
      val first = explicit[index]
      var end = index + 1
      while (
          end < explicit.size &&
              first.skipped == explicit[end].skipped &&
              compatible(first, explicit[end])
      ) end++
      val group = explicit.subList(index, end)
      // A total must include every matching pending amount, whether performed or omitted later.
      val groupedRequests = group.map { it.request }.toSet()
      val outside = allSteps.any { it.request !in groupedRequests && compatible(first, it) }
      if (group.size > 1 && !outside) {
        val changes = group.map { it.instruction as Change }
        val sum = changes.sumOf {
          it.count.toString().toInt() * (if (it.gaining == null) -1 else 1)
        }
        result +=
            first.copy(
                context = null,
                instruction =
                    Change.change(
                        count = kotlin.math.abs(sum),
                        gaining = target(changes.first()).takeIf { sum >= 0 },
                        removing = target(changes.first()).takeIf { sum < 0 },
                        quantifier = changes.first().quantifier,
                    ),
            )
      } else result += group
      index = end
    }
    return result
  }

  private fun render(instruction: InstructionTree, actor: Actor): String {
    val transformer =
        object : PetTransformer() {
          override fun transformNode(node: PetNode): PetNode {
            val transformed = transformChildren(node)
            if (transformed !is Expression) return transformed
            val args =
                transformed.arguments.filterNot {
                  (it == actor.expression && isSubtype(transformed, "Owned")) ||
                      (it == cn("Hand").expression && isSubtype(transformed, "CardBack"))
                }
            return transformed.copy(arguments = args, argumentsSpecified = args.isNotEmpty())
          }
        }
    val relative = transformer.transformInstructionTree(instruction)
    if (relative !is Change || relative.quantifier == Instruction.Quantifier.AMAP)
        return relative.toString()
    val count = relative.count.toString().toInt()
    val gain = relative.gaining
    val loss = relative.removing
    val target = gain ?: loss!!
    val quantity = if (count == 1) "" else "$count "
    if ((gain == null || loss == null) && target.className == cn("Production")) {
      val owner = target.arguments.dropLast(1)
      val resource =
          target.arguments
              .last()
              .arguments
              .single()
              .copy(arguments = owner, argumentsSpecified = owner.isNotEmpty())
      return "PROD[${if (gain == null) "-" else ""}$quantity$resource]"
    }
    if (
        gain != null &&
            loss != null &&
            gain.className == loss.className &&
            isSubtype(gain, "CardBack") &&
            (gain.arguments + loss.arguments).none { isSubtype(it, "Player") }
    ) {
      val destination = gain.arguments.lastOrNull() ?: cn("Hand").expression
      val source = loss.arguments.lastOrNull() ?: cn("Hand").expression
      return "$quantity${gain.className}<$destination FROM $source>"
    }
    return Change.change(gain, loss, count, quantifier = null).toString()
  }
}
