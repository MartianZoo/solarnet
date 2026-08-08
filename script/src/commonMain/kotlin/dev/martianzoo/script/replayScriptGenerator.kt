package dev.martianzoo.script

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.data.GameEvent
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskEditedEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Task
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

/** Generates a script that reconstructs [source] through the ordinary automatic game workflow. */
public fun generateReplayScript(source: World): String {
  val sourceEvents = source.events.entriesSinceSetup()
  val lines = mutableListOf(setupCommand(source))
  val replay = ScriptSession(source.vocabulary.locale)
  executeRequired(replay, lines.single())

  while (true) {
    val replayEvents = replay.game.events.entriesSinceSetup()
    requireMatchingPrefix(sourceEvents, replayEvents, lines)
    if (replayEvents.size == sourceEvents.size) break

    val pending = replay.game.tasks.extract { it }.associateBy { it.id }
    check(pending.isNotEmpty()) {
      "Replay stopped at event ${replayEvents.size} without a pending task"
    }
    val resolution = nextResolution(sourceEvents, replayEvents.size, pending)
    val playLine = executeTfmPlay(replay, sourceEvents, replayEvents.size, resolution)
    if (playLine != null) {
      lines += playLine
      continue
    }
    val taskPrefix =
        if (resolution.byId) {
          lines += "tasks"
          executeRequired(replay, "tasks")
          replay.selectableTasks().single { (_, task) -> task.id == resolution.task.id }.first + " "
        } else {
          ""
        }
    val taskLine =
        "as ${resolution.task.assignee} task $taskPrefix" +
            (resolution.instruction
                ?: source.vocabulary.renderPets(resolution.task.instruction).replace("!", ""))
    lines += taskLine
    val beforeTask = replay.game.events.entriesSinceSetup().size
    val output = executeRequired(replay, taskLine)
    check(replay.game.events.entriesSinceSetup().size > beforeTask) {
      "Generated command made no progress: `$taskLine`\n${output.joinToString("\n")}" +
          "\nNext source events:\n" +
          sourceEvents.drop(replayEvents.size).take(20).joinToString("\n")
    }
  }

  return lines.joinToString(separator = "\n", postfix = "\n")
}

private fun executeTfmPlay(
    replay: ScriptSession,
    sourceEvents: List<GameEvent>,
    replaySize: Int,
    resolution: Resolution,
): String? {
  val choosingPlayCard = resolution.task.instruction.isPlayCardStandardAction()
  val directlyPlayingCard = resolution.task.instruction.cardName() != null
  if (!choosingPlayCard && !directlyPlayingCard) return null
  if (
      directlyPlayingCard &&
          replay.game.tasks.matching { it.instruction.toString().contains("StandardAction") }.any()
  ) {
    return null
  }

  val cardName =
      resolution.task.instruction.cardName()
          ?: sourceEvents.drop(replaySize).firstNotNullOfOrNull { event ->
            (event as? TaskEditedEvent)?.task?.instruction?.cardName()
                ?: (event as? TaskRemovedEvent)?.task?.instruction?.cardName()
          }
          ?: return null
  val paymentTasks = paymentTasksForPlay(sourceEvents, replaySize, cardName)
  val payment = sourcePayment(sourceEvents, replaySize, paymentTasks)
  check(payment != null) {
    "Could not express $cardName payment through tfm_play: ${paymentTasks.values}"
  }
  val inlinePayments = payment.payments.takeUnless { payment.hasUnusedAccept }
  val cardText = replay.game.vocabulary.renderPets(cardName.expression)
  val command =
      "as ${resolution.task.assignee} tfm_play $cardText" +
          inlinePayments?.takeIf { it.isNotEmpty() }?.joinToString(prefix = ", ").orEmpty()
  executeRequired(replay, command)
  return command
}

private fun paymentTasksForPlay(
    sourceEvents: List<GameEvent>,
    replaySize: Int,
    cardName: ClassName,
): Map<TaskId, Task> {
  val future = sourceEvents.drop(replaySize)
  val playIndex = future.indexOfFirst { candidate ->
    val event = candidate as? ChangeEvent ?: return@indexOfFirst false
    event.change.gaining?.let { expression ->
      expression.className == cn("PlayCard") &&
          expression.arguments
              .lastOrNull { it.className == CLASS && it.arguments.size == 1 }
              ?.arguments
              ?.single()
              ?.className == cardName
    } == true
  }
  check(playIndex >= 0) { "No PlayCard change found for $cardName after source event $replaySize" }
  val removalIndex = future.indexOfFirst { candidate ->
    val event = candidate as? TaskRemovedEvent ?: return@indexOfFirst false
    event.task.instruction.cardName() == cardName
  }
  check(removalIndex >= playIndex) { "No PlayCard task removal found for $cardName" }
  return future
      .subList(playIndex, removalIndex + 1)
      .filterIsInstance<TaskAddedEvent>()
      .filter { event -> event.task.cause?.context?.className == cn("Accept") }
      .associate { it.task.id to it.task }
}

private data class Payment(val payments: List<String>, val hasUnusedAccept: Boolean)

private fun sourcePayment(
    sourceEvents: List<GameEvent>,
    replaySize: Int,
    pending: Map<TaskId, Task>,
): Payment? {
  if (pending.isEmpty()) return Payment(emptyList(), false)
  val current = pending.mapValuesTo(mutableMapOf()) { (_, task) -> task.instruction }
  val payments = mutableListOf<String>()
  var hasUnusedAccept = false
  for (event in sourceEvents.drop(replaySize)) {
    when (event) {
      is TaskEditedEvent ->
          if (event.task.id in current) current[event.task.id] = event.task.instruction
      is TaskRemovedEvent -> {
        val instruction = current.remove(event.task.id) ?: continue
        when (instruction) {
          NoOp -> hasUnusedAccept = true
          is Transmute -> {
            val payment = instruction.paymentText()
            if (payment == null) hasUnusedAccept = true else payments += payment
          }
          else -> error("Unsupported tfm_play payment resolution: $instruction")
        }
        if (current.isEmpty()) return Payment(payments, hasUnusedAccept)
      }
      else -> Unit
    }
  }
  return null
}

private fun Instruction.cardName() =
    ((this as? Gain)?.gaining)
        ?.takeIf { it.className == cn("PlayCard") }
        ?.arguments
        ?.filter { it.className == CLASS && it.arguments.size == 1 }
        ?.takeIf { it.size >= 2 }
        ?.last()
        ?.arguments
        ?.single()
        ?.className

private fun Instruction.isPlayCardStandardAction(): Boolean =
    (this as? Gain)?.gaining?.let { expression ->
      expression.className == cn("UseAction1") &&
          expression.arguments.any { it.className == cn("PlayCardSA") }
    } == true

private fun Transmute.paymentText(): String? {
  if (fromEx.toExpression.className != cn("Pay")) return null
  val count = (scalar as? ActualScalar)?.value ?: return null
  return "$count ${fromEx.fromExpression.className}"
}

private fun setupCommand(source: World): String {
  val configurations = source.reader.getComponents("GameConfiguration")
  val activeOptions = configurations.mapTo(linkedSetOf()) { it.className }
  val exclusions = buildList {
    if (cn("TerraformingMars") in activeOptions && cn("CorporateEraExpansion") !in activeOptions) {
      add("Exclude<Class<CorporateEraExpansion>>")
    }
    if (
        cn("VenusNextExpansion") in activeOptions && cn("WorldGovernmentOption") !in activeOptions
    ) {
      add("Exclude<Class<WorldGovernmentOption>>")
    }
    if (
        cn("SoloMode") in activeOptions &&
            activeOptions.none { it.toString().endsWith("SoloVariant") }
    ) {
      add("Exclude<Class<StandardSoloVariant>>")
    }
  }
  val players = source.reader.getComponents("Player").size
  val contributedDefaults =
      setOfNotNull(
          cn("CorporateEraExpansion").takeIf { cn("TerraformingMars") in activeOptions },
          cn("WorldGovernmentOption").takeIf { cn("VenusNextExpansion") in activeOptions },
          cn("StandardSoloVariant").takeIf { cn("SoloMode") in activeOptions },
      )
  val roots =
      listOf("$players Player") +
          configurations.elements
              .filterNot { it.className in contributedDefaults }
              .map { source.vocabulary.renderPets(it.expression) }
  val instruction = (exclusions + roots).joinToString()
  return "newgame \"$instruction\" purple"
}

private data class Resolution(
    val task: Task,
    val instruction: String? = null,
    val byId: Boolean = false,
)

private fun nextResolution(
    sourceEvents: List<GameEvent>,
    replaySize: Int,
    pending: Map<TaskId, Task>,
): Resolution {
  var selected: Task? = null
  var revised: Task? = null
  val addedBeforeChange = mutableListOf<Task>()
  for (event in sourceEvents.drop(replaySize)) {
    when (event) {
      is TaskAddedEvent -> if (selected == null) addedBeforeChange += event.task
      is TaskEditedEvent -> {
        if (selected == null && event.task.id in pending) selected = pending.getValue(event.task.id)
        if (event.task.id == selected?.id && event.task.instruction != selected?.instruction) {
          revised = event.task
        }
      }
      is TaskRemovedEvent -> {
        if (selected == null && event.task.id in pending) {
          if (addedBeforeChange.isNotEmpty()) {
            val selectedInstruction = addedBeforeChange.joinToString {
              it.instruction.toString().replace("!", "")
            }
            return Resolution(pending.getValue(event.task.id), selectedInstruction)
          }
          return Resolution(pending.getValue(event.task.id), "Ok", byId = true)
        }
        if (event.task.id == selected?.id) return Resolution(revised ?: selected!!)
      }
      is ChangeEvent -> {
        addedBeforeChange.clear()
        if (selected != null && revised != null) return Resolution(revised)
      }
    }
  }
  error("Source history never resolves any of the replay's pending tasks: ${pending.keys}")
}

private fun requireMatchingPrefix(
    source: List<GameEvent>,
    replay: List<GameEvent>,
    lines: List<String>,
) {
  val mismatch = replay.indices.firstOrNull { it >= source.size || replay[it] != source[it] }
  check(mismatch == null) {
    buildString {
      append("Generated script diverged at event $mismatch after `${lines.last()}`")
      append("\nsource: ").append(source.getOrNull(mismatch!!))
      append("\nreplay: ").append(replay.getOrNull(mismatch))
    }
  }
}

private fun executeRequired(session: ScriptSession, command: String): List<String> {
  val output = session.command(command)
  check(output.none { it.startsWith("Error:") || it.startsWith("¯") }) {
    "Generated command failed: `$command`\n${output.joinToString("\n")}"
  }
  return output
}
