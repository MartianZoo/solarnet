package dev.martianzoo.agent

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.Transforming.bindXTo
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.pets.types.GroundType
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskResult

/**
 * One caller-held, provisional narrowing of a task. Discarding this object changes no game state.
 */
public class TaskForm
internal constructor(
    public val taskId: TaskId,
    initialInstruction: InstructionTree,
    private val agent: AgentImpl,
) {
  private var currentInstruction: InstructionTree = initialInstruction

  /** One decision in this form's current instruction. A later choice makes it stale. */
  @ConsistentCopyVisibility
  public data class Decision
  internal constructor(
      public val kind: Kind,
      /** The first unresolved type part for [Kind.TARGET], or null for other decisions. */
      public val focus: Expression?,
      internal val instruction: InstructionTree,
  ) {
    public enum class Kind {
      ALTERNATIVE,
      TARGET,
      AMOUNT,
    }
  }

  /** Rechecks this form against the engine's current task and returns its instruction. */
  public val instruction: InstructionTree
    get() = agent.recheckForm(taskId, currentInstruction).also { currentInstruction = it }

  /** Remembers one further narrowing locally; it must narrow the current form. */
  public fun narrow(narrowing: String): InstructionTree {
    val previous = instruction
    val proposed = agent.prepareFormNarrowing(taskId, narrowing)
    return remember(previous, proposed)
  }

  /** Remembers one choice by naming only the part offered for [decision]. */
  public fun choose(decision: Decision, choice: String): InstructionTree {
    val requestedAmount =
        if (decision.kind == Decision.Kind.AMOUNT && choice != "Ok") {
          choice.toIntOrNull()
              ?: throw IllegalArgumentException("`$choice` is not a numeric amount")
        } else null
    val selected =
        optionCandidates(decision, requestedAmount).firstOrNull { it.first == choice }?.second
            ?: throw IllegalArgumentException("`$choice` is not an option for $decision")
    return remember(instruction, agent.recheckForm(taskId, selected))
  }

  private fun remember(previous: InstructionTree, proposed: InstructionTree): InstructionTree {
    proposed.ensureNarrows(previous, agent.reader)
    currentInstruction = proposed
    return proposed
  }

  /** The next voluntary decision. Unsupported abstract shapes fail explicitly. */
  public fun decisions(): List<Decision> {
    val current = instruction
    val head = choiceHead(current)
    if (
        head is Change &&
            head.quantifier == OPTIONAL &&
            current.descendantsOfType<Per>().isNotEmpty()
    ) {
      throw UnsupportedOperationException(
          "optional choices inside `PER` need task resolution first"
      )
    }
    val focus =
        if (head is Change) {
          targetChoice(head, current)?.let { firstTypeReason(agent.reader.resolve(it).groundType) }
        } else null
    if (
        head is Change &&
            focus == null &&
            (head.count is XScalar || head.quantifier == OPTIONAL) &&
            listOfNotNull(head.gaining, head.removing).any { agent.reader.resolve(it).abstract }
    ) {
      throw UnsupportedOperationException("state-dependent target resolution is still pending")
    }
    val kind =
        when (head) {
          is Or -> Decision.Kind.ALTERNATIVE
          is Change ->
              when {
                focus != null -> Decision.Kind.TARGET
                head.count is XScalar || head.quantifier == OPTIONAL -> Decision.Kind.AMOUNT
                else -> null
              }
          else -> null
        }
    if (kind != null) return listOf(Decision(kind, focus, current))
    if (head is Change) return emptyList()
    if (head.isAbstract(agent.reader)) {
      throw UnsupportedOperationException("cannot enumerate a choice in `$current` yet")
    }
    return emptyList()
  }

  /**
   * The next parts the caller can choose for [decision]. Partial candidates are not resolved to
   * filter options: a later choice could make an option workable. Component dependencies of
   * nonoptional changes are restricted to existing components, ignoring refinements for that check.
   * Optional changes retain absent dependencies because a later zero choice can still be legal.
   * Structural impossibility and concrete-target amount bounds also remove options. `X` can have a
   * large domain, so consume the sequence lazily. Re-query after the World changes.
   */
  public fun options(decision: Decision): Sequence<String> =
      optionCandidates(decision).map { it.first }

  private fun optionCandidates(
      decision: Decision,
      requestedAmount: Int? = null,
  ): Sequence<Pair<String, InstructionTree>> {
    require(decision in decisions()) { "this decision is no longer current" }
    val current = instruction
    val head = choiceHead(current)
    fun inForm(selected: InstructionTree): InstructionTree {
      val narrowed =
          if (current is Then) {
            if (selected !is Instruction) {
              throw UnsupportedOperationException("a `THEN` first stage cannot select a group here")
            }
            val first = PetNode.replacer(head, selected).transformInstruction(current.first)
            current.withInstructions(listOf(first) + current.instructions.drop(1))
          } else PetNode.replacer(head, selected).transformInstructionTree(current)
      if (head !is Change || selected !is Change || current.typeVariables.isEmpty) return narrowed
      val bindings =
          listOf(head.gaining to selected.gaining, head.removing to selected.removing)
              .filter { (wide, narrow) -> wide != null && narrow != null && wide != narrow }
              .flatMap { (wide, narrow) ->
                current.typeVariables
                    .bindingsFrom(
                        wide!!,
                        agent.reader.resolve(wide).groundType,
                        agent.reader.resolve(narrow!!).groundType,
                        agent.reader.classTable,
                    )
                    .entries
              }
              .associate { it.key to it.value }
      val binding = current.typeVariables.bind(bindings, agent.reader.classTable)
      // Bind the original occurrences before replacing the chosen head, so a partial binding
      // retains its identity in the first stage too.
      val bound = binding.transformInstructionTree(current)
      return if (
          bound != current &&
              (current.typeVariables.variableAt(targetChoice(head, current)!!) != null ||
                  bound.narrows(narrowed, agent.reader))
      )
          bound
      else binding.transformInstructionTree(narrowed)
    }
    val candidates: Sequence<Pair<String, InstructionTree>> =
        when (decision.kind) {
          Decision.Kind.ALTERNATIVE ->
              (head as Or).instructions.asSequence().map { it.toString() to inForm(it) }
          Decision.Kind.TARGET ->
              targetChoices(head as Change, current).map { (part, selected) ->
                part to inForm(selected)
              }
          Decision.Kind.AMOUNT -> amountChoices(head as Change, current, ::inForm, requestedAmount)
        }
    return candidates
  }

  private fun choiceHead(tree: InstructionTree): InstructionTree =
      when (tree) {
        is Then -> choiceHead(tree.first)
        is By -> choiceHead(tree.inner)
        is Gated -> choiceHead(tree.inner)
        is Per -> choiceHead(tree.inner)
        else -> tree
      }

  private fun metricScale(tree: InstructionTree): Long =
      when (tree) {
        is Then -> metricScale(tree.first)
        is By -> metricScale(tree.inner)
        is Gated -> metricScale(tree.inner)
        is Per -> agent.reader.count(tree.metric).toLong() * metricScale(tree.inner)
        else -> 1L
      }

  private fun firstTypeReason(type: GroundType): Expression? {
    if (type.rootClass.className == CLASS) {
      // A class literal chooses a Class, not an instance's inherited dependencies.
      val represented = type.representedClass ?: return null
      return if (represented.abstract) represented.className.expression else null
    }
    if (type.rootClass.abstract) return type.rootClass.className.expression
    return type.expressionFull.arguments.firstNotNullOfOrNull {
      firstTypeReason(agent.reader.resolve(it).groundType)
    }
  }

  private fun typeChoices(
      type: GroundType,
      filterDependencies: Boolean,
      existing: Boolean = false,
  ): Sequence<Pair<Expression, Expression>> {
    val table = agent.reader.classTable
    if (type.rootClass.abstract) {
      val children =
          table.directSubclasses(type.rootClass).mapNotNull { child ->
            val narrowed = table.glb(type, child.baseType) ?: return@mapNotNull null
            if (!table.isInhabited(narrowed)) return@mapNotNull null
            if (existing && !hasExistingComponent(narrowed)) return@mapNotNull null
            child to narrowed
          }
      val only = children.singleOrNull()
      if (only != null && only.first.abstract) {
        return typeChoices(only.second, filterDependencies, existing)
      }
      return children.asSequence().map { (child, narrowed) ->
        val finished =
            if (!narrowed.rootClass.abstract && !narrowed.dependencies.abstract)
                narrowed.expression.copy(refinement = null)
            else narrowed.expression
        child.className.expression to finished
      }
    }
    val full = type.expressionFull
    val index =
        full.arguments.indexOfFirst {
          firstTypeReason(agent.reader.resolve(it).groundType) != null
        }
    if (index < 0) return emptySequence()
    val dependency = agent.reader.resolve(full.arguments[index]).groundType
    return typeChoices(
            dependency,
            filterDependencies,
            existing = filterDependencies && full.className != CLASS,
        )
        .mapNotNull { (part, narrowed) ->
          val arguments =
              full.arguments.toMutableList().also {
                it[index] = if (full.className == CLASS) narrowed.className.expression else narrowed
              }
          val next = agent.reader.resolve(full.copy(arguments = arguments)).groundType
          if (
              !table.isInhabited(next) ||
                  (existing && full.className != CLASS && !hasExistingComponent(next))
          )
              null
          else {
            val finished =
                if (!next.rootClass.abstract && !next.dependencies.abstract)
                    next.expression.copy(refinement = null)
                else next.expression
            part to finished
          }
        }
  }

  private fun hasExistingComponent(type: GroundType): Boolean {
    // Ignore refinements for this existence check: later choices can specialize their meaning.
    val structural =
        object : PetTransformer() {
              override fun transformNode(node: PetNode): PetNode =
                  transformChildren(if (node is Expression) node.copy(refinement = null) else node)
            }
            .transformExpression(type.expression)
    return agent.reader.getComponents(structural).elements.isNotEmpty()
  }

  private fun targetChoice(change: Change, whole: InstructionTree): Expression? {
    // A supplying occurrence must be chosen before interpreting constraints that observe it,
    // such as a destination that excludes the source of a transmutation.
    fun supplies(target: Expression): Boolean =
        whole.typeVariables.variableDeclaredAt(target) != null || target.arguments.any(::supplies)
    return listOfNotNull(change.gaining, change.removing)
        .sortedByDescending(::supplies)
        .firstOrNull { firstTypeReason(agent.reader.resolve(it).groundType) != null }
  }

  private fun targetChoices(
      change: Change,
      whole: InstructionTree,
  ): Sequence<Pair<String, Instruction>> {
    val target = requireNotNull(targetChoice(change, whole))
    val choosingGain = target === change.gaining
    val type = agent.reader.resolve(target).groundType
    val choices =
        typeChoices(type, filterDependencies = change.quantifier != OPTIONAL).map { (part, choice)
          ->
          val narrowed =
              PetElaborator(agent.reader.classTable).retainTypeVariableNames(choice, target)
          val selected =
              if (change is Transmute) {
                val destination = if (choosingGain) narrowed else change.gaining!!
                val source = if (!choosingGain) narrowed else change.removing!!
                PetNode.replacer(change.fromEx, Full(destination, source))
                    .transformInstruction(change)
              } else PetNode.replacer(target, narrowed).transformInstruction(change)
          part.toString() to selected
        }
    return if (change.quantifier == OPTIONAL) sequenceOf("Ok" to NoOp) + choices else choices
  }

  private fun amountChoices(
      change: Change,
      whole: InstructionTree,
      inForm: (InstructionTree) -> InstructionTree,
      requestedAmount: Int?,
  ): Sequence<Pair<String, InstructionTree>> {
    val gain = change.gaining?.let(agent.reader::resolve)
    val removal = change.removing?.let(agent.reader::resolve)
    require(listOfNotNull(gain, removal).none { it.abstract })
    val limit = agent.changeLimit(change)
    val count = change.count
    val scale = metricScale(whole)
    if (scale == 0L) return emptySequence()
    if (count is ActualScalar) {
      require(change.quantifier == OPTIONAL)
      val maximum =
          if (limit == null) 0
          else minOf(count.value.toLong(), (limit.toLong() + scale - 1) / scale).toInt()
      val positive =
          amounts(maximum, requestedAmount).map { amount ->
            val selected =
                if (change is Transmute)
                    change.copy(scalar = ActualScalar(amount), quantifier = MANDATORY)
                else Change.change(change.gaining, change.removing, amount, MANDATORY)
            amount.toString() to selected
          }
      return (sequenceOf("Ok" to NoOp) + positive).map { (part, selected) ->
        part to inForm(selected)
      }
    }
    val coefficient = (count as XScalar).multiple
    val largestCoefficient =
        whole.descendantsOfType<XScalar>().maxOfOrNull { it.multiple } ?: coefficient
    val sharedAcrossThen =
        whole.descendantsOfType<Then>().any { it.first.descendantsOfType<XScalar>().isNotEmpty() }
    val effectiveCoefficient = coefficient.toLong() * scale
    val largestRepresentable =
        minOf(
            Int.MAX_VALUE / largestCoefficient,
            (Int.MAX_VALUE.toLong() / effectiveCoefficient).toInt(),
        )
    val maximum =
        when {
          limit == null -> 0
          change.quantifier == MANDATORY ->
              minOf(largestRepresentable, (limit.toLong() / effectiveCoefficient).toInt())
          sharedAcrossThen -> largestRepresentable
          limit == Int.MAX_VALUE -> largestRepresentable
          limit == 0 -> minOf(largestRepresentable, 1)
          else ->
              minOf(largestRepresentable, (1 + (limit.toLong() - 1) / effectiveCoefficient).toInt())
        }
    val positive =
        amounts(maximum, requestedAmount).map { amount ->
          amount.toString() to bindXTo(amount).transformInstructionTree(whole)
        }
    return if (change.quantifier == OPTIONAL) sequenceOf("Ok" to inForm(NoOp)) + positive
    else positive
  }

  private fun amounts(maximum: Int, requested: Int?): Sequence<Int> =
      if (requested == null) (1..maximum).asSequence()
      else if (requested in 1..maximum) sequenceOf(requested) else emptySequence()

  /** Submits the accumulated narrowing to the engine, selecting this task if necessary. */
  public fun commit(): TaskResult = agent.commitForm(taskId, instruction)
}
