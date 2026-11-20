package dev.martianzoo.engine

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.ResourceLimitException
import dev.martianzoo.pets.api.SystemClasses.SYSTEM
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.types.ClassLimitTable.Limit
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Component.Companion.toComponent
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.toComponent

/**
 * Evaluates instructions against the current World and executes their closed changes.
 *
 * A gained component's exact concrete required parts are constructed in dependency order before
 * reactions to any of their gains. Parts' automatic reactions run before their owners'. Each
 * recorded change executes matching double-colon (`::`) effects recursively before evaluating and
 * admitting its queued effects. A `::` instruction can produce queued work, but that immediate
 * instruction itself never enters the task queue. [Effector.fire] chooses each effect batch; this
 * class constructs required parts and runs immediate effects before admitting queued work.
 */
internal class Instructor
internal constructor(
    private val reader: GameReader,
    private val limiter: Limiter,
    private val changer: Changer,
    private val effector: Effector,
    private val classTable: ClassTable,
    private val elaborator: PetElaborator,
    private val timeline: TimelineImpl,
) {
  private var automaticEffectStack: List<PendingTask> = emptyList()
  private val systemClass = classTable.getClass(SYSTEM)
  private var constructionEvents: MutableList<ConstructionEvent>? = null

  private data class ConstructionEvent(
      val event: ChangeEvent,
      val ancestors: List<PendingTask>,
  )

  /**
   * Applies a correction with required parts and :: effects, suppressing ordinary : effects.
   * Returns work explicitly dispatched by those immediate effects. Unlike ordinary execution, this
   * checks all invariants after the complete group of correction changes.
   */
  internal fun exMachina(changes: InstructionGroup, actor: Actor): List<PendingTask> = buildList {
    changes.instructions.forEach {
      if (it.needsNarrowing(reader)) {
        throw NotFullySpecifiedException("instruction needs narrowing: `$it`", it.sourceLocation)
      }
      val change =
          it as? Change
              ?: throw ExpressionException(
                  "exMachina accepts only elementary changes; found `$it`",
                  sourceLocation = it.sourceLocation,
              )
      executeChange(change, null, this, actor, queuedEffects = false)
    }
    limiter.checkAllInvariants()
  }

  internal fun execute(
      instruction: Instruction,
      cause: Cause?,
      actor: Actor,
  ): List<PendingTask> = buildList {
    doExecute(instruction, cause, this, actor)
  }

  /**
   * Executes an evaluated first stage; later shared stages evaluate against the state they inherit.
   */
  internal fun executeEvaluated(
      instruction: Instruction,
      cause: Cause?,
      actor: Actor,
  ): List<PendingTask> = buildList {
    doExecuteEvaluated(instruction, cause, this, actor)
  }

  private fun doExecute(
      instruction: Instruction,
      cause: Cause?,
      spawned: MutableList<PendingTask>,
      actor: Actor,
      queuedEffects: Boolean = true,
  ) {
    when (val evaluated = evaluate(instruction)) {
      is Instruction ->
          doExecuteEvaluated(
              evaluated,
              cause,
              spawned,
              actor,
              queuedEffects,
          )
      // Independent siblings, such as the branches of a fanout, execute in place here; only an
      // enqueued task turns them into separately selectable work.
      is InstructionGroup ->
          evaluated.instructions.forEach {
            doExecute(it, cause, spawned, actor, queuedEffects)
          }
    }
  }

  private fun doExecuteEvaluated(
      evaluated: Instruction,
      cause: Cause?,
      spawned: MutableList<PendingTask>,
      actor: Actor,
      queuedEffects: Boolean = true,
  ) {
    if (evaluated !is Then && evaluated.needsNarrowing(reader)) {
      throw NotFullySpecifiedException(
          "instruction needs narrowing: `$evaluated`",
          evaluated.sourceLocation,
      )
    }
    when (evaluated) {
      is Change ->
          executeChange(
              evaluated,
              cause,
              spawned,
              if (
                  automaticEffectStack.isNotEmpty() &&
                      (evaluated.gaining ?: evaluated.removing)?.let {
                        reader.resolve(it).rootClass.isSubtypeOf(systemClass)
                      } == true
              )
                  ADMIN
              else actor,
              queuedEffects,
          )
      is Instruction.Dispatch -> spawned += admitted(evaluated, cause)
      is By ->
          throw ExpressionException(
              "instruction-side `BY` reached execution without an assignee transfer: " + evaluated,
              sourceLocation = evaluated.sourceLocation,
          )
      is Then -> {
        doExecuteEvaluated(
            evaluated.first,
            cause,
            spawned,
            actor,
            queuedEffects,
        )
        evaluated.instructions.drop(1).forEach { tree ->
          InstructionGroup.of(tree).instructions.forEach {
            doExecute(it, cause, spawned, actor, queuedEffects)
          }
        }
      }
      is Or -> {} // Every surviving arm is a zero change; narrowing was checked above.
      is NoOp -> {}
      else -> error("somehow a ${evaluated::class.simpleName} was enqueued: $evaluated")
    }
  }

  private fun executeChange(
      instruction: Change,
      cause: Cause?,
      spawned: MutableList<PendingTask>,
      actor: Actor,
      queuedEffects: Boolean,
  ) {
    val ct = instruction.count as ActualScalar
    if (ct.value == 0) return
    check(!queuedEffects || instruction.quantifier == MANDATORY)

    val gaining = instruction.gaining?.toComponent(reader)
    val removing = instruction.removing?.toComponent(reader)

    val eventCursor = timeline.nextEventOrdinal
    while (true) {
      val (result, done) =
          changer.change(
              count = ct.value,
              gaining = gaining,
              removing = removing,
              cause = cause,
              actor = actor,
          )

      constructPartsAndFire(result, spawned, queuedEffects)
      if (done) break
    }
    // TODO(ENGINE_CONTRACT): review invariant timing for client calls containing several changes.
    // Ordinary execution checks each evaluated elementary change and its complete :: chain. A
    // later change in the same client call cannot repair a violation found here. Land Claim's
    // immediate cube removal is included. Corrections skip this check and validate their whole
    // group in exMachina; ordinary transaction-wide suspension remains a proposal to evaluate.
    if (queuedEffects && automaticEffectStack.isEmpty()) {
      try {
        limiter.checkInvariantsSince(eventCursor)
      } catch (e: LimitsException) {
        e.sourceLocation =
            instruction.sourceLocation
                ?: instruction.gaining?.sourceLocation
                ?: instruction.removing?.sourceLocation
        throw e
      }
    }
  }

  /** Required parts all exist before any event in their construction dispatches effects. */
  private fun constructPartsAndFire(
      event: ChangeEvent,
      spawned: MutableList<PendingTask>,
      queuedEffects: Boolean,
  ) {
    val recorded = ConstructionEvent(event, automaticEffectStack)
    constructionEvents?.let {
      it += recorded
      return
    }
    val events = mutableListOf(recorded)
    val pending = mutableListOf<Pair<Limit, ConstructionEvent>>()
    constructionEvents = events
    try {
      var discovered = 0
      while (true) {
        while (discovered < events.size) {
          val origin = events[discovered++]
          origin.event.change.gaining
              ?.takeIf {
                it != origin.event.change.removing && reader.countComponent(it.type) > 0
              }
              ?.let { owner ->
                classTable.componentLimits.requiredParts(owner.type).forEach {
                  pending += it to origin
                }
              }
        }
        pending.removeAll { (part, _) -> reader.countComponent(part.type) >= part.range.first }
        if (pending.isEmpty()) break
        val ready = pending.indexOfFirst { (part, _) ->
          part.type.typeDependencies.all { reader.countComponent(it.boundType) > 0 }
        }
        // If none can proceed, ordinary gain evaluation reports the missing dependencies.
        val (part, origin) = pending.removeAt(ready.takeIf { it >= 0 } ?: 0)
        val owner = checkNotNull(origin.event.change.gaining)
        val missing = part.range.first - reader.countComponent(part.type)
        val instruction = elaborator.atomizeGains(gain(part.type.expression, missing))
        executeAutomaticEffect(
            PendingTask.fromEffect(
                context = owner,
                triggerEvent = origin.event,
                changedComponentPlayer = owner.owningPlayer,
                automatic = true,
                instruction = InstructionGroup.of(instruction),
            ),
            spawned,
            queuedEffects,
            origin.ancestors,
        )
      }
    } finally {
      constructionEvents = null
    }
    for ((change) in events.asReversed()) {
      for (task in effector.fire(change, automatic = true)) {
        executeAutomaticEffect(task, spawned, queuedEffects)
      }
    }
    if (queuedEffects) {
      for ((change) in events) {
        spawned += effector.fire(change, automatic = false)
      }
    }
  }

  private fun executeAutomaticEffect(
      task: PendingTask,
      spawned: MutableList<PendingTask>,
      queuedEffects: Boolean,
      ancestors: List<PendingTask> = automaticEffectStack,
  ) {
    if (ancestors.size >= MAX_AUTOMATIC_EFFECT_DEPTH) {
      throw ResourceLimitException(
          "automatic effect chain exceeded depth $MAX_AUTOMATIC_EFFECT_DEPTH: " +
              (ancestors + task).joinToString(" -> ") { it.instruction.toString() },
          sourceLocation = task.instruction.sourceLocation,
      )
    }
    val enclosingStack = automaticEffectStack
    automaticEffectStack = ancestors + task
    try {
      task.instruction.instructions.forEach {
        doExecute(
            it,
            task.cause,
            spawned,
            task.assignee,
            queuedEffects,
        )
      }
    } finally {
      automaticEffectStack = enclosingStack
    }
  }

  private val evaluator =
      InstructionEvaluator(reader, limiter, elaborator) { automaticEffectStack.isNotEmpty() }

  internal fun evaluate(
      instruction: InstructionTree,
      worldGainNarrowing: Boolean = false,
      expandEach: Boolean = true,
  ): InstructionTree = evaluator.evaluate(instruction, worldGainNarrowing, expandEach)

  internal fun changeLimit(change: Change): Int? = evaluator.changeLimit(change)

  internal fun actorFor(instruction: By): Actor = evaluator.actorFor(instruction)

  internal fun validateConcreteChange(change: Change) = evaluator.validateConcreteChange(change)

  /**
   * The work [dispatch] admits: its body, for its recipient, with its deferral (L2-17 to L2-19).
   */
  private fun admitted(dispatch: Instruction.Dispatch, cause: Cause?): PendingTask =
      PendingTask(
          assignee = reader.recipientOf(dispatch),
          instruction = InstructionGroup.of(dispatch.body),
          cause = cause,
          deferral = dispatch.deferral,
      )
}

private const val MAX_AUTOMATIC_EFFECT_DEPTH = 8
