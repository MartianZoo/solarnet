package dev.martianzoo.tfm.web.playground

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.agent.TaskForm
import dev.martianzoo.agent.TaskForm.Decision
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.canon.cardCost
import dev.martianzoo.tfm.canon.soloTileAreas
import dev.martianzoo.tfm.engine.CardTracking
import dev.martianzoo.tfm.engine.CardTracking.NamesRequiredException
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.engine.cardMatchesFilter
import dev.martianzoo.tfm.state.ApiUtils.mapDefinition

/**
 * A live solo game: its World and Agents, the named-card tracker, and the shuffled decks this
 * playground deals from. Every gameplay command is one attempt: when card tracking lacks names, a
 * deck or the player supplies them, and the same Agent command runs again from the same state.
 */
internal class PlaySession(
    val premise: GamePremise,
    val seed: Int,
    safeAutoexec: Boolean = true,
) {
  val agents = Agents(TfmEngine.newGame(premise))
  val world: World
    get() = agents.world

  private val tracker = CardTracking.start(world, requireSelectingNames = true)
  val player: Player = world.actors.filterIsInstance<Player>().single()

  /** Each back's seeded order; a deck is this order and how much of it has been dealt. */
  private val decks: Map<ClassName, List<ClassName>>

  /** Dealt counts per deck, recorded at each completed position where they changed. */
  private val dealt = mutableListOf<Pair<Checkpoint, Map<ClassName, Int>>>()

  /** The position at which a deck ran out, ending the game, and which deck it was. */
  private var ending: Pair<Checkpoint, ClassName>? = null

  /** The first position a player can see; earlier positions belong to world construction. */
  val start: Checkpoint

  /** The open draft narrowing of the selected task, if any choice has been made in it. */
  private var form: TaskForm? = null
  private var naming: Naming? = null
  private var optionLimit = OPTION_PAGE

  /** The most recent refusal, shown until the next successful command. */
  var problem: String? = null
    private set

  private class Attempt(val command: () -> Unit, val dealt: MutableMap<ClassName, Int>) {
    val named = mutableSetOf<Triple<ClassName, ClassName, Boolean>>()
    var lastRequest: NamesRequiredException? = null
  }

  private class Naming(val request: NamesRequiredException, val attempt: Attempt) {
    val chosen = mutableListOf<ClassName>()
  }

  init {
    require(cn("SoloMode") in premise.modules) { "Play requires SoloMode." }
    agents.taskLog.start()
    val front = world.classTable.getClass(cn("CardFront"))
    decks =
        world.classTable
            .allClasses()
            .filter { !it.abstract && it.isSubtypeOf(front) && world.classTable.isInhabited(it) }
            .groupBy { requireNotNull(cardBack(it)).className }
            .mapValues { (_, cards) -> orderedDeck(cards.map { it.className }, seed) }
    agents[player].autoExecPolicy = AutoExecPolicy.NONE
    val placementCards = decks.getValue(PROJECT_CARD).take(4)
    val areas =
        soloTileAreas(
            mapDefinition(world.reader),
            placementCards.map { cardCost(world.classTable.getClass(it)) },
        )
    dealt += world.timeline.checkpoint() to decks.mapValues { if (it.key == PROJECT_CARD) 4 else 0 }
    attempt { agents[ADMIN].beginOperation("SetupPhase FROM BootstrapPhase") }
    problem?.let { throw IllegalStateException("the game could not start: $it") }
    areas.forEachIndexed { index, area ->
      val tile = if (index % 2 == 0) "NormalCityTile" else "GreeneryTile"
      attempt { agents[ADMIN].doTask("$tile<SoloOpponent, ${area.className}>") }
      problem?.let { throw IllegalStateException("neutral tile placement failed: $it") }
    }
    start = world.timeline.checkpoint()
    if (safeAutoexec) setSafeAutoexec(true)
  }

  /** CONCRETE settles only a task that is the player's sole available choice. */
  fun setSafeAutoexec(enabled: Boolean) {
    val policy = if (enabled) AutoExecPolicy.CONCRETE else AutoExecPolicy.NONE
    if (agents[player].autoExecPolicy == policy) return
    if (enabled) {
      attempt {
        agents[player].autoExecPolicy = policy
        // Retrying after a card-name request must still settle under the new policy.
        agents[player].autoExecNow()
      }
    } else {
      agents[player].autoExecPolicy = policy
    }
  }

  /** Completed positions a player can visit, oldest first. */
  val stops: List<Checkpoint>
    get() = world.timeline.positions.filter { it.ordinal >= start.ordinal }

  val atTail: Boolean
    get() = world.timeline.checkpoint() == world.timeline.positions.last()

  /** Named cards of every back now in the player's [location] (Hand or Selecting). */
  fun held(location: ClassName): List<String> =
      tracker.hand(player, location = location).map { it.toString() }

  /** What the game waits on at the viewed position. */
  fun prompt(): Prompt {
    if (!atTail) return recorded()
    ending
        ?.takeIf { it.first == world.timeline.checkpoint() }
        ?.let {
          return Prompt("Game ended: the ${it.second} deck is exhausted")
        }
    return describe()
  }

  private fun describe(): Prompt {
    naming?.let { open ->
      val request = open.request
      val remaining = request.count - open.chosen.size
      val verb = if (request.arriving) "moves into" else "leaves"
      return Prompt(
          "Which ${request.back} $verb ${request.location}? ($remaining more)",
          "Event ${request.event.ordinal}: ${request.event.change}",
          (request.candidates.orEmpty() - open.chosen.toSet()).map {
            Prompt.Option(it.toString(), held = true)
          },
          retreat = "CANCEL ATTEMPT",
      )
    }
    nextAdminTask()?.let { task ->
      return Prompt(
          "Continue automatic work",
          options = listOf(Prompt.Option(label(task), who = ADMIN.toString(), taskId = task.id)),
      )
    }
    val selected = world.tasks.selectedTask()?.let(world.tasks::getTaskData)
    if (selected != null) {
      if (selected.assignee == ADMIN) return waitingForAdmin(listOf(selected))
      return narrowing(selected)
    }
    val pending = pendingTasks()
    if (pending.isEmpty()) {
      val adminTasks = agents[ADMIN].tasks.extract { it }.filterNot { it.deferred }
      if (adminTasks.isNotEmpty()) return waitingForAdmin(adminTasks)
      val phase = world.reader.getComponents(cn("Phase").expression).elements.singleOrNull()
      return Prompt(
          if (world.tasks.extract { it }.isEmpty()) "No pending work"
          else "Only deferred work remains",
          buildList {
                phase?.let { add("Phase: $it") }
                if (agents[player].count("Victory") > 0) add("Victory recorded")
                if (agents[ADMIN].count("VictoryPhase") > 0) {
                  add("Score: ${agents[player].count("VictoryPoint")} VP")
                }
              }
              .joinToString(" · "),
      )
    }
    return Prompt(
        "Choose a task",
        options =
            pending.map { task ->
              taskOption(world, task) { selected ->
                val form = agents[selected.assignee].fillInTask(selected.id)
                form
                    .decisions()
                    .singleOrNull()
                    ?.takeIf { it.kind == Decision.Kind.ALTERNATIVE }
                    ?.let { form.options(it).toList() }
                    .orEmpty()
              }
            },
    )
  }

  private fun waitingForAdmin(tasks: List<Task>): Prompt =
      Prompt(
          "Admin work needs automatic handling",
          tasks.joinToString("\n") { label(it) },
          enabled = false,
      )

  /** The engine's pending work at a viewed earlier position, without draft choices. */
  private fun recorded(): Prompt {
    val selected = world.tasks.selectedTask()?.let(world.tasks::getTaskData)
    val pending = pendingTasks()
    return Prompt(
        if (selected != null) "Task ${selected.id.ordinal} was selected" else "Choose a task",
        selected?.let { "${it.assignee}: ${it.instruction}" },
        pending.map { Prompt.Option(label(it), who = it.assignee.toString(), taskId = it.id) },
        enabled = false,
    )
  }

  private fun narrowing(task: Task): Prompt {
    val form = formFor(task)
    val instruction = form.instruction
    val detail = "Task ${task.id.ordinal} (${task.assignee}): $instruction"
    val retreat = if (instruction != task.instruction) "RESET CHOICES" else null
    return try {
      val decision =
          form.decisions().singleOrNull()
              ?: return Prompt(
                  "Execute this task",
                  detail,
                  listOf(Prompt.Option("EXECUTE")),
                  false,
                  retreat,
              )
      val heldNames = (held(HAND) + held(SELECTING)).toSet()
      val playable = cardChoices(instruction, decision)
      val window =
          form
              .options(decision)
              .filter { playable == null || it == "Ok" || it in playable }
              .take(optionLimit + 1)
              .toList()
      val shown = window.take(optionLimit).map { Prompt.Option(it, held = it in heldNames) }
      val ordered =
          if (decision.kind == Decision.Kind.TARGET) shown.sortedByDescending { it.held } else shown
      Prompt(
          if (shown.isEmpty()) "No choices available; use history and TRUNCATE to undo this task"
          else heading(decision),
          detail,
          ordered,
          window.size > optionLimit,
          retreat,
      )
    } catch (unsupported: UnsupportedOperationException) {
      Prompt(
          "This task's next choice cannot be listed: ${unsupported.message}",
          detail,
          retreat = retreat,
      )
    }
  }

  /** Card-playing choices use known faces at the source, not the catalog's entire card pool. */
  private fun cardChoices(instruction: InstructionTree, decision: Decision): Set<String>? {
    val focus = decision.focus ?: return null
    val table = world.classTable
    val front = table.getClass(cn("CardFront"))
    if (!table.getClass(focus.className).isSubtypeOf(front)) return null
    val change = instruction.descendantsOfType<Change>().firstOrNull() ?: return null
    val gain = change.gaining?.let(world.reader::resolve) ?: return null
    val back = table.getClass(cn("CardBack"))
    val location = table.getClass(cn("CardLocation"))
    val source =
        when (gain.className) {
          cn("PlayProject") ->
              return tracker.hand(player, PROJECT_CARD).mapTo(linkedSetOf()) { "$it" }
          cn("PlayCard") -> gain
          else -> {
            if (!gain.rootClass.isSubtypeOf(front)) return null
            change.removing?.let(world.reader::resolve)?.takeIf { it.rootClass.isSubtypeOf(back) }
                ?: return null
          }
        }
    val arguments = source.expressionFull.arguments.map(world.reader::resolve)
    val sourceLocation = arguments.single { it.rootClass.isSubtypeOf(location) }.rootClass
    val sourceBack =
        if (source.rootClass.isSubtypeOf(back)) source.rootClass
        else
            arguments
                .single { it.groundType.representedClass?.isSubtypeOf(back) == true }
                .groundType
                .representedClass!!
    return listOf(HAND, SELECTING)
        .filter { table.getClass(it).isSubtypeOf(sourceLocation) }
        .flatMap { tracker.hand(player, location = it) }
        .filter { cardBack(table.getClass(it))?.isSubtypeOf(sourceBack) == true }
        .mapTo(linkedSetOf()) { "$it" }
  }

  private fun heading(decision: Decision): String =
      when (decision.kind) {
        Decision.Kind.ALTERNATIVE -> "Choose an alternative"
        Decision.Kind.TARGET -> "Narrow ${decision.focus}"
        Decision.Kind.AMOUNT -> "Choose an amount"
      }

  /** Acts on [option] of the current prompt. */
  fun choose(option: Prompt.Option) {
    check(atTail) { "history is read-only" }
    check(ending == null) { "the game has ended" }
    problem = null
    val label = option.label
    val open = naming
    if (open != null) {
      open.chosen += cn(label)
      if (open.chosen.size == open.request.count) {
        naming = null
        try {
          supply(open.request, open.chosen, open.attempt)
          run(open.attempt)
        } catch (refused: Exception) {
          fail(open.attempt, refused)
        }
      }
      return
    }
    option.taskId
        ?.let(world.tasks::getTaskData)
        ?.takeIf { it.assignee == ADMIN }
        ?.let { task ->
          attempt { agents[ADMIN].doTask(task.id) }
          return
        }
    val selected = world.tasks.selectedTask()?.let(world.tasks::getTaskData)
    if (selected == null) {
      val task = world.tasks.getTaskData(requireNotNull(option.taskId))
      attempt { agents[task.assignee].selectTask(task.id) }
      return
    }
    check(selected.assignee == player) { "Admin tasks are handled automatically" }
    val form = formFor(selected)
    val decision = form.decisions().singleOrNull()
    if (decision != null) {
      try {
        form.choose(decision, label)
      } catch (refused: Exception) {
        problem = refused.message
        return
      }
    }
    optionLimit = OPTION_PAGE
    val finished =
        try {
          form.decisions().isEmpty()
        } catch (_: UnsupportedOperationException) {
          // Let ordinary engine resolution advance the chosen branch.
          form.instruction != selected.instruction
        }
    if (finished) {
      this.form = null
      attempt { form.commit() }
    }
  }

  /** Selects a pending OR task and chooses the clicked arm from its resulting form. */
  fun chooseTaskArm(option: Prompt.Option, arm: String) {
    require(arm in option.arms)
    choose(option)
    if (problem == null) {
      prompt().options.firstOrNull { it.label == arm }?.let(::choose)
    }
  }

  /** Lists more options of a long decision. */
  fun showMore() {
    optionLimit += OPTION_PAGE
  }

  /** Discards draft choices, or the open naming attempt and every name it supplied. */
  fun retreat() {
    problem = null
    naming?.let { open ->
      naming = null
      abandon(open.attempt)
      return
    }
    form = null
    optionLimit = OPTION_PAGE
  }

  /** Moves to completed position [checkpoint], leaving any later history in place. */
  fun seek(checkpoint: Checkpoint) {
    naming?.let { abandon(it.attempt) }
    naming = null
    form = null
    optionLimit = OPTION_PAGE
    problem = null
    world.timeline.seek(checkpoint)
  }

  /** Discards the history after the viewed position so that play can continue from it. */
  fun truncate() {
    world.timeline.truncate()
    val cut = world.timeline.checkpoint()
    dealt.removeAll { it.first.ordinal > cut.ordinal }
    if ((ending?.first?.ordinal ?: -1) > cut.ordinal) ending = null
    for (back in decks.keys) {
      for (location in listOf(HAND, SELECTING)) {
        tracker.replacePendingArrivals(player, back = back, location = location)
        tracker.replacePendingDepartures(player, back = back, location = location)
      }
    }
    form = null
    naming = null
    problem = null
  }

  private fun nextAdminTask(): Task? {
    val admin = agents[ADMIN]
    return admin.tasks
        .matching { !it.deferred && admin.canExecuteTask(it.id) }
        .firstOrNull()
        ?.let(world.tasks::getTaskData)
  }

  private fun pendingTasks(): List<Task> =
      agents[player].tasks.extract { it }.filterNot { it.deferred }

  private fun formFor(task: Task): TaskForm =
      form?.takeIf { it.taskId == task.id }
          ?: agents[task.assignee].fillInTask(task.id).also { form = it }

  private fun attempt(command: () -> Unit) {
    check(atTail && naming == null) { "no command can start now" }
    tracker.requireComplete()
    run(Attempt(command, dealt.last().second.toMutableMap()))
  }

  private fun run(initial: Attempt) {
    var attempt = initial
    while (true) {
      val request =
          try {
            world.timeline.atomic {
              attempt.command()
              tracker.requireComplete()
            }
            world.timeline.recordPosition()
            if (attempt.dealt != dealt.last().second) {
              dealt += world.timeline.checkpoint() to attempt.dealt.toMap()
            }
            form = null
            // EAGER can leave executable Admin work pending when card names are missing. The
            // engine probe ignores external names; execute through our normal naming/retry path.
            val next = nextAdminTask() ?: return
            attempt = Attempt({ agents[ADMIN].doTask(next.id) }, dealt.last().second.toMutableMap())
            continue
          } catch (request: NamesRequiredException) {
            request
          } catch (refused: Exception) {
            fail(attempt, refused)
            return
          }
      try {
        val previous = attempt.lastRequest
        check(
            previous == null ||
                previous.event.ordinal != request.event.ordinal ||
                previous.player != request.player ||
                previous.back != request.back ||
                previous.location != request.location ||
                previous.arriving != request.arriving ||
                previous.count != request.count
        ) {
          "Supplying card names made no progress: ${request.message}"
        }
        attempt.lastRequest = request
        val candidates = request.candidates
        when {
          candidates == null ->
              if (!deal(request, attempt)) {
                abandon(attempt)
                ending = world.timeline.checkpoint() to request.back
                return
              }
          candidates.size < request.count -> {
            abandon(attempt)
            problem = "${request.message}, but only ${candidates.size} could move"
            return
          }
          candidates.size == request.count -> supply(request, candidates.toList(), attempt)
          else -> {
            naming = Naming(request, attempt)
            return
          }
        }
      } catch (refused: Exception) {
        fail(attempt, refused)
        return
      }
    }
  }

  private fun fail(attempt: Attempt, refused: Exception) {
    abandon(attempt)
    form = null
    problem = refused.message ?: refused.toString()
  }

  /** Deals [request]'s cards from its deck, skipping cards its filter rejects; false if empty. */
  private fun deal(request: NamesRequiredException, attempt: Attempt): Boolean {
    val order = decks[request.back] ?: throw IllegalStateException("no ${request.back} deck")
    var cursor = attempt.dealt.getValue(request.back)
    val cards = mutableListOf<ClassName>()
    while (cards.size < request.count) {
      val card = order.getOrNull(cursor++) ?: return false
      val filter = request.filter
      if (filter == null || cardMatchesFilter(world.reader, card, filter)) cards += card
    }
    attempt.dealt[request.back] = cursor
    supply(request, cards, attempt)
    return true
  }

  private fun supply(request: NamesRequiredException, cards: List<ClassName>, attempt: Attempt) {
    val names = cards.toTypedArray()
    if (request.arriving) {
      tracker.expectArrivals(
          request.player,
          *names,
          back = request.back,
          location = request.location,
      )
    } else {
      tracker.expectDepartures(
          request.player,
          *names,
          back = request.back,
          location = request.location,
      )
    }
    attempt.named += Triple(request.back, request.location, request.arriving)
  }

  /** Forgets every name [attempt] supplied; its provisional deals were never recorded. */
  private fun abandon(attempt: Attempt) {
    for ((back, location, arriving) in attempt.named) {
      if (arriving) tracker.replacePendingArrivals(player, back = back, location = location)
      else tracker.replacePendingDepartures(player, back = back, location = location)
    }
  }

  private fun label(task: Task): String =
      "${task.id.ordinal}  ${task.instruction}${task.then?.let { " THEN $it" } ?: ""}"

  private companion object {
    val HAND = cn("Hand")
    val SELECTING = cn("Selecting")
    val PROJECT_CARD = cn("ProjectCard")
    const val OPTION_PAGE = 200
  }
}
