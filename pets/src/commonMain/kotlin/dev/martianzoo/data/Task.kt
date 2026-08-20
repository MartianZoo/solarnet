package dev.martianzoo.data

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.api.SystemClasses.DIE
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree

public data class Task(
    /** Identifies this task by the ordinal of its add event. Stable through task edits. */
    val id: TaskId,

    /** Whose pending-work queue contains this task and whose scoped gameplay may revise it. */
    val assignee: Actor,

    /** The Actor recorded for resulting changes unless the instruction contains an explicit BY. */
    val actor: Actor = assignee,

    /** If true, the world may not be modified until this task is completed. */
    val next: Boolean = false,

    /**
     * What to do. Can be abstract. If so, it will have to be revised to something narrower and
     * concrete before it is executed. Normalized to [instruction].
     */
    private val instructionIn: Instruction,

    /** Independent work enqueued when this task is removed. Used for `THEN` instructions. */
    private val thenIn: InstructionGroup? = null,

    /** Why was this task born? */
    val cause: Cause?,

    /** Why is the task still here? */
    val whyPending: String? = null,
) {

  /** Normalized form of [instructionIn]. */
  public val instruction: Instruction =
      normalizeForTask(instructionIn) as? Instruction
          ?: throw TaskException(
              "task input must be split into individual instructions: $instructionIn"
          )

  /** Normalized form of [thenIn]. */
  val then: InstructionGroup? by lazy {
    thenIn?.let(::normalizeForTask)?.let(InstructionGroup::of)?.takeIf { !it.isEmpty() }
  }

  init {
    require(instruction.descendantsOfType<Gain>().none { it.gaining == DIE.expression }) {
      "Die remained after task normalization: $instruction"
    }
    when (instruction) {
      is Transform -> throw ExpressionException("unhandled transform in task: $instruction")
      else -> {}
    }
  }

  public operator fun times(factor: Int): Task {
    return copy(instructionIn = instruction * factor, thenIn = then?.times(factor))
  }

  override fun toString(): String = buildString {
    append(id)
    append(if (next) "* " else "  ")
    appendAssigneeLabel()
    append(instruction)
    then?.let { append(" (THEN $it)") }
    cause?.let { append(" $cause") }
    whyPending?.let { append(" ($it)") }
  }

  public fun toStringWithoutCause(displayId: String = id.toString()): String = buildString {
    append(displayId)
    append(if (next) "* " else "  ")
    appendAssigneeLabel()
    append(instruction)
    then?.let { append(" (THEN $it)") }
    whyPending?.let { append(" ($it)") }
  }

  private fun StringBuilder.appendAssigneeLabel() {
    append("[")
    append(assignee)
    append("] ")
  }

  public companion object {
    public fun newTasks(
        firstId: TaskId,
        assignee: Actor,
        instruction: InstructionGroup,
        cause: Cause?,
        actor: Actor = assignee,
        isAbstract: ((Expression) -> Boolean)? = null,
    ): List<Task> {
      val ids = generateSequence(firstId, TaskId::next).iterator()
      val normalized =
          InstructionGroup.of(instruction.instructions.map(::normalizeForTask)).instructions
      return normalized.map {
        newTask(ids.next(), assignee, actor, it, cause, isAbstract = isAbstract)
      }
    }

    private fun normalizeForTask(tree: InstructionTree): InstructionTree {
      return when (tree) {
        is InstructionGroup -> InstructionGroup.of(tree.instructions.map(::normalizeForTask))
        is Change ->
            if (tree.gaining != DIE.expression) {
              tree
            } else {
              throw DeadEndException("a Die instruction was reached")
            }
        is By -> {
          val inner = normalizeForTask(tree.inner)
          if (inner is Then) {
            inner.withInstructions(inner.instructions.map { By.createTree(it, tree.actor) })
          } else {
            By.createTree(inner, tree.actor)
          }
        }
        is Gated -> tree.copy(inner = normalizeForTask(tree.inner))
        is Per -> {
          val inner = normalizeForTask(tree.inner)
          tree.copy(
              inner =
                  inner as? Instruction
                      ?: throw TaskException("PER normalized to independent instructions: $inner")
          )
        }
        is Or -> {
          val liveOptions =
              tree.instructions.mapNotNull {
                try {
                  normalizeForTask(it)
                } catch (_: DeadEndException) {
                  null
                }
              }
          if (liveOptions.isEmpty()) throw DeadEndException("every choice reaches Die")
          Or.createTree(liveOptions)
        }
        is Then -> {
          if ((tree.first as? Gain)?.gaining?.className == DIE) {
            throw DeadEndException("a Die instruction was reached")
          }
          tree.withInstructions(tree.instructions.map(::normalizeForTask))
        }
        is NoOp -> NoOp
        is Transform -> throw ExpressionException("unhandled transform in task: $tree")
      }
    }

    private fun newTask(
        id: TaskId,
        assignee: Actor,
        actor: Actor,
        instruction: Instruction,
        cause: Cause?,
        automatic: Boolean = false,
        isAbstract: ((Expression) -> Boolean)? = null,
    ): Task {
      val task =
          Task(
              id = id,
              assignee = assignee,
              actor = actor,
              next = automatic,
              instructionIn = instruction,
              cause = cause,
          )
      val normal = task.instruction

      return if (normal is Then && !normal.keepLinked(isAbstract)) {
        task.copy(
            instructionIn = normal.first,
            thenIn = normal.continuationAfterFirst(),
        )
      } else {
        task
      }
    }
  }

  /** A task's stable internal identity, wrapping the ordinal of its add event. */
  public data class TaskId(val ordinal: Int) : Comparable<TaskId> {
    init {
      require(ordinal >= 0)
    }

    public fun next(): TaskId = TaskId(ordinal + 1)

    override fun compareTo(other: TaskId): Int = ordinal.compareTo(other.ordinal)

    override fun toString(): String = ordinal.toString()
  }
}
