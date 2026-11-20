package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
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
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.state.Actor
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId

/**
 * Identifies an Actor's requested work without selecting, executing, or changing the World. Uses
 * the same instruction interpretation as execution. Results describe current permissions, not proof
 * that their effects can execute successfully, and must be recomputed after changes. Distinct
 * compatible alternatives, including paths through containers, are errors.
 */
public class TaskMatcher(private val world: GameWorld) {
  private val reader = world.reader
  private val allTasks = world.tasks
  private val elaborator = PetElaborator(reader.classTable)
  private val evaluator =
      InstructionEvaluator(reader, Limiter(reader.classTable, world), elaborator)

  /** A unique root permission, required container openings, and its narrowed requested work. */
  @ConsistentCopyVisibility
  public data class Match
  internal constructor(
      public val task: Task,
      public val preparations: List<InstructionTree>,
      public val instruction: InstructionTree,
  )

  private data class Path(val preparations: List<InstructionTree>, val instruction: InstructionTree)

  internal val possibleWorldFacts =
      object : TypeInfo by reader {
        override fun isAbstract(e: Expression): Boolean = reader.resolve(e).isAbstract(this)

        override fun ensureNarrows(wide: Expression, narrow: Expression) {
          reader.resolve(narrow).ensureNarrows(reader.resolve(wide), this)
        }

        override fun ensureSelectionNarrows(wide: Expression, narrow: Expression) {
          reader.resolve(narrow).groundType.ensureSelectionNarrows(reader.resolve(wide), this)
        }

        override fun has(requirement: Requirement): Boolean = true
      }

  /** Interprets text with the same implicit owner and omitted quantifier as Agent input. */
  public fun match(actor: Actor, text: String, contextClass: ClassName? = null): Match? {
    val parsed = parse<InstructionTree>(text)
    return match(
        actor,
        elaborator.elaborateInput(parsed, actor as? Player),
        quantifierOmitted = parsed is Change && parsed.quantifier == null,
        contextClass = contextClass,
    )
  }

  /**
   * Returns the unique eligible permission for [narrowing], or null when none is identified. A
   * selected task excludes other work; an explicit ID or source restricts matching. A required EACH
   * or group opening is reported without performing it. No ordinary effect or THEN tail is
   * traversed to find work that does not yet exist. [expandContainers] is false for selection-only
   * callers, because selection must not execute a parent or admit its children. [releases] supplies
   * eligible NOW bodies that another Actor can release to this Actor. Matching keeps the original
   * task ID and assignee so the caller can release the matching task before executing its body.
   */
  public fun match(
      actor: Actor,
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
      taskId: TaskId? = null,
      contextClass: ClassName? = null,
      expandContainers: Boolean = true,
      releases: List<Pair<Task, InstructionTree>> = emptyList(),
  ): Match? {
    val selected = allTasks.selectedTask()
    if (
        selected != null &&
            (allTasks.getTaskData(selected).assignee != actor ||
                (taskId != null && taskId != selected))
    )
        throw TaskException("task $selected is already selected")
    val assigned =
        (allTasks.extract { it }.filter { it.assignee == actor } +
                releases.map { (task, body) -> task.copy(instruction = body) })
            .filter {
              (taskId == null || it.id == taskId) &&
                  (selected == null || it.id == selected) &&
                  (contextClass == null || it.cause?.context?.className == contextClass) &&
                  it.deferral == allTasks.earliestDeferral
            }
    val matches = assigned.flatMap { task ->
      val offered =
          (task.instruction as? By)
              ?.takeIf {
                task.selected && it.actor == actor.expression
              }
              ?.inner ?: task.instruction
      val paths =
          try {
            paths(narrowing, offered, quantifierOmitted, expandContainers)
          } catch (e: NarrowingException) {
            if (selected != null || taskId != null) throw e
            throw TaskException("cannot identify a task for `$narrowing`: ${e.message}", e)
          }
      paths
          .distinctBy { it.copy(instruction = withoutSatisfiedGate(it.instruction)) }
          .map {
            if (it.preparations.isNotEmpty() && task.then != null)
                throw TaskException(
                    "Explicitly open task ${task.id}; its container has a continuation"
                )
            Match(task, it.preparations, it.instruction)
          }
    }
    if (matches.isNotEmpty()) {
      val first = matches.first()
      if (
          matches
              .map { it.copy(task = it.task.copy(id = first.task.id, cause = first.task.cause)) }
              .distinct()
              .size != 1
      )
          throw TaskException(
              "ambiguous request `$narrowing`: ${matches.size} matching task paths\n" +
                  matches.joinToString("\n") {
                    "${it.task.id}: ${it.preparations + it.instruction}"
                  }
          )
      return first
    }
    if ((selected != null || taskId != null) && assigned.size == 1) {
      val task = assigned.single()
      val effective = effectiveNarrowing(narrowing, task.instruction, quantifierOmitted)
      effective.ensureNarrows(task.instruction, reader)
      return Match(task, emptyList(), effective)
    }
    // Preserve the ordinary unmet-requirement diagnostic without opening a blocked container.
    val possible = assigned.filter {
      intersectTask(narrowing, it.instruction, quantifierOmitted, possibleWorldFacts) != null
    }
    if (possible.isEmpty()) return null
    val id = uniqueMatchingTask(possible)
    val task = possible.first { it.id == id }
    return Match(task, emptyList(), narrowing)
  }

  /**
   * Identifies a whole signed total from [candidates], which the caller supplies from its eligible
   * tasks or pending releases. No tasks execute here; newly caused rewards cannot join this
   * snapshot. Returns null when these are not compatible concrete changes of the same target and
   * quantifier, fewer than two match, or the total differs. [requireWholeTotal] makes a different
   * total an error instead. A distinct matching OR/container path always throws [TaskException].
   * [contextClass] restricts source attribution and [quantifierOmitted] permits the shared pending
   * quantifier. Results put gains before removals; the caller executes and restores the whole call.
   */
  public fun matchingAmounts(
      instruction: InstructionTree,
      quantifierOmitted: Boolean,
      contextClass: ClassName?,
      candidates: List<Pair<Task, InstructionTree>>,
      requireWholeTotal: Boolean = false,
  ): List<Pair<Task, Change>>? {
    val requested =
        InstructionGroup.of(instruction).instructions.map { it as? Change ?: return null }
    val first = requested.firstOrNull() ?: return null
    if (first.gaining != null && first.removing != null) return null
    if (requested.any { it.needsNarrowing(reader) || it.count !is ActualScalar }) return null
    if (
        requested.any {
          it.gaining != first.gaining ||
              it.removing != first.removing ||
              (!quantifierOmitted && it.quantifier != first.quantifier)
        }
    )
        return null
    val target = first.gaining ?: first.removing
    fun sameTarget(change: Change): Boolean =
        (change.gaining == target && change.removing == null) ||
            (change.removing == target && change.gaining == null)
    fun signedCount(change: Change): Long =
        (change.count as ActualScalar).value.toLong() * if (change.gaining == null) -1 else 1
    val matches = candidates.mapNotNull { (task, instruction) ->
      if (
          task.deferral > allTasks.earliestDeferral ||
              (contextClass != null && task.cause?.context?.className != contextClass)
      ) {
        return@mapNotNull null
      }
      val change =
          when (instruction) {
            is Change -> instruction
            is Per -> {
              val inner = instruction.inner as? Change ?: return@mapNotNull null
              if (!sameTarget(inner)) return@mapNotNull null
              (inner * reader.count(instruction.metric)) as? Change ?: return@mapNotNull null
            }
            else -> return@mapNotNull null
          }
      if (!sameTarget(change)) return@mapNotNull null
      task to change
    }
    if (matches.size < 2) return null
    val amounts = matches.map { it.second }
    if (amounts.any { it.count !is ActualScalar }) return null
    if (amounts.map { it.quantifier }.distinct().size != 1) return null
    if (!quantifierOmitted && amounts.any { it.quantifier != first.quantifier }) return null
    if (amounts.sumOf(::signedCount) != requested.sumOf(::signedCount)) {
      if (requireWholeTotal)
          throw TaskException("Requested amount does not equal all matching delegated amounts")
      return null
    }
    if (
        candidates.any { (task, offered) ->
          matches.none { it.first.id == task.id } &&
              task.deferral == allTasks.earliestDeferral &&
              (contextClass == null || task.cause?.context?.className == contextClass) &&
              paths(instruction, offered, quantifierOmitted, true).isNotEmpty()
        }
    )
        throw TaskException("ambiguous request: the total also identifies another task path")
    return matches.sortedBy { it.second.gaining == null }
  }

  // Two arms of the same OR can permit exactly the same operation, with an already satisfied
  // gate on only one arm (e.g. adjacent greenery and its placement fallback). The gate adds no
  // effects or remaining work. Keep containers and THEN continuations intact in this comparison.
  private fun withoutSatisfiedGate(instruction: InstructionTree): InstructionTree =
      if (instruction is Gated && reader.has(instruction.gate))
          withoutSatisfiedGate(instruction.inner)
      else instruction

  private fun paths(
      requested: InstructionTree,
      offered: InstructionTree,
      omitted: Boolean,
      expand: Boolean,
  ): List<Path> {
    if (offered is Gated && !reader.has(offered.gate)) return emptyList()
    val direct = intersectTask(requested, offered, omitted)
    if (expand && offered is Or && requested !is Or && direct !is Or) {
      return offered.instructions.flatMap { paths(requested, it, omitted, expand) }
    }
    if (direct != null) return listOf(Path(emptyList(), direct))
    if (offered is Gated && reader.has(offered.gate)) {
      return paths(requested, offered.inner, omitted, expand).map {
        if (it.preparations.isEmpty())
            it.copy(instruction = Gated.createTree(offered.gate, it.instruction))
        else
            it.copy(
                preparations =
                    listOf(Gated.createTree(offered.gate, it.preparations.first())) +
                        it.preparations.drop(1)
            )
      }
    }
    val evaluated =
        try {
          evaluator.evaluate(offered, expandEach = expand)
        } catch (_: NotNowException) {
          return emptyList()
        }
    if (expand && (offered is Each || offered is InstructionGroup)) {
      val children = (evaluated as? InstructionGroup)?.instructions ?: return emptyList()
      return children.flatMap { child ->
        paths(requested, child, omitted, expand).map {
          it.copy(preparations = listOf(offered) + it.preparations)
        }
      }
    }
    if (evaluated == offered) return emptyList()
    return intersectTask(requested, evaluated, omitted)?.let { listOf(Path(emptyList(), it)) }
        ?: emptyList()
  }

  internal fun intersectTask(
      narrowing: InstructionTree,
      existing: InstructionTree,
      quantifierOmitted: Boolean,
      info: TypeInfo = reader,
  ): InstructionTree? {
    if (narrowing is Instruction.Dispatch && existing is Instruction.Dispatch) {
      if (narrowing.actor != existing.actor || narrowing.deferral != existing.deferral) return null
      if (narrowing.body == existing.body) return existing
      val first = (existing.body as? Then)?.first ?: existing.body
      // A dispatch may identify the current step, including its calculated amount. It may not
      // remove any choice from the recipient: the whole pending step must fit the request.
      if (first.narrows(narrowing.body, reader)) return existing
      fun calculatedHead(tree: InstructionTree): InstructionTree =
          when (tree) {
            is Gated -> calculatedHead(tree.inner)
            is Per -> calculatedHead(tree.inner * reader.count(tree.metric))
            else -> tree
          }
      val calculated = calculatedHead(first)
      return existing.takeIf { calculated != NoOp && calculated.narrows(narrowing.body, reader) }
    }
    val typedZero = narrowing is Change && narrowing.count == ActualScalar(0)
    val effective =
        try {
          effectiveNarrowing(narrowing, existing, quantifierOmitted, info, intersect = true)
        } catch (_: NarrowingException) {
          return null
        }
    effective.intersect(existing, reader.classTable, info)?.let {
      // A shared no-op does not establish that the submitted Type identifies this task.
      return it.takeUnless { typedZero && it == NoOp }
    }
    if (existing is Gated && info.has(existing.gate)) {
      effective.intersect(existing.inner, reader.classTable, info)?.let {
        return Gated.createTree(existing.gate, it)
      }
    }
    return null
  }

  internal fun effectiveNarrowing(
      narrowing: InstructionTree,
      existing: InstructionTree,
      quantifierOmitted: Boolean,
      info: TypeInfo = reader,
      intersect: Boolean = false,
  ): InstructionTree {
    // A first-stage request can identify a linked choice through OR and a satisfied gate.
    if (existing is Or && narrowing !is Or && existing.descendantsOfType<Then>().isNotEmpty()) {
      val matches =
          existing.instructions.mapNotNull { choice ->
            val candidate =
                try {
                  effectiveNarrowing(narrowing, choice, quantifierOmitted, info, intersect)
                } catch (_: NarrowingException) {
                  null
                }
            candidate?.takeIf { it.intersect(choice, reader.classTable, info) != null }
          }
      if (matches.size == 1) return matches.single()
    }
    if (
        existing is Gated &&
            narrowing !is Gated &&
            existing.inner.descendantsOfType<Then>().isNotEmpty() &&
            info.has(existing.gate)
    ) {
      val inner = effectiveNarrowing(narrowing, existing.inner, quantifierOmitted, info, intersect)
      return Gated.createTree(existing.gate, inner)
    }
    if (existing is Then && narrowing !is Then) {
      if (narrowing is Each || narrowing is InstructionGroup) return narrowing
      val head =
          if (existing.first is Gated && narrowing !is Gated) {
            val gate = existing.first as Gated
            Gated.createTree(
                gate.gate,
                effectiveNarrowing(narrowing, gate.inner, quantifierOmitted, info, intersect),
            )
          } else {
            effectiveNarrowing(narrowing, existing.first, quantifierOmitted, info, intersect)
          }
      if (head is Instruction) {
        return existing.narrowFirst(head, info)
      }
    }
    // Selection may still narrow a full linked sequence. Restore an omitted first-stage gate
    // before intersecting shared choices; evaluation checks that gate before execution.
    if (
        narrowing is Then &&
            existing is Then &&
            existing.first is Gated &&
            narrowing.first !is Gated
    ) {
      val gated = existing.first as Gated
      return existing.withInstructions(
          listOf(Gated.createTree(gated.gate, narrowing.first)) + narrowing.instructions.drop(1)
      )
    }
    if (!quantifierOmitted || narrowing !is Change) return narrowing
    fun matches(proposed: InstructionTree, instruction: InstructionTree): Boolean =
        if (intersect) proposed.intersect(instruction, reader.classTable, info) != null
        else proposed.narrows(instruction, info)
    if (matches(narrowing, existing)) return narrowing

    fun inheritQuantifier(change: Change): InstructionTree =
        when (narrowing) {
          is Gain -> Gain.gain(narrowing.scaledEx, change.quantifier)
          is Remove -> Remove.remove(narrowing.scaledEx, change.quantifier)
          is Transmute -> narrowing.copy(quantifier = change.quantifier)
        }

    val choices =
        when (existing) {
          is Change -> listOf(existing)
          is Or -> existing.instructions.filterIsInstance<Change>()
          else -> emptyList()
        }
    return choices
        .mapNotNull { choice ->
          // Inheritance repairs a quantifier mismatch, not incompatible Types or counts. In
          // particular, do not turn an unrelated mandatory call into an optional Ok match.
          if (intersect && narrowing.quantifier!!.narrows(choice.quantifier!!, info)) {
            return@mapNotNull null
          }
          inheritQuantifier(choice).takeIf { inherited -> matches(inherited, choice) }
        }
        .distinct()
        .singleOrNull() ?: narrowing
  }

  internal fun uniqueMatchingTask(matches: List<Task>): TaskId {
    val first =
        matches.firstOrNull() ?: throw TaskException("no matching task; available tasks: $allTasks")
    // Origin metadata does not distinguish choices that otherwise present and behave identically.
    if (matches.map { it.copy(id = first.id, cause = first.cause) }.distinct().size == 1) {
      return first.id
    }
    throw TaskException(
        "${matches.size} matching tasks are ambiguous; select one by its task ID:\n${matches.joinToString("\n")}"
    )
  }
}
