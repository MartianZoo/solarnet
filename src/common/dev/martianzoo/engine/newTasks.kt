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
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Quantifier.AMAP
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
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
    normalizeTask(
        Task(
            id = TaskId(nextOrdinal++),
            controller = controller,
            actor = actor,
            instruction = it,
            cause = cause,
        ),
        isAbstract,
    )
  }
}

private fun normalizeForTask(tree: InstructionTree): InstructionTree =
    when (tree) {
      is InstructionGroup -> InstructionGroup.of(tree.instructions.map(::normalizeForTask))
      is Change if tree.gaining != DIE.expression -> tree
      is Change ->
          if (tree.quantifier == OPTIONAL || tree.quantifier == AMAP) NoOp
          else throw DeadEndException("a Die instruction was reached")
      is By -> {
        val inner = normalizeForTask(tree.inner)
        when (inner) {
          is NoOp -> NoOp
          is Then ->
              inner.withInstructions(inner.instructions.map { By.createTree(it, tree.actor) })
          else -> By.createTree(inner, tree.actor)
        }
      }
      is Each ->
          when (val body = normalizeForTask(tree.body)) {
            is NoOp -> NoOp
            else -> tree.copy(body = body)
          }
      is Gated -> Gated.createTree(tree.gate, normalizeForTask(tree.inner))
      is Per -> {
        val inner = normalizeForTask(tree.inner)
        if (inner is NoOp) {
          NoOp
        } else {
          tree.copy(
              inner =
                  inner as? Instruction
                      ?: throw TaskException("PER normalized to independent instructions: $inner")
          )
        }
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
        val live = tree.instructions.map(::normalizeForTask).filterNot { it is NoOp }
        when (live.size) {
          0 -> NoOp
          1 -> live.single()
          else -> tree.withInstructions(live)
        }
      }
      is NoOp -> NoOp
      is Transform -> throw ExpressionException("unhandled transform in task: $tree")
    }

/**
 * Applies engine-owned normalization while preserving this task's identity and lifecycle. A
 * separable sequence moves into [Task.then] only when that continuation slot is free; otherwise
 * both sequence boundaries remain intact so their implicit variables stay independent.
 */
internal fun normalizeTask(
    task: Task,
    isAbstract: ((Expression) -> Boolean)? = null,
): Task {
  val instruction =
      normalizeForTask(task.instruction) as? Instruction
          ?: throw TaskException(
              "task input must be split into individual instructions: ${task.instruction}"
          )
  val then = task.then?.let(::normalizeForTask)?.let(InstructionGroup::of)?.takeIf { !it.isEmpty() }
  val normalized = task.copy(instruction = instruction, then = then)
  val sequence = normalized.instruction as? Then ?: return normalized
  if (normalized.then != null || sequence.mustRemainOneTask(isAbstract)) return normalized
  return normalized.copy(
      instruction = sequence.first,
      then = sequence.continuationAfterFirst(),
  )
}
