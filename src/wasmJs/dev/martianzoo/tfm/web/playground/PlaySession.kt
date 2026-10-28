package dev.martianzoo.tfm.web.playground

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.agent.GameFileImporter
import dev.martianzoo.agent.TaskForm
import dev.martianzoo.agent.TaskForm.Decision
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.engine.World
import dev.martianzoo.engine.recording
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.GameRecording
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.canon.cardCost
import dev.martianzoo.tfm.canon.soloTileAreas
import dev.martianzoo.tfm.engine.CardTracking
import dev.martianzoo.tfm.engine.CardTracking.NamesRequiredException
import dev.martianzoo.tfm.engine.DecisionSearch
import dev.martianzoo.tfm.engine.DecisionSearch.Verdict.COMPLETES
import dev.martianzoo.tfm.engine.DecisionSearch.Verdict.DEAD_END
import dev.martianzoo.tfm.engine.DecisionSearch.Verdict.UNKNOWN
import dev.martianzoo.tfm.engine.TfmAutoExec
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.engine.cardMatchesFilter
import dev.martianzoo.tfm.state.ApiUtils.mapDefinition
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.serialization.Serializable

/**
 * A live solo game: its World and Agents, the named-card tracker, and the shuffled decks this
 * playground deals from. Every gameplay command is one attempt: when card tracking lacks names, a
 * deck or the player supplies them, and the same Agent command runs again from the same state.
 *
 * With safe autoexecution, the Player's SAFE policy performs every task proven to lose nothing by
 * being performed now, and this session deals the cards such tasks draw. A [DecisionSearch] of the
 * work under way then hides each offered choice proven unable to finish it, and when exactly one
 * choice remains, takes it: that move cannot be a mistake either.
 */
internal class PlaySession
internal constructor(
    val premise: GamePremise,
    val seed: Int,
    val agents: Agents,
    recovered: Recovery?,
) {
  constructor(
      premise: GamePremise,
      seed: Int,
      safeAutoexec: Boolean = false,
  ) : this(premise, seed, TfmEngine.agents(TfmEngine.newGame(premise)), null) {
    if (safeAutoexec) setSafeAutoexec(true)
  }

  @Serializable
  internal data class Recovery(
      val seed: Int,
      val startOrdinal: Int,
      val dealt: List<DeckPosition>,
      val endingOrdinal: Int? = null,
      val endingBack: String? = null,
      val gameFile: String,
  ) {
    @Serializable internal data class DeckPosition(val ordinal: Int, val counts: Map<String, Int>)
  }

  val world: World
    get() = agents.world

  private val tracker =
      if (recovered == null) CardTracking.start(world, requireSelectingNames = true)
      else recoverCardTracking(world)
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
  private var pendingCardTask: TaskId? = null
  private val chosenCards = linkedSetOf<ClassName>()
  private var optionLimit = OPTION_PAGE
  private var safeAutoexecEnabled = false

  /** Receives the search's reasons for dead ends and undecided branches, for diagnosis. */
  internal var searchTrace: ((String) -> Unit)? = null
  private var settling = false

  /** The last described prompt, with its choices' actions, while its inputs are unchanged. */
  private var described: Described? = null

  private class Described(val key: List<Any?>, val prompt: Prompt, val forced: (() -> Unit)?)

  /** The most recent refusal, shown until the next successful command. */
  var problem: String? = null
    private set

  private class Attempt(
      val command: () -> Unit,
      val dealt: MutableMap<ClassName, Int>,
      val cards: CardChoice? = null,
      val chosen: List<ClassName> = emptyList(),
  ) {
    val named = mutableMapOf<Triple<ClassName, ClassName, Boolean>, MutableList<ClassName>>()
    var lastRequest: NamesRequiredException? = null
  }

  private class Naming(val request: NamesRequiredException, val attempt: Attempt) {
    val chosen = mutableListOf<ClassName>()
  }

  /** Whether the current action has choices that are not yet in a completed World position. */
  internal val hasDraftInput: Boolean
    get() =
        problem != null ||
            naming != null ||
            pendingCardTask != null ||
            chosenCards.isNotEmpty() ||
            optionLimit != OPTION_PAGE ||
            form?.let { draft ->
              world.tasks.selectedTask()?.let(world.tasks::getTaskData)?.instruction !=
                  draft.instruction
            } == true

  private data class CardChoice(
      val back: ClassName,
      val source: ClassName,
      val drawing: Boolean,
      val required: Int?,
      val maximum: Int?,
  )

  init {
    require(cn("SoloMode") in premise.modules) { "Play requires SoloMode." }
    val front = world.classTable.getClass(cn("CardFront"))
    decks =
        world.classTable
            .allClasses()
            .filter { !it.abstract && it.isSubtypeOf(front) && world.classTable.isInhabited(it) }
            .groupBy { requireNotNull(cardBack(it)).className }
            .mapValues { (_, cards) -> orderedDeck(cards.map { it.className }, seed) }
    if (recovered != null) {
      require(recovered.seed == seed) { "Saved shuffle seed differs from the replay" }
      start =
          requireNotNull(
              world.timeline.positions.firstOrNull { it.ordinal >= recovered.startOrdinal }
          ) {
            "The replay has no completed position after the saved game start"
          }
      require(recovered.dealt.isNotEmpty()) { "Saved deck progress is empty" }
      dealt +=
          recovered.dealt.map { position ->
            Checkpoint(position.ordinal) to position.counts.mapKeys { cn(it.key) }
          }
      ending =
          recovered.endingOrdinal?.let { ordinal ->
            Checkpoint(ordinal) to cn(requireNotNull(recovered.endingBack))
          }
      agents[player].autoExecPolicy = AutoExecPolicy.NONE
      attempt {
        agents[ADMIN].autoExecPolicy = AutoExecPolicy.EAGER
        agents[ADMIN].autoExecNow()
      }
    } else {
      agents.taskLog.start()
      agents.taskLog.gameStarted()
      agents[player].autoExecPolicy = AutoExecPolicy.NONE
      val placementCards = decks.getValue(PROJECT_CARD).take(4)
      val areas =
          soloTileAreas(
              mapDefinition(world.reader),
              placementCards.map { cardCost(world.classTable.getClass(it)) },
          )
      dealt +=
          world.timeline.checkpoint() to decks.mapValues { if (it.key == PROJECT_CARD) 4 else 0 }
      attempt { agents[ADMIN].beginOperation("SetupPhase FROM BootstrapPhase") }
      problem?.let { throw IllegalStateException("the game could not start: $it") }
      areas.forEachIndexed { index, area ->
        val tile = if (index % 2 == 0) "NormalCityTile" else "GreeneryTile"
        attempt { agents[ADMIN].doTask("$tile<SoloOpponent, ${area.className}>") }
        problem?.let { throw IllegalStateException("neutral tile placement failed: $it") }
      }
      start = world.timeline.checkpoint()
    }
  }

  /** Saves completed gameplay and the deck positions needed to continue dealing after a reload. */
  suspend fun recovery(): Recovery =
      Recovery(
          seed,
          start.ordinal,
          dealt.map { (checkpoint, counts) ->
            Recovery.DeckPosition(checkpoint.ordinal, counts.mapKeys { it.key.toString() })
          },
          ending?.first?.ordinal,
          ending?.second?.toString(),
          replayFile(),
      )

  /**
   * SAFE performs the Player's tasks that cannot be performed too early, then this session takes
   * each sole available choice in the work under way.
   */
  fun setSafeAutoexec(enabled: Boolean) {
    safeAutoexecEnabled = enabled
    described = null
    lookahead = null
    lookaheadAt = null
    projectCheckKey = null
    cardActionCheckKey = null
    val policy = if (enabled) AutoExecPolicy.SAFE else AutoExecPolicy.NONE
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

  val completed: Boolean
    get() = atTail && (world.tasks.isEmpty() || ending?.first == world.timeline.checkpoint())

  /** Encodes the current live history with the same decision stops as bundled replay files. */
  suspend fun replayFile(): String {
    check(atTail) { "return to the latest position before exporting" }
    val whole = world.recording()
    val positions =
        (listOf(whole.positions.first()) +
                agents.taskLog.selectionCheckpoints() +
                whole.positions.last())
            .distinct()
            .sortedBy { it.ordinal }
    return GameFileJson.encode(
        GameRecording(whole.premise, whole.events, positions),
        agents.taskLog.inputs(),
    )
  }

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
    return current().prompt
  }

  /** Whether the current action offer can name this played card and its printed action slot. */
  fun cardActionAvailable(card: String, slot: Int): Boolean {
    val key = listOf(world.timeline.checkpoint(), world.tasks.selectedTask(), form?.instruction)
    if (cardActionCheckKey != key) {
      cardActionCheckKey = key
      cardActionChecks.clear()
      cardActionSearch = null
    }
    return cardActionChecks.getOrPut(card to slot) { checkCardAction(card, slot) }
  }

  private var projectCheckKey: List<Any?>? = null
  private val projectChecks = mutableMapOf<String, Boolean>()
  private var projectSearch: DecisionSearch? = null

  /** A hand or Selecting card is clickable only after its exact play can finish this action. */
  fun projectCardAvailable(card: String): Boolean {
    val key = listOf(world.timeline.checkpoint(), world.tasks.selectedTask(), form?.instruction)
    if (projectCheckKey != key) {
      projectCheckKey = key
      projectChecks.clear()
      projectSearch = null
    }
    return projectChecks.getOrPut(card) {
      val task = projectTask() ?: return@getOrPut false
      if (card !in held(HAND) && card !in held(SELECTING)) return@getOrPut false
      if (!safeAutoexecEnabled) return@getOrPut true
      val draft = form?.instruction
      val search =
          projectSearch
              ?: DecisionSearch(
                      agents,
                      tracker,
                      budget = UI_SEARCH_BUDGET,
                      trace = searchTrace,
                      maxDuration = UI_SEARCH_DURATION,
                  )
                  .also { projectSearch = it }
      search.verdict { trial -> commitProjectChoice(trial, task.id, card, draft) } != DEAD_END
    }
  }

  /** Submits the clicked card as part of the same command that selects PlayProject. */
  fun playProjectCard(card: String) {
    if (!projectCardAvailable(card)) return
    val task = projectTask() ?: return
    val draft = form?.instruction
    form = null
    attempt { commitProjectChoice(agents, task.id, card, draft) }
  }

  private fun projectTask(): Task? {
    val shown = prompt()
    if (!shown.enabled) return null
    world.tasks.selectedTask()?.let(world.tasks::getTaskData)?.let { selected ->
      return selected.takeIf {
        shown.detail?.contains("PlayProject") == true ||
            shown.options.any { option ->
              option.enabled && actionName(option.label) == "PlayProject"
            }
      }
    }
    return shown.options
        .firstOrNull { option ->
          option.enabled &&
              (actionName(option.label) == "PlayProject" ||
                  option.arms.any { actionName(it) == "PlayProject" })
        }
        ?.taskId
        ?.let(world.tasks::getTaskData)
  }

  private fun commitProjectChoice(
      target: dev.martianzoo.agent.Agents,
      taskId: TaskId,
      card: String,
      narrowed: InstructionTree?,
  ) {
    val task = target.world.tasks.getTaskData(taskId)
    val draft =
        if (narrowed == null) target[task.assignee].fillInTask(taskId)
        else target[task.assignee].fillInTask(taskId, narrowed)
    if (
        draft.instruction.descendantsOfType<Change>().none {
          it.gaining?.className == cn("PlayProject")
        }
    ) {
      chooseFormOption(draft, "PlayProject")
    }
    val cardDecision = draft.decisions().singleOrNull() ?: error("No project card choice")
    val cardOption =
        draft.options(cardDecision).firstOrNull { actionName(it) == card }
            ?: error("$card is not an available project")
    draft.choose(cardDecision, cardOption)
    val source = requireNotNull(projectSource(draft.instruction)) { "No unique source for $card" }
    if (draft.decisions().singleOrNull()?.focus?.className == cn("CardLocation")) {
      chooseFormOption(draft, source)
    }
    draft.commit()
  }

  private fun checkCardAction(card: String, slot: Int): Boolean {
    val shown = prompt()
    if (!shown.enabled) return false
    val offersAction: (Prompt.Option) -> Boolean = { option ->
      option.enabled &&
          (actionName(option.label) == "UseCardAction" ||
              option.arms.any { actionName(it) == "UseCardAction" })
    }
    val offered = shown.options.any(offersAction)
    if (!offered && shown.detail?.contains("UseCardAction") != true) return false
    val task =
        world.tasks.selectedTask()?.let(world.tasks::getTaskData)
            ?: shown.options.firstOrNull(offersAction)?.taskId?.let(world.tasks::getTaskData)
            ?: return false
    return try {
      val narrowed =
          if (world.tasks.selectedTask() == null) null
          else form?.takeIf { it.taskId == task.id }?.instruction
      val draft =
          if (narrowed == null) agents[task.assignee].fillInTask(task.id)
          else agents[task.assignee].fillInTask(task.id, narrowed)
      if (offered) chooseFormOption(draft, "UseCardAction")
      val cardDecision = draft.decisions().singleOrNull() ?: return false
      val cardOption =
          draft.options(cardDecision).firstOrNull { actionName(it) == card } ?: return false
      draft.choose(cardDecision, cardOption)
      val slotDecision = draft.decisions().singleOrNull() ?: return false
      val slotOption =
          draft.options(slotDecision).firstOrNull { actionName(it) == "Action$slot" }
              ?: return false
      draft.choose(slotDecision, slotOption)
      val selectedInstruction = draft.instruction
      if (!safeAutoexecEnabled) return true
      val search =
          cardActionSearch
              ?: DecisionSearch(
                      agents,
                      tracker,
                      budget = UI_SEARCH_BUDGET,
                      trace = searchTrace,
                      maxDuration = UI_SEARCH_DURATION,
                  )
                  .also { cardActionSearch = it }
      search.verdict { trial ->
        if (trial.world.tasks.selectedTask() == null) trial[task.assignee].selectTask(task.id)
        val candidate = trial[task.assignee].fillInTask(task.id, selectedInstruction)
        candidate.commit()
      } != DEAD_END
    } catch (_: IllegalArgumentException) {
      false
    } catch (_: UnsupportedOperationException) {
      false
    }
  }

  /** The prompt at the tail, computed once for each state of the game and of draft input. */
  private fun current(): Described {
    val key =
        listOf(
            world.timeline.checkpoint(),
            form?.instruction,
            naming,
            naming?.chosen?.toList(),
            pendingCardTask,
            chosenCards.toList(),
            optionLimit,
            safeAutoexecEnabled,
        )
    described
        ?.takeIf { it.key == key }
        ?.let {
          return it
        }
    forcedChoice = null
    val prompt = describe()
    return Described(key, prompt, forcedChoice.takeIf { prompt.enabled }).also { described = it }
  }

  /** The action of the single choice able to finish the work under way, while describing it. */
  private var forcedChoice: (() -> Unit)? = null

  /** A search of the work under way from [lookaheadAt], or null without safe autoexecution. */
  private var lookahead: DecisionSearch? = null
  private var lookaheadAt: Checkpoint? = null
  private var cardActionCheckKey: List<Any?>? = null
  private val cardActionChecks = mutableMapOf<Pair<String, Int>, Boolean>()
  private var cardActionSearch: DecisionSearch? = null

  private fun proofScope(): Boolean =
      world.reader.getComponents(cn("TurnBasedPhase").expression).elements.isNotEmpty()

  private fun search(): DecisionSearch? {
    // Completion proofs concern a single corporation, Prelude, action, or final greenery play.
    if (!safeAutoexecEnabled || !atTail || !proofScope()) return null
    val here = world.timeline.checkpoint()
    if (lookaheadAt != here) {
      lookahead =
          DecisionSearch(
              agents,
              tracker,
              budget = UI_SEARCH_BUDGET,
              trace = searchTrace,
              maxDuration = UI_SEARCH_DURATION,
          )
      lookaheadAt = here
    }
    return lookahead
  }

  private fun describe(): Prompt = describeChoices()

  /**
   * Keeps the options a search does not prove dead, retaining each verdict for the UI. When only
   * one option remains, taking it makes no choice for the player even if completion is unproven.
   */
  private fun <T> prune(
      candidates: List<T>,
      verdict: (T) -> DecisionSearch.Verdict?,
      action: (T) -> () -> Unit,
      incomplete: () -> Unit,
  ): List<Pair<T, DecisionSearch.Verdict?>> {
    val live = candidates.mapNotNull { candidate ->
      val result = verdict(candidate)
      if (result == UNKNOWN) incomplete()
      if (result == DEAD_END) null else candidate to result
    }
    if (safeAutoexecEnabled && live.size == 1) {
      forcedChoice = action(live.single().first)
    }
    return live
  }

  private fun describeChoices(): Prompt {
    naming?.let { open ->
      val request = open.request
      val remaining = request.count - open.chosen.size
      val verb = if (request.arriving) "moves into" else "leaves"
      val unproven = safeAutoexecEnabled && proofScope()
      return Prompt(
          "Which ${request.back} $verb ${request.location}? ($remaining more)",
          "Event ${request.event.ordinal}: ${request.event.change}",
          (request.candidates.orEmpty() - open.chosen.toSet()).map {
            Prompt.Option(it.toString(), held = true, unproven = unproven)
          },
          retreat = "CANCEL ATTEMPT",
          warning = if (unproven) SEARCH_INCOMPLETE else null,
      )
    }
    nextAdminTask()?.let { task ->
      val option = Prompt.Option(label(task), who = ADMIN.toString(), taskId = task.id)
      // Admin's eager work is no Player choice.
      if (safeAutoexecEnabled) forcedChoice = { choose(option) }
      return Prompt("Continue automatic work", options = listOf(option))
    }
    val selected = world.tasks.selectedTask()?.let(world.tasks::getTaskData)
    pendingCardTask?.let { taskId ->
      val task = world.tasks.getTaskData(taskId)
      cardChoice(task)?.let {
        return cardPrompt(task, it)
      }
    }
    if (selected != null) {
      if (selected.assignee == ADMIN) return waitingForAdmin(listOf(selected))
      cardChoice(selected)?.let {
        return cardPrompt(selected, it)
      }
      return narrowing(selected)
    }
    val pending = pendingTasks()
    if (pending.isEmpty()) {
      val adminTasks = agents[ADMIN].tasks.extract { it }.filterNot { it.deferred }
      if (adminTasks.isNotEmpty()) return waitingForAdmin(adminTasks)
      val phase = world.reader.getComponents(cn("Phase").expression).elements.singleOrNull()
      return Prompt(
          if (world.tasks.isEmpty()) "Game ended" else "Only deferred work remains",
          buildList {
                phase?.let { add("Phase: $it") }
                if (agents[player].count("Victory") > 0) add("Victory recorded")
                if (
                    world.classTable.findClass(cn("VictoryPhase")) != null &&
                        agents[ADMIN].count("VictoryPhase") > 0
                ) {
                  add("Score: ${agents[player].count("VictoryPoint")} VP")
                }
              }
              .joinToString(" · "),
      )
    }
    var incompleteSearch = false
    val previews = pending.map { task ->
      taskOption(world, task) { selected ->
        val form = agents[selected.assignee].fillInTask(selected.id)
        val decision =
            form.decisions().singleOrNull()?.takeIf { it.kind == Decision.Kind.ALTERNATIVE }
                ?: return@taskOption emptyList()
        val arms = form.options(decision).toList()
        val standard = standardActionArm(form, arms) ?: return@taskOption arms
        form.choose(decision, standard)
        val action = form.decisions().singleOrNull() ?: return@taskOption arms
        form.options(action).toList() + arms.filterNot { it == standard }
      }
    }
    // Only selectable tasks are choices now; the others stay visible for reference.
    val selectable = previews.filter { it.enabled }
    val live =
        prune(
            selectable,
            { option ->
              val task = world.tasks.getTaskData(requireNotNull(option.taskId))
              search()?.verdict { it[task.assignee].selectTask(task.id) }
            },
            { option -> { choose(option) } },
        ) {
          incompleteSearch = true
        }
    val options = previews.mapNotNull { option ->
      val result = live.firstOrNull { it.first == option }?.second
      if (option.enabled && result == null && search() != null) return@mapNotNull null
      if (option.arms.isEmpty()) return@mapNotNull option.copy(unproven = result == UNKNOWN)
      val taskId = requireNotNull(option.taskId)
      val arms =
          option.arms.mapNotNull { arm ->
            val armResult =
                search()?.let { trial ->
                  val draft = agents[world.tasks.getTaskData(taskId).assignee].fillInTask(taskId)
                  chooseFormOption(draft, arm)
                  trial.verdictForm(taskId, draft.instruction)
                }
            if (armResult == UNKNOWN) incompleteSearch = true
            if (armResult == DEAD_END) null else arm to armResult
          }
      option.copy(
          arms = arms.map { it.first },
          unproven = result == UNKNOWN,
          unprovenArms = arms.filter { it.second == UNKNOWN }.mapTo(linkedSetOf()) { it.first },
      )
    }
    return Prompt(
        if (selectable.isNotEmpty() && live.isEmpty() && search() != null)
            "No choice can complete this action"
        else "Choose a task",
        options = options,
        warning = if (incompleteSearch) SEARCH_INCOMPLETE else null,
    )
  }

  /** A displayed standard action follows the same two choices as its unexpanded turn offer. */
  private fun chooseFormOption(form: TaskForm, option: String) {
    // Narrowing a named project can itself fill its unique card location in the trial form.
    if (
        form.decisions().isEmpty() &&
            option in setOf("Hand", "Selecting", "Revealed") &&
            form.instruction.descendantsOfType<Change>().any {
              it.gaining?.className == cn("PlayProject")
            } &&
            cn(option) in form.instruction.descendantsOfType<ClassName>()
    ) {
      return
    }
    val decision =
        form.decisions().singleOrNull()
            ?: throw NarrowingException("$option is not available in this task")
    if (decision.kind == Decision.Kind.ALTERNATIVE) {
      val available = form.options(decision).toList()
      if (option !in available) {
        form.choose(decision, requireNotNull(standardActionArm(form, available)))
      }
    }
    val next =
        form.decisions().singleOrNull()
            ?: throw NarrowingException(
                "$option is not available after choosing the standard action"
            )
    form.choose(next, option)
  }

  /** The exact two-arm turn offer; other OR tasks keep their usual presentation. */
  private fun standardActionArm(form: TaskForm, available: List<String>): String? {
    val alternatives = (form.instruction as? Or)?.instructions ?: return null
    if (alternatives.size != 2) return null
    val changes = alternatives.map { it as? Change ?: return null }
    if (changes.none { it.gaining?.className in setOf(cn("Pass"), cn("PassBaton")) }) return null
    val standard = changes.singleOrNull { it.gaining?.className == cn("StandardAction") }
    return standard?.toString()?.takeIf { it in available }
  }

  private fun waitingForAdmin(tasks: List<Task>): Prompt =
      Prompt(
          "Admin work needs automatic handling",
          tasks.joinToString("\n") { label(it) },
          enabled = false,
      )

  private fun cardChoice(task: Task): CardChoice? {
    if (task.assignee != player) return null
    val change = task.instruction as? Change ?: return null
    val source = change.removing ?: return null
    val reader = world.reader
    val back = reader.resolve(source).rootClass
    if (!back.isSubtypeOf(world.classTable.getClass(cn("CardBack")))) return null
    val location =
        reader
            .resolve(source)
            .expressionFull
            .arguments
            .map { reader.resolve(it).className }
            .firstOrNull { it == SELECTING || it == HAND } ?: return null
    val drawing = change.gaining != null
    if (
        drawing &&
            (location != SELECTING ||
                reader.resolve(change.gaining!!).expressionFull.arguments.none {
                  reader.resolve(it).className == HAND
                })
    ) {
      return null
    }
    val count = change.count as? ActualScalar
    return CardChoice(
        back.className,
        location,
        drawing,
        count?.value?.takeIf { change.quantifier == MANDATORY },
        count?.value?.takeIf { change.quantifier != MANDATORY },
    )
  }

  /** Whether performing [task] asks the Player which named cards leave Hand or Selecting. */
  private fun choosesCards(task: Task): Boolean {
    val back = world.classTable.getClass(cn("CardBack"))
    return task.instruction.descendantsOfType<Change>().any { change ->
      change.removing?.let { removing ->
        runCatching { world.reader.resolve(removing).rootClass.isSubtypeOf(back) }
            .getOrDefault(true)
      } == true
    }
  }

  private fun cardPrompt(task: Task, choice: CardChoice): Prompt {
    val available = tracker.hand(player, choice.back, choice.source).mapTo(linkedSetOf()) { "$it" }
    val chosen = chosenCards.mapTo(linkedSetOf()) { "$it" }
    val count = chosen.size
    var incompleteSearch = false
    val floor = choice.required ?: if (choice.maximum != null) 0 else 1
    val ceiling = choice.required ?: choice.maximum ?: available.size
    fun allowedSize(size: Int) = size in floor..minOf(ceiling, available.size)
    // The search keeps cards anonymous, so only how many cards move can matter to completion;
    // which cards is the Player's choice. A size is proved impossible only by the search.
    val verdicts = mutableMapOf<Int, DecisionSearch.Verdict?>()
    fun verdict(size: Int): DecisionSearch.Verdict? =
        verdicts.getOrPut(size) {
          search()?.verdict {
            val form = it[player].fillInTask(task.id)
            form.decisions().singleOrNull()?.let { decision ->
              require(decision.kind == Decision.Kind.AMOUNT)
              form.choose(decision, if (size == 0) "Ok" else size.toString())
            }
            form.commit()
          }
        }
    val sizes = floor..minOf(ceiling, available.size)
    /** The best verdict among allowed sizes of at least [size]. */
    fun reachable(size: Int): DecisionSearch.Verdict? {
      if (search() == null) return null
      val found = (maxOf(size, floor)..sizes.last).map(::verdict)
      return when {
        COMPLETES in found -> COMPLETES
        found.any { it != DEAD_END } -> UNKNOWN
        else -> DEAD_END
      }
    }
    val confirmVerdict =
        if (chosen.all { it in available } && allowedSize(count)) verdict(count) else DEAD_END
    if (confirmVerdict == UNKNOWN) incompleteSearch = true
    val canConfirm = confirmVerdict != DEAD_END
    val more = if (count < ceiling) reachable(count + 1) else DEAD_END
    if (more == UNKNOWN) incompleteSearch = true
    val selectable = available.filterTo(linkedSetOf()) { it in chosen || more != DEAD_END }
    val unproven =
        if (more == UNKNOWN) (selectable - chosen).toCollection(linkedSetOf()) else linkedSetOf()
    if (safeAutoexecEnabled && chosen.isEmpty()) {
      // A selection is forced when one size alone can finish and it names no particular card.
      val possible = sizes.filter { verdict(it) != DEAD_END }
      possible
          .singleOrNull()
          ?.takeIf { it == 0 || it == available.size }
          ?.let { size ->
            forcedChoice = {
              if (size > 0) available.forEach(::toggleCard)
              confirmCards()
            }
          }
    }
    return Prompt(
        if (choice.drawing) "Choose cards to draw" else "Choose cards to discard",
        "Task ${task.id.ordinal} (${task.assignee}): ${task.instruction}. Select cards in ${choice.source}, then confirm here.",
        retreat = if (chosen.isNotEmpty()) "CLEAR SELECTION" else "CANCEL CARD CHOICE",
        cards =
            Prompt.Cards(
                choice.drawing,
                choice.source == HAND,
                selectable,
                chosen,
                canConfirm,
                unproven,
                confirmVerdict == UNKNOWN,
            ),
        warning = if (incompleteSearch) SEARCH_INCOMPLETE else null,
    )
  }

  fun toggleCard(name: String) {
    val taskId = pendingCardTask ?: world.tasks.selectedTask() ?: return
    val task = world.tasks.getTaskData(taskId)
    val choice = cardChoice(task) ?: return
    if (name !in cardPrompt(task, choice).cards!!.available) return
    val card = cn(name)
    if (!chosenCards.add(card)) chosenCards.remove(card)
    problem = null
  }

  fun confirmCards() {
    val taskId = pendingCardTask ?: world.tasks.selectedTask() ?: return
    val task = world.tasks.getTaskData(taskId)
    val choice = cardChoice(task) ?: return
    if (!cardPrompt(task, choice).cards!!.canConfirm) return
    val names = chosenCards.toList()
    val form = formFor(task)
    try {
      form.decisions().singleOrNull()?.let { decision ->
        require(decision.kind == Decision.Kind.AMOUNT)
        form.choose(decision, if (names.isEmpty()) "Ok" else names.size.toString())
      }
      require(form.decisions().isEmpty()) { "This card task needs another choice" }
    } catch (refused: Exception) {
      this.form = null
      problem = refused.message ?: refused.toString()
      return
    }
    chosenCards.clear()
    pendingCardTask = null
    this.form = null
    attempt(choice, names) { form.commit() }
    settle()
  }

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
      val decision = form.decisions().singleOrNull()
      if (decision == null) {
        val execute = Prompt.Option("EXECUTE")
        val verdict = search()?.verdictForm(task.id, instruction)
        if (safeAutoexecEnabled && verdict != DEAD_END) forcedChoice = { choose(execute) }
        return if (verdict == DEAD_END)
            Prompt("This task cannot complete the action", detail, retreat = retreat)
        else
            Prompt(
                "Execute this task",
                detail,
                listOf(execute.copy(unproven = verdict == UNKNOWN)),
                retreat = retreat,
                warning = if (verdict == UNKNOWN) SEARCH_INCOMPLETE else null,
            )
      }
      val heldNames = (held(HAND) + held(SELECTING)).toSet()
      val playable = cardChoices(instruction, decision)
      val projectSource =
          if (decision.focus?.className == cn("CardLocation")) projectSource(instruction) else null
      var incompleteSearch = false
      val alternatives =
          if (decision.kind == Decision.Kind.ALTERNATIVE) form.options(decision).toList()
          else emptyList()
      val standard = standardActionArm(form, alternatives)
      val choices =
          if (standard == null) form.options(decision)
          else {
            val draft = agents[task.assignee].fillInTask(task.id, instruction)
            draft.choose(draft.decisions().single(), standard)
            (draft.options(draft.decisions().single()).toList() +
                    alternatives.filterNot { it == standard })
                .asSequence()
          }
      val listed =
          choices
              .filter { projectSource == null || it == projectSource }
              .filter { playable == null || it == "Ok" || it in playable }
              .take(optionLimit + 1)
              .toList()
      // A numeric choice can have many locally valid values. Listing them must not run a full
      // completion search on the browser thread; the Player can choose any listed value.
      val opensAmount =
          decision.kind == Decision.Kind.ALTERNATIVE &&
              alternatives.any { option ->
                runCatching {
                      val draft = agents[task.assignee].fillInTask(task.id, instruction)
                      draft.choose(draft.decisions().single(), option)
                      draft.decisions().singleOrNull()?.kind == Decision.Kind.AMOUNT
                    }
                    .getOrDefault(true)
              }
      if (decision.kind == Decision.Kind.AMOUNT || opensAmount) {
        return Prompt(
            heading(decision),
            detail,
            listed.take(optionLimit).map { Prompt.Option(it, held = it in heldNames) },
            listed.size > optionLimit,
            retreat,
        )
      }
      val window =
          prune(
              listed,
              { option ->
                val trial = search()
                if (trial == null) null
                else {
                  val draft = agents[task.assignee].fillInTask(task.id, instruction)
                  chooseFormOption(draft, option)
                  trial.verdictForm(task.id, draft.instruction)
                }
              },
              { option -> { choose(Prompt.Option(option)) } },
          ) {
            incompleteSearch = true
          }
      // The last listed option is a sentinel: more options may exist even if it was pruned.
      if (listed.size > optionLimit) forcedChoice = null
      val shown =
          window.take(optionLimit).map { (option, verdict) ->
            Prompt.Option(option, held = option in heldNames, unproven = verdict == UNKNOWN)
          }
      if (projectSource != null && shown.any { it.label == projectSource }) {
        forcedChoice = { choose(Prompt.Option(projectSource)) }
      }
      val ordered =
          if (decision.kind == Decision.Kind.TARGET) shown.sortedByDescending { it.held } else shown
      Prompt(
          if (shown.isEmpty() && listed.size > optionLimit) heading(decision)
          else if (shown.isEmpty() && search() != null) "No choice can complete this action"
          else if (shown.isEmpty())
              "No choices available; use history and TRUNCATE to undo this task"
          else heading(decision),
          detail,
          ordered,
          listed.size > optionLimit || window.size > optionLimit,
          retreat,
          warning = if (incompleteSearch) SEARCH_INCOMPLETE else null,
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
  private fun cardChoices(instruction: InstructionTree, decision: Decision): Set<String>? =
      tracker.playableFaces(player, instruction, decision.focus)?.mapTo(linkedSetOf()) { "$it" }

  /** The unique location containing the named project in this action, if known. */
  private fun projectSource(instruction: InstructionTree): String? {
    val project =
        instruction
            .descendantsOfType<Change>()
            .firstOrNull {
              it.gaining?.className == cn("PlayProject")
            }
            ?.gaining ?: return null
    val face =
        world.reader
            .resolve(project)
            .expressionFull
            .arguments
            .map(world.reader::resolve)
            .mapNotNull { it.groundType.representedClass }
            .firstOrNull { it.isSubtypeOf(world.classTable.getClass(cn("CardFront"))) }
            ?.className ?: return null
    val sources = buildList {
      if (face in tracker.hand(player, cn("ProjectCard"), HAND)) add("Hand")
      if (face in tracker.hand(player, cn("ProjectCard"), SELECTING)) add("Selecting")
      if (
          isEmpty() &&
              face in
                  tracker.playableFaces(player, instruction, cn("CardFront").expression).orEmpty()
      )
          add("Revealed")
    }
    return sources.singleOrNull()
  }

  private fun heading(decision: Decision): String =
      when (decision.kind) {
        Decision.Kind.ALTERNATIVE -> "Choose an alternative"
        Decision.Kind.TARGET -> "Narrow ${decision.focus}"
        Decision.Kind.AMOUNT -> "Choose an amount"
      }

  /** Acts on [option] of the current prompt, then settles whatever cannot be a choice. */
  fun choose(option: Prompt.Option) {
    chooseOnly(option)
    settle()
  }

  private fun chooseOnly(option: Prompt.Option) {
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
      chosenCards.clear()
      if (cardChoice(task) != null) {
        pendingCardTask = task.id
        return
      }
      attempt { agents[task.assignee].selectTask(task.id) }
      return
    }
    check(selected.assignee == player) { "Admin tasks are handled automatically" }
    val form = formFor(selected)
    val decision = form.decisions().singleOrNull()
    if (decision != null) {
      try {
        chooseFormOption(form, label)
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
    chooseOnly(option)
    if (problem == null) chooseOnly(Prompt.Option(arm))
    settle()
  }

  /** Lists more options of a long decision. */
  fun showMore() {
    optionLimit += OPTION_PAGE
  }

  /** Discards draft choices, or the open naming attempt and every name it supplied. */
  fun retreat() {
    problem = null
    chosenCards.clear()
    if (pendingCardTask != null) {
      pendingCardTask = null
      form = null
      return
    }
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
    pendingCardTask = null
    chosenCards.clear()
    optionLimit = OPTION_PAGE
    problem = null
    described = null
    lookahead = null
    lookaheadAt = null
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
    pendingCardTask = null
    chosenCards.clear()
    naming = null
    problem = null
    described = null
    lookahead = null
    lookaheadAt = null
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

  /** Take queue order only when a short exhaustive trial proves every continuation equivalent. */
  private fun provenFifoTask(): Task? {
    if (world.tasks.selectedTask() != null || pendingCardTask != null) return null
    val pending = pendingTasks()
    if (pending.size !in 2..3) return null
    val first = pending.first()
    if (first.instruction.isAbstract(world.reader) || choosesCards(first)) return null
    val trial =
        DecisionSearch(
            agents,
            tracker,
            budget = UI_SEARCH_BUDGET,
            trace = searchTrace,
            maxDuration = UI_SEARCH_DURATION,
        )
    return first.takeIf { trial.uniformOutcome() != null }
  }

  private fun formFor(task: Task): TaskForm =
      form?.takeIf { it.taskId == task.id }
          ?: agents[task.assignee].fillInTask(task.id).also { form = it }

  private fun attempt(
      cards: CardChoice? = null,
      chosen: List<ClassName> = emptyList(),
      command: () -> Unit,
  ) {
    check(atTail && naming == null) { "no command can start now" }
    tracker.requireComplete()
    run(Attempt(command, dealt.last().second.toMutableMap(), cards, chosen))
    settle()
  }

  /**
   * Uses the selected project's known card source, then, with safe autoexecution, performs safe
   * Player tasks and proven forced choices until a real choice, naming request, or problem remains.
   */
  private fun settle() {
    if (settling) return
    // A named project's source is determined by where that card is held.
    if (
        atTail &&
            naming == null &&
            problem == null &&
            ending == null &&
            world.tasks.selectedTask() != null
    ) {
      val next = prompt()
      if (next.heading == "Narrow CardLocation") {
        val source = form?.instruction?.let(::projectSource)
        next.options.firstOrNull { it.label == source && it.enabled }?.let(::chooseOnly)
      }
    }
    if (!safeAutoexecEnabled) return
    settling = true
    val started = TimeSource.Monotonic.markNow()
    try {
      repeat(MAX_SETTLE_STEPS) {
        // A fresh search can start on every step; its individual deadline is not a deadline for
        // this whole automatic walk. Leave remaining work for the Player when the walk runs long.
        if (started.elapsedNow() >= MAX_SETTLE_DURATION) return
        if (!atTail || naming != null || problem != null || ending != null) return
        val before =
            listOf(
                world.timeline.checkpoint(),
                form?.instruction,
                pendingCardTask,
                chosenCards.toList(),
            )
        // A card leaving Hand or Selecting is the Player's choice of which card; only draws are
        // dealt here.
        val safe =
            pendingTasks().firstOrNull {
              world.tasks.selectedTask() == null &&
                  !choosesCards(it) &&
                  TfmAutoExec.safeTask(world, it)
            }
        if (safe != null) {
          attempt { agents[player].doTask(safe.id) }
        } else {
          val fifo = provenFifoTask()
          if (fifo != null) attempt { agents[player].doTask(fifo.id) }
          else {
            val forced = current().forced ?: return
            forced()
          }
        }
        val after =
            listOf(
                world.timeline.checkpoint(),
                form?.instruction,
                pendingCardTask,
                chosenCards.toList(),
            )
        if (after == before) return
      }
    } finally {
      settling = false
    }
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
            form = form?.takeIf { draft ->
              draft.taskId == world.tasks.selectedTask() &&
                  runCatching { draft.instruction }.isSuccess
            }
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
        val cards = attempt.cards
        when {
          cards != null &&
              request.player == player &&
              request.back == cards.back &&
              request.count == attempt.chosen.size &&
              ((request.location == cards.source && !request.arriving) ||
                  (cards.drawing && request.location == HAND && request.arriving)) -> {
            require(candidates == null || candidates.containsAll(attempt.chosen)) {
              "The selected cards are no longer available"
            }
            supply(request, attempt.chosen, attempt)
          }
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
          // Canon's Admin removals from Selecting are cleanup. The Player has already chosen
          // which cards to keep, so the remaining identities are not another Player decision.
          request.event.actor == ADMIN && !request.arriving && request.location == SELECTING ->
              supply(request, candidates.take(request.count), attempt)
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
    attempt.named.getOrPut(Triple(request.back, request.location, request.arriving)) {
      mutableListOf()
    } += cards
  }

  /** Forgets every name [attempt] supplied; its provisional deals were never recorded. */
  private fun abandon(attempt: Attempt) {
    for ((back, location, arriving) in attempt.named.keys) {
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
    const val UI_SEARCH_BUDGET = 2_000
    val UI_SEARCH_DURATION = 500.milliseconds
    val MAX_SETTLE_DURATION = 1.seconds
    const val MAX_SETTLE_STEPS = 200
    const val MAX_CARD_SELECTIONS = 256
    const val MAX_FORCED_SELECTIONS = 16L

    fun combinationCount(n: Int, k: Int): Long {
      var result = 1L
      for (i in 0 until k) result = result * (n - i) / (i + 1)
      return result
    }

    const val SEARCH_INCOMPLETE = "* means no completion was proved; the choice remains available."

    fun combinations(names: List<String>, count: Int): Sequence<List<String>> = sequence {
      if (count == 0) {
        yield(emptyList())
      } else if (names.size >= count) {
        for (index in 0..names.size - count) {
          for (tail in combinations(names.drop(index + 1), count - 1)) {
            yield(listOf(names[index]) + tail)
          }
        }
      }
    }
  }
}

/** Replays a saved live game, then restores its card names and deck counters for further play. */
internal suspend fun recoverPlaySession(saved: PlaySession.Recovery): PlaySession {
  val document = GameFileJson.parse(saved.gameFile)
  lateinit var premise: GamePremise
  val agents =
      GameFileImporter.replay(
          document,
          Canon,
          newAgents = { resolved ->
            premise = resolved
            TfmEngine.agents(TfmEngine.newGame(resolved)).also { session ->
              session.taskLog.start()
              session.taskLog.gameStarted()
            }
          },
          startGame = { it[ADMIN].beginOperation("SetupPhase FROM BootstrapPhase") },
      )
  return PlaySession(premise, saved.seed, agents, saved)
}

/** Card notes in the live log name the exact historical moves; later moves use normal tracking. */
private fun recoverCardTracking(world: World): CardTracking {
  val end = world.timeline.checkpoint()
  world.timeline.seek(world.timeline.positions.first())
  val tracker = CardTracking.start(world, requireSelectingNames = true)
  world.timeline.seek(end)
  val player = world.actors.filterIsInstance<Player>().single()
  repeat(world.events.entriesSince(Checkpoint(0)).size + 1) {
    try {
      tracker.hand(player)
      return tracker
    } catch (request: NamesRequiredException) {
      val note =
          requireNotNull(
              request.event.notes?.lineSequence()?.firstOrNull { it.startsWith("Cards: ") }
          ) {
            "Saved card names are missing at event ${request.event.ordinal}"
          }
      val names = note.removePrefix("Cards: ").split(", ").map(::cn).toTypedArray()
      require(names.size >= request.count) {
        "Saved card names are incomplete at event ${request.event.ordinal}"
      }
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
    }
  }
  error("Saved card names could not be restored")
}
