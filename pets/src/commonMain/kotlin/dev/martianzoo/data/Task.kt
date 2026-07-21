package dev.martianzoo.data

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.SystemClasses.DIE
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Companion.split
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.InstructionGroup
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform

public data class Task(
    /**
     * Identifies this task within a game at a particular point in time. These do get reused (for
     * user convenience) but of course no two have the same id at the same time.
     */
    val id: TaskId,

    /** Whose pending-work queue contains this task and whose scoped gameplay may revise it. */
    val assignee: Actor,

    /** If true, no game state may be modified until this task is completed. */
    val next: Boolean = false,

    /**
     * What to do. Can be abstract. If so, it will have to be revised to something narrower and
     * concrete before it is executed. Normalized to [instruction].
     */
    private val instructionIn: Instruction,

    /**
     * Any instruction that should be automatically enqueued when this task is removed (a [Multi] is
     * split). Used for `THEN` instructions. Normalized to [then].
     */
    private val thenIn: Instruction? = null,

    /** Why was this task born? */
    val cause: Cause?,

    /** Why is the task still here? */
    val whyPending: String? = null,
) {

  /** Normalized form of [instructionIn]. */
  val instruction = normalizeForTask(instructionIn)

  /** Normalized form of [thenIn]. */
  val then: Instruction? by lazy { // should it be InstructionGroup?
    if (thenIn != null) {
      val normed = normalizeForTask(thenIn)
      if (normed != NoOp) return@lazy normed
    }
    null
  }

  init {
    require(instruction.descendantsOfType<Gain>().none { it.gaining == DIE.expression }) {
      "Die remained after task normalization: $instruction"
    }
    when (instruction) {
      is Transform -> error("can't enqueue: $instruction")
      else -> {}
    }
  }

  operator fun times(factor: Int): Task {
    return copy(instructionIn = instruction * factor, thenIn = then?.times(factor))
  }

  private fun normalizeForTask(instruction: Instruction): Instruction {
    return when (instruction) {
      is Change ->
          if (instruction.gaining != DIE.expression) {
            instruction
          } else {
            throw DeadEndException("a Die instruction was reached")
          }
      is Gated -> instruction.copy(inner = normalizeForTask(instruction.inner))
      is Per -> instruction.copy(inner = normalizeForTask(instruction.inner))
      is Or -> {
        val liveOptions =
            instruction.instructions.mapNotNull {
              try {
                normalizeForTask(it)
              } catch (_: DeadEndException) {
                null
              }
            }
        if (liveOptions.isEmpty()) throw DeadEndException("every choice reaches Die")
        Or.create(liveOptions.toSet())
      }
      is Then -> {
        val parts = instruction.instructions
        parts.firstOrNull { (it as? Gain)?.gaining?.className == DIE }
            ?: Then.create(parts.map(::normalizeForTask))
      }
      is NoOp,
      is Multi -> split(instruction).asInstruction()
      is Transform -> error("can't enqueue: $instruction")
    }
  }

  override fun toString() = buildString {
    append(id)
    append(if (next) "* " else "  ")
    appendAssigneeLabel()
    append(instruction)
    then?.let { append(" (THEN $it)") }
    cause?.let { append(" $cause") }
    whyPending?.let { append(" ($it)") }
  }

  fun toStringWithoutCause(queueAssignee: Actor? = null) = buildString {
    append(id)
    append(if (next) "* " else "  ")
    if (queueAssignee == null) {
      appendAssigneeLabel()
    } else {
      append("[queue: $queueAssignee, assignee: $assignee] ")
    }
    append(instruction)
    then?.let { append(" (THEN $it)") }
    whyPending?.let { append(" ($it)") }
  }

  private fun StringBuilder.appendAssigneeLabel() {
    append("[")
    append(assignee)
    append("] ")
  }

  companion object {
    public fun newTasks(
        firstId: TaskId,
        assignee: Actor,
        instruction: InstructionGroup,
        cause: Cause?,
    ): List<Task> {
      val ids = generateSequence(firstId, TaskId::next).iterator()
      return instruction.map { newTask(ids.next(), assignee, it, cause) }
    }

    public fun newTask(
        id: TaskId,
        assignee: Actor,
        instruction: Instruction,
        cause: Cause?,
        automatic: Boolean = false,
    ): Task {
      val task =
          Task(
              id = id,
              assignee = assignee,
              next = automatic,
              instructionIn = instruction,
              cause = cause,
          )
      val normal = task.instruction

      return if (normal is Then && !normal.keepLinked()) {
        task.copy(
            instructionIn = normal.instructions.first(),
            thenIn = Then.create(normal.instructions.drop(1)),
        )
      } else {
        task
      }
    }

    fun noid(
        assignee: Actor,
        automatic: Boolean,
        hit: Instruction,
        cause: Cause,
    ) =
        Task(
            id = TaskId("ZZ"),
            assignee = assignee,
            next = automatic,
            instructionIn = hit,
            cause = cause,
        )
  }

  data class TaskId(val s: String) : Comparable<TaskId> {
    init {
      require(s.length in 1..2) { s }
      require(s.all { it in 'A'..'Z' })
    }

    override fun compareTo(other: TaskId) = s.padStart(2).compareTo(other.s.padStart(2))

    override fun toString() = s

    fun next(): TaskId {
      val news =
          when {
            s == "Z" -> "AA"
            s.length == 1 -> "${s[0] + 1}"
            s[1] == 'Z' -> "${s[0] + 1}A"
            else -> "${s[0]}${s[1] + 1}"
          }
      return TaskId(news)
    }
  }
}
