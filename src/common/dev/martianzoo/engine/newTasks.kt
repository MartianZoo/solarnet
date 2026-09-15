package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.SystemClasses.DIE
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId

internal fun newTasks(
    firstId: TaskId,
    controller: Actor,
    instruction: InstructionGroup,
    cause: Cause?,
    actor: Actor = controller,
    isAbstract: ((Expression) -> Boolean)? = null,
): List<Task> {
  var nextOrdinal = firstId.ordinal
  val normalized =
      InstructionGroup.of(instruction.instructions.map(::normalizeForTask)).instructions
  return normalized.map {
    newTask(
        TaskId(nextOrdinal++),
        controller,
        actor,
        it,
        cause,
        isAbstract = isAbstract,
    )
  }
}

private fun normalizeForTask(tree: InstructionTree): InstructionTree =
    when (tree) {
      is InstructionGroup -> InstructionGroup.of(tree.instructions.map(::normalizeForTask))
      is Change if tree.gaining != DIE.expression -> tree
      is Change -> throw DeadEndException("a Die instruction was reached")
      is By -> {
        val inner = normalizeForTask(tree.inner)
        if (inner is Then) {
          inner.withInstructions(inner.instructions.map { By.createTree(it, tree.actor) })
        } else {
          By.createTree(inner, tree.actor)
        }
      }
      is Each -> tree.copy(body = normalizeForTask(tree.body))
      is Gated -> Gated.createTree(tree.gate, normalizeForTask(tree.inner))
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

/** Applies engine-owned task normalization without changing the task's identity or lifecycle. */
internal fun normalizeTask(task: Task): Task {
  val instruction =
      normalizeForTask(task.instruction) as? Instruction
          ?: throw TaskException(
              "task input must be split into individual instructions: ${task.instruction}"
          )
  val then = task.then?.let(::normalizeForTask)?.let(InstructionGroup::of)?.takeIf { !it.isEmpty() }
  return task.copy(instruction = instruction, then = then)
}

private fun newTask(
    id: TaskId,
    controller: Actor,
    actor: Actor,
    instruction: Instruction,
    cause: Cause?,
    isAbstract: ((Expression) -> Boolean)? = null,
): Task {
  val normalized =
      normalizeTask(
          Task(
              id = id,
              controller = controller,
              actor = actor,
              instruction = instruction,
              cause = cause,
          )
      )
  val normalizedThen = normalized.instruction as? Then
  return if (normalizedThen != null && !normalizedThen.mustRemainOneTask(isAbstract)) {
    normalized.copy(
        instruction = normalizedThen.first,
        then = normalizedThen.continuationAfterFirst(),
    )
  } else {
    normalized
  }
}
