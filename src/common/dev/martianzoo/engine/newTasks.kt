package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.SystemClasses.DIE
import dev.martianzoo.pets.ast.Deferral
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
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.state.Actor
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId

/**
 * The independent tasks after normalization. Literal zero changes need no task; zero counts
 * discovered when selecting an existing task remain typed until its submitted choice is checked.
 */
internal fun taskInstructions(instruction: InstructionGroup): List<Instruction> =
    InstructionGroup.of(instruction.instructions.map { normalizeForTask(it, discardZero = true) })
        .instructions

/** One normalized task for a single [instruction] from [taskInstructions]. */
internal fun newTask(
    id: TaskId,
    assignee: Actor,
    instruction: Instruction,
    cause: Cause?,
    isAbstract: ((Expression) -> Boolean)? = null,
    deferral: Deferral = Deferral.NONE,
): Task =
    normalizeTask(
        Task(
            id = id,
            assignee = assignee,
            instruction = instruction,
            cause = cause,
            deferral = deferral,
        ),
        isAbstract,
    )

private fun normalizeForTask(tree: InstructionTree, discardZero: Boolean = false): InstructionTree =
    when (tree) {
      is InstructionGroup ->
          InstructionGroup.of(tree.instructions.map { normalizeForTask(it, discardZero) })
      is Change if discardZero && tree.count == ActualScalar(0) -> NoOp
      is Change if tree.gaining != DIE.expression -> tree
      is Change ->
          if (tree.quantifier == OPTIONAL || tree.quantifier == AMAP) NoOp
          else
              throw DeadEndException(
                  "a `Die` instruction was reached",
                  sourceLocation = tree.sourceLocation,
              )
      is By -> {
        val inner = normalizeForTask(tree.inner, discardZero)
        when (inner) {
          is NoOp -> NoOp
          is Then ->
              inner.withInstructions(inner.instructions.map { By.createTree(it, tree.actor) })
          else -> By.createTree(inner, tree.actor)
        }
      }
      is Each ->
          when (val body = normalizeForTask(tree.body, discardZero)) {
            is NoOp -> NoOp
            else -> tree.copy(body = body)
          }
      is Instruction.Dispatch ->
          when (val body = normalizeForTask(tree.body, discardZero)) {
            is NoOp -> NoOp
            else -> tree.copy(body = body)
          }
      is Gated -> Gated.createTree(tree.gate, normalizeForTask(tree.inner, discardZero))
      is Per -> {
        val inner = normalizeForTask(tree.inner, discardZero)
        if (inner is NoOp) {
          NoOp
        } else {
          tree.copy(
              inner =
                  inner as? Instruction
                      ?: throw TaskException(
                          "`PER` normalized to independent instructions: `$inner`"
                      )
          )
        }
      }
      is Or -> {
        val liveOptions =
            tree.instructions.mapNotNull {
              try {
                normalizeForTask(it, discardZero)
              } catch (_: DeadEndException) {
                null
              }
            }
        if (liveOptions.isEmpty()) throw DeadEndException("every choice reaches `Die`")
        Or.createTree(liveOptions)
      }
      is Then -> {
        val live =
            tree.instructions.map { normalizeForTask(it, discardZero) }.filterNot { it is NoOp }
        when (live.size) {
          0 -> NoOp
          1 -> live.single()
          else -> tree.withInstructions(live)
        }
      }
      is NoOp -> NoOp
      is Transform ->
          throw ExpressionException(
              "unhandled instruction transform: `$tree`",
              sourceLocation = tree.sourceLocation,
          )
    }

/**
 * Applies engine-owned normalization while preserving this task's identity and lifecycle. A
 * separable sequence moves into [Task.then] only when that continuation slot is free; otherwise
 * both sequence boundaries remain intact so their local Type-variable scopes stay independent.
 */
internal fun normalizeTask(
    task: Task,
    isAbstract: ((Expression) -> Boolean)? = null,
): Task {
  val instruction =
      normalizeForTask(task.instruction) as? Instruction
          ?: throw TaskException(
              "task input must be split into individual instructions: `${task.instruction}`"
          )
  val then =
      task.then?.let { normalizeForTask(it) }?.let(InstructionGroup::of)?.takeIf { !it.isEmpty() }
  val normalized = task.copy(instruction = instruction, then = then)
  val sequence = normalized.instruction as? Then ?: return normalized
  val runtimeSequence =
      sequence.typeVariables.expandNames().transformInstruction(sequence) as Instruction.Then
  val runtime = normalized.copy(instruction = runtimeSequence)
  if (runtime.then != null || runtimeSequence.mustRemainOneTask(isAbstract)) {
    if (runtimeSequence.descendantsOfType<By>().isNotEmpty()) {
      throw TaskException(
          "`BY` cannot occur inside a `THEN` that must remain a single task: `$runtimeSequence`"
      )
    }
    return runtime
  }
  return runtime.copy(
      instruction = runtimeSequence.first,
      then = runtimeSequence.continuationAfterFirst(),
  )
}
