package dev.martianzoo.tfm.engine

import dev.martianzoo.engine.World
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.SystemClasses.ATOMIZED
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.SystemClasses.CUSTOM_INSTRUCTION
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Deferral
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Dispatch
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Quantifier.AMAP
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Task

/**
 * Proposes pending tasks whose early claim may preserve completed outcomes. This development-time
 * analysis must be checked against [DecisionSearch] with the NONE policy before a Canon register
 * row is enabled; it is not the runtime SAFE predicate.
 *
 * The task [t] must involve no choice: a concrete instruction, fixed except for amounts the World
 * itself determines. Every completed play must perform [t] before the work under way can end,
 * because ordinary work blocks deferred work and nothing can discard [t]. Deferred work therefore
 * never precedes [t]. Let W be every other execution that can precede [t]: the other ordinary
 * tasks, their continuations, and every reaction they can trigger. If [t] commutes with each member
 * of W, any completed play can be rearranged to perform [t] first, reaching the same state; so
 * performing [t] now loses nothing (the persistent-set argument of partial-order reduction).
 *
 * The proof compares what [t] does and depends on with what W can do and depend on. It
 * over-approximates both from the Pets of the pending work and of every effect that work could
 * trigger or bring into play, so an unrecognized construct makes the task unsafe rather than safe.
 * The comparison is directional, as the goal is: performing [t] early may enlarge another task's
 * choices, but must not shrink them, change an amount the World computes, change which reactions
 * fire, or make other work impossible.
 */
public class SafeTasks
private constructor(
    private val world: World,
    private val catalogCache: CatalogCache,
) {
  /** Class-table facts shared by an Agent and its speculative forks. */
  private class CatalogCache {
    val related = mutableMapOf<Pair<Class, Class>, Boolean>()
    val effects = mutableMapOf<Class, List<Effect>>()
    val concrete = mutableMapOf<Class, List<Class>>()
    val dependents = mutableMapOf<Class, List<Pattern>>()
  }

  private val reader = world.reader
  private val table = world.classTable
  private val elaborator = PetElaborator(table)
  private val component: Class = table.getClass(COMPONENT)
  private val atomized: Class? = table.findClass(ATOMIZED)
  private val customInstruction: Class? = table.findClass(CUSTOM_INSTRUCTION)
  private val cardBack: Class? = table.findClass(cn("CardBack"))
  private val revealedLocations: List<Class> =
      listOf("Hand", "Selecting", "Revealed").mapNotNull { table.findClass(cn(it)) }
  private val filteredReveals: List<Class> = FILTERED_REVEALS.mapNotNull(table::findClass)

  /** Why performing [task] now might lose an outcome, or null if it cannot. */
  private fun objection(task: Task): String? {
    if (task.deferred) return "deferred"
    if (task.selected || world.tasks.selectedTask() != null) return "a task is selected"
    return try {
      if (task.instruction.isAbstract(reader)) return "it needs a choice"
      val claim = footprintOfClaim(task) ?: return "its reactions could not be summarized"
      claim.unknown?.let {
        return it
      }
      val others = world.tasks.extract { it }.filter { it.id != task.id && !it.deferred }
      val before = workBefore(others) ?: return "other work could not be summarized"
      conflict(task, claim, before)
    } catch (failure: Unanalyzable) {
      failure.message
    }
  }

  /** Some work could not be summarized; the task is then treated as unsafe. */
  private class Unanalyzable(message: String) : RuntimeException(message)

  // PATTERNS

  /** The components an access may touch: every component of [klass] narrowing [type], if known. */
  private data class Pattern(val klass: Class, val type: Type?) {
    override fun toString(): String = type?.expression?.toString() ?: "${klass.className}*"
  }

  private val top = Pattern(component, null)

  private fun pattern(expression: Expression): Pattern {
    val type =
        try {
          reader.resolve(expression)
        } catch (_: Exception) {
          null
        }
    if (type != null) return Pattern(type.rootClass, type)
    val klass = table.findClass(expression.className) ?: return top
    return Pattern(klass, null)
  }

  private fun intersects(a: Pattern, b: Pattern): Boolean {
    if (a.klass == component || b.klass == component) return true
    if (!related(a.klass, b.klass)) return false
    val left = a.type ?: return true
    val right = b.type ?: return true
    return try {
      table.glb(left, right) != null
    } catch (_: Exception) {
      true
    }
  }

  private fun related(a: Class, b: Class): Boolean =
      catalogCache.related.getOrPut(a to b) {
        a.isSubtypeOf(b) ||
            b.isSubtypeOf(a) ||
            try {
              table.glb(a, b) != null
            } catch (_: Exception) {
              true
            }
      }

  // ACCESSES

  private enum class Op {
    /** A gain whose amount does not depend on the gained type. */
    GAIN,
    /** A gain clipped to the gained type's remaining capacity. */
    GAIN_AMAP,
    /** A removal that must find its whole amount present. */
    REMOVE,
    /** A removal clipped to the amount present. */
    REMOVE_AMAP,
    /** A removal whose amount or existence a Player chooses, bounded by the amount present. */
    REMOVE_CHOICE,
    /** An amount, a trigger condition, or a branch count that depends on the exact count. */
    READ,
    /** An availability gate that removals can falsify. */
    GATE_MIN,
    /** An availability gate that gains can falsify. */
    GATE_MAX,
    /** Cards arriving from a hidden deck. */
    REVEAL,
  }

  private class Access(
      val op: Op,
      val pattern: Pattern,
      /** For a READ in a trigger condition, the class whose gains fire that trigger. */
      val threshold: Class? = null,
      /** For a REVEAL, whether the arrival was selected by an external filter. */
      val filtered: Boolean = false,
      /** The Actor performing a change written in a task itself; null inside reactions. */
      val actor: Actor? = null,
      /** The marker of a latch reaction this access belongs to; see [latchOf]. */
      val latch: Pattern? = null,
  ) {
    override fun toString(): String = "$op $pattern"
  }

  private class Event(val gain: Boolean, val pattern: Pattern)

  /** What some work does and depends on. */
  private inner class Footprint {
    val accesses = mutableListOf<Access>()
    val events = mutableListOf<Event>()
    var unknown: String? = null

    fun add(
        op: Op,
        pattern: Pattern,
        threshold: Class? = null,
        filtered: Boolean = false,
        actor: Actor? = null,
        latch: Pattern? = null,
    ) {
      if (pattern.klass == component && op != Op.READ && op != Op.GATE_MIN && op != Op.GATE_MAX) {
        unknown = "an unidentified change"
      }
      accesses += Access(op, pattern, threshold, filtered, actor, latch)
    }
  }

  /**
   * Records [tree]'s direct accesses and events; triggered work is handled by the caller. [actor]
   * performs changes written directly in a task, and is null inside reactions.
   */
  private fun walk(
      tree: InstructionTree,
      into: Footprint,
      filtered: Boolean = false,
      actor: Actor? = null,
      latch: Pattern? = null,
  ) {
    when (tree) {
      is InstructionGroup -> tree.instructions.forEach { walk(it, into, filtered, actor) }
      is NoOp -> {}
      is Change -> change(tree, into, filtered, actor, latch)
      is Per -> {
        read(tree.metric, into)
        walk(tree.inner, into, filtered, actor)
      }
      is Gated -> {
        gate(tree.gate, into)
        walk(tree.inner, into, filtered, actor)
      }
      is Or -> tree.instructions.forEach { walk(it, into, filtered, actor) }
      is Then -> tree.instructions.forEach { walk(it, into, filtered, actor) }
      is By -> {
        expressionReads(tree.actor, into)
        walk(tree.inner, into, filtered, null)
      }
      // Deferred work waits for every ordinary task; it cannot run before the task in question.
      is Dispatch -> if (tree.deferral == Deferral.NONE) walk(tree.body, into, filtered, null)
      is Each -> {
        into.add(Op.READ, pattern(tree.selector))
        expressionReads(tree.selector, into)
        walk(tree.body, into, filtered, actor)
      }
      else -> into.unknown = "unsupported instruction `$tree`"
    }
  }

  private fun change(
      change: Change,
      into: Footprint,
      filtered: Boolean,
      actor: Actor?,
      latch: Pattern? = null,
  ) {
    val chosen = change.count is XScalar || change.quantifier == OPTIONAL
    change.removing?.let { removing ->
      val removed = pattern(removing)
      expressionReads(removing, into)
      into.add(
          when {
            chosen -> Op.REMOVE_CHOICE
            change.quantifier == AMAP -> Op.REMOVE_AMAP
            else -> Op.REMOVE
          },
          removed,
          actor = actor,
      )
      into.events += Event(gain = false, removed)
    }
    change.gaining?.let { gaining ->
      val gained = pattern(gaining)
      expressionReads(gaining, into)
      if (customInstruction != null && gained.klass.isSubtypeOf(customInstruction)) {
        into.unknown = "custom instruction `$gaining`"
      }
      into.add(
          if (change.quantifier == AMAP) Op.GAIN_AMAP else Op.GAIN,
          gained,
          actor = actor,
          latch = latch,
      )
      if (change.removing == null && isRevealed(gained)) {
        into.add(Op.REVEAL, gained, filtered = filtered)
      }
      into.events += Event(gain = true, gained)
    }
  }

  /** Whether gaining [pattern] draws a card back from a hidden deck. */
  private fun isRevealed(pattern: Pattern): Boolean {
    val back = cardBack ?: return false
    if (!related(pattern.klass, back)) return false
    val type = pattern.type ?: return true
    return type.typeDependencies.any { dependency ->
      val bound = dependency.boundType.rootClass
      revealedLocations.any { related(it, bound) }
    } || type.typeDependencies.isEmpty()
  }

  /** Refinements decide which components an expression matches, so they read their requirement. */
  private fun expressionReads(expression: Expression, into: Footprint) {
    expression.refinement?.let { refinementReads(it, into) }
    expression.arguments.forEach { expressionReads(it, into) }
  }

  private fun refinementReads(refinement: Refinement, into: Footprint) {
    when (refinement) {
      is Refinement.Has -> requirementReads(refinement.requirement, into, Op.READ)
      is Refinement.Not -> expressionReads(refinement.excluded, into)
      is Refinement.And -> refinement.refinements.forEach { refinementReads(it, into) }
    }
  }

  private fun read(metric: Metric, into: Footprint, threshold: Class? = null) {
    for ((counted, _) in countedPatterns(metric, into)) {
      into.add(Op.READ, counted, threshold)
    }
  }

  /**
   * The patterns a metric counts, each with its polarity: true if the metric can only grow as
   * matching components are gained. Unrecognized metrics read everything.
   */
  private fun countedPatterns(metric: Metric, into: Footprint): List<Pair<Pattern, Boolean>> =
      when (metric) {
        is Metric.Count -> {
          expressionReads(metric.expression, into)
          customReads(metric.expression)?.map { it to true }
              ?: listOf(pattern(metric.expression) to true)
        }
        is Metric.Constant,
        is Property -> emptyList()
        is Metric.Scaled -> countedPatterns(metric.inner, into)
        is Metric.Max -> countedPatterns(metric.inner, into) + countedPatterns(metric.maximum, into)
        is Metric.Subtract ->
            countedPatterns(metric.minuend, into) +
                countedPatterns(metric.subtrahend, into).map { (p, up) -> p to !up }
        is Metric.Or -> metric.metrics.flatMap { countedPatterns(it, into) }
        // A rank compares several counts; any change can move it either way.
        is Metric.Rank -> {
          metric.selector?.let { into.add(Op.READ, pattern(it)) }
          metric.metrics
              .flatMap { countedPatterns(it, into) }
              .flatMap { (p, _) ->
                listOf(p to true, p to false)
              }
        }
        else -> listOf(top to true, top to false)
      }

  /** A custom metric counts what its Kotlin implementation reads, if that is known. */
  private fun customReads(expression: Expression): List<Pattern>? {
    val name = expression.className
    val reads = CUSTOM_METRIC_READS[name] ?: return if (isCustomMetric(name)) listOf(top) else null
    return reads.map { table.findClass(cn(it))?.let { klass -> Pattern(klass, null) } ?: top }
  }

  private fun isCustomMetric(name: ClassName): Boolean =
      table.findClass(name)?.let { klass ->
        klass.allSuperclasses().any { it.className == cn("CustomMetric") }
      } == true

  /** Records an availability gate by the direction of change that could make it false. */
  private fun gate(requirement: Requirement, into: Footprint) =
      requirementReads(requirement, into, null)

  private fun requirementReads(
      requirement: Requirement,
      into: Footprint,
      forced: Op?,
      threshold: Class? = null,
      latch: Pattern? = null,
  ) {
    when (requirement) {
      is Requirement.Min,
      is Requirement.Max,
      is Requirement.Exact -> {
        val counting = requirement as Requirement.Counting
        for ((counted, growing) in countedPatterns(counting.metric, into)) {
          val op =
              forced
                  ?: when (requirement) {
                    // More of a growing count can only keep a minimum met.
                    is Requirement.Min -> if (growing) Op.GATE_MIN else Op.GATE_MAX
                    is Requirement.Max -> if (growing) Op.GATE_MAX else Op.GATE_MIN
                    else -> Op.READ
                  }
          val marker = latch?.takeIf {
            requirement is Requirement.Max && requirement.maximum == 0 && intersects(counted, it)
          }
          into.add(op, counted, threshold, latch = marker)
        }
      }
      is Requirement.And ->
          requirement.requirements.forEach {
            requirementReads(it, into, forced, threshold, latch)
          }
      is Requirement.Or ->
          requirement.requirements.forEach {
            requirementReads(it, into, forced, threshold, latch)
          }
      else -> into.add(Op.READ, top, threshold)
    }
  }

  // LISTENERS

  /** A subscribed effect of one live or possible component. */
  private class Listener(val source: Pattern, val effect: Effect)

  /** Subscribed effects of components in play now. */
  private val liveListeners: List<Listener> by lazy {
    reader.getComponents(component.defaultType).elements.flatMap { type ->
      subscribedEffects(type.rootClass, type.expression).map {
        Listener(Pattern(type.rootClass, type), it)
      }
    }
  }

  /** The effects [klass] carries, specialized to [context] when it is concrete. */
  private fun classEffects(klass: Class, context: Expression?): List<Effect> {
    val general =
        catalogCache.effects.getOrPut(klass) {
          try {
            elaborator.classEffects(klass)
          } catch (_: Exception) {
            throw Unanalyzable("effects of `${klass.className}`")
          }
        }
    if (context == null) return general
    val specific =
        try {
          reader.resolve(context)
        } catch (_: Exception) {
          null
        }
    return general.map { effect ->
      try {
        if (specific != null && !specific.abstract) {
          elaborator.specializeEffect(klass.defaultType, specific, effect, context)
        } else {
          replaceThisExpressionsWith(context).transformEffect(effect)
        }
      } catch (_: Exception) {
        effect
      }
    }
  }

  private fun subscribedEffects(klass: Class, context: Expression?): List<Effect> =
      classEffects(klass, context).filter { !isSelf(it.trigger) }

  private fun isSelf(trigger: Trigger): Boolean =
      when (trigger) {
        is Trigger.SelfTrigger -> true
        is Trigger.SubscribedTrigger -> false
        is Trigger.Or -> trigger.triggers.all(::isSelf)
        is Trigger.WrappingTrigger -> isSelf(trigger.inner)
      }

  /** Whether [trigger] can match [event]. */
  private fun matches(trigger: Trigger, event: Event): Boolean =
      when (trigger) {
        is Trigger.OnGainOf -> event.gain && intersects(event.pattern, pattern(trigger.expression))
        is Trigger.OnRemoveOf ->
            !event.gain && intersects(event.pattern, pattern(trigger.expression))
        is Trigger.SelfTrigger -> false
        is Trigger.Or -> trigger.triggers.any { matches(it, event) }
        // Who performed the change and the state it happened in only narrow the match.
        is Trigger.ByTrigger -> matches(trigger.inner, event)
        is Trigger.IfTrigger -> matches(trigger.inner, event)
        is Trigger.XTrigger -> matches(trigger.inner, event)
        is Trigger.Transform -> true
      }

  /** Records the reads of [effect]'s trigger conditions, which decide whether it fires. */
  private fun triggerReads(effect: Effect, into: Footprint) =
      triggerReads(effect.trigger, into, latchOf(effect))

  private fun triggerReads(trigger: Trigger, into: Footprint, latch: Pattern?) {
    when (trigger) {
      is Trigger.IfTrigger -> {
        requirementReads(trigger.condition, into, Op.READ, watchedGain(trigger.inner), latch)
        triggerReads(trigger.inner, into, latch)
      }
      is Trigger.WrappingTrigger -> triggerReads(trigger.inner, into, latch)
      is Trigger.Or -> trigger.triggers.forEach { triggerReads(it, into, latch) }
      is Trigger.OnGainOf -> expressionReads(trigger.expression, into)
      is Trigger.OnRemoveOf -> expressionReads(trigger.expression, into)
      is Trigger.SelfTrigger -> {}
    }
  }

  /**
   * The marker [effect] latches, if it is a latch: it only gains as many as possible of a marker
   * that can exist at most once, and fires only while that marker is absent. The marker then
   * records whether any qualifying event happened, whatever order the events came in.
   */
  private fun latchOf(effect: Effect): Pattern? {
    val gain = effect.instruction as? Instruction.Gain ?: return null
    if (gain.quantifier != AMAP) return null
    val marker = pattern(gain.gaining)
    val type = marker.type ?: return null
    val single =
        try {
          table.componentLimits.limitsFor(type).any { it.range.last <= 1 }
        } catch (_: Exception) {
          false
        }
    if (!single || !requiresAbsence(effect.trigger, marker)) return null
    return marker
  }

  private fun requiresAbsence(trigger: Trigger, marker: Pattern): Boolean =
      when (trigger) {
        is Trigger.IfTrigger ->
            conjuncts(trigger.condition).any { requirement ->
              requirement is Requirement.Max &&
                  requirement.maximum == 0 &&
                  (requirement.countedMetric as? Metric.Count)?.let {
                    intersects(pattern(it.expression), marker)
                  } == true
            }
        is Trigger.WrappingTrigger -> requiresAbsence(trigger.inner, marker)
        else -> false
      }

  private fun conjuncts(requirement: Requirement): List<Requirement> =
      if (requirement is Requirement.And) requirement.requirements.flatMap(::conjuncts)
      else listOf(requirement)

  private fun watchedGain(trigger: Trigger): Class? =
      when (trigger) {
        is Trigger.OnGainOf -> table.findClass(trigger.expression.className)
        is Trigger.ByTrigger -> watchedGain(trigger.inner)
        is Trigger.XTrigger -> watchedGain(trigger.inner)
        else -> null
      }

  // THE CLAIM

  /** What performing [task] now does and depends on, including its inseparable reactions. */
  private fun footprintOfClaim(task: Task): Footprint? {
    val claim = Footprint()
    walk(task.instruction, claim, actor = task.assignee)
    // The continuation is admitted only after the task finishes, so it cannot precede it.
    if (claim.unknown != null) return null
    var index = 0
    val automaticSeen = mutableSetOf<String>()
    while (index < claim.events.size) {
      val event = claim.events[index++]
      for (listener in liveListeners) {
        if (!matches(listener.effect.trigger, event)) continue
        // Whether a reaction fires is decided now, by its trigger condition.
        triggerReads(listener.effect, claim)
        if (listener.effect.automatic && automaticSeen.add(listener.effect.toString())) {
          // An automatic reaction is part of this execution.
          walk(listener.effect.instruction, claim)
        }
      }
      // The gained or removed component's own reactions follow the same rule.
      for (effect in selfEffects(event)) {
        triggerReads(effect, claim)
        if (effect.automatic && automaticSeen.add(effect.toString())) {
          walk(effect.instruction, claim)
        }
      }
      if (automaticSeen.size > MAX_WORK) return null
    }
    return claim.takeIf { it.unknown == null }
  }

  /** Effects a component carries for its own gain or removal. */
  private fun selfEffects(event: Event): List<Effect> {
    if (event.pattern.klass == component) throw Unanalyzable("an unidentified change")
    return concreteClasses(event.pattern.klass).flatMap { klass ->
      val context =
          event.pattern.type?.takeIf { it.rootClass == klass }?.expression
              ?: klass.className.expression
      classEffects(klass, context).filter { effect -> selfTriggered(effect.trigger, event.gain) }
    }
  }

  private fun selfTriggered(trigger: Trigger, gain: Boolean): Boolean =
      when (trigger) {
        is Trigger.WhenGain -> gain
        is Trigger.WhenRemove -> !gain
        is Trigger.Or -> trigger.triggers.any { selfTriggered(it, gain) }
        is Trigger.WrappingTrigger -> selfTriggered(trigger.inner, gain)
        is Trigger.SubscribedTrigger -> false
      }

  private fun concreteClasses(klass: Class): List<Class> =
      catalogCache.concrete.getOrPut(klass) {
        if (!klass.abstract) listOf(klass)
        else table.allSubclasses(klass).filter { !it.abstract && table.isInhabited(it) }
      }

  // WORK THAT CAN PRECEDE THE CLAIM

  /** What can happen before the claim: other tasks, their continuations, and reactions. */
  private inner class Before {
    val work = Footprint()
    /** Components this work may bring into play, whose effects could then fire. */
    val possibleListeners = mutableListOf<Listener>()
    /** Live components this work may take out of play. */
    val removedPatterns = mutableListOf<Pattern>()
  }

  private fun workBefore(tasks: List<Task>): Before? {
    // Work blocked by a gate that nothing before the claim can open cannot precede it.
    var candidates = tasks
    while (true) {
      val before = closure(candidates) ?: return null
      val blocked = candidates.filter { blockedWithout(it, candidates - it) }
      if (blocked.isEmpty()) return before
      candidates = candidates - blocked.toSet()
    }
  }

  private val closureCache = mutableMapOf<Set<String>, Before?>()

  private fun closure(tasks: List<Task>): Before? {
    val key = tasks.mapTo(hashSetOf()) { "${it.id}" }
    return closureCache.getOrPut(key) { computeClosure(tasks) }
  }

  private fun computeClosure(tasks: List<Task>): Before? {
    val before = Before()
    class Work(
        val tree: InstructionTree,
        val filtered: Boolean,
        val actor: Actor?,
        val latch: Pattern?,
    )
    val queue = ArrayDeque<Work>()
    val seen = mutableSetOf<String>()
    fun enqueue(
        tree: InstructionTree,
        filtered: Boolean = false,
        actor: Actor? = null,
        latch: Pattern? = null,
    ) {
      if (seen.add("$filtered|$actor|$latch|$tree")) queue += Work(tree, filtered, actor, latch)
    }
    for (task in tasks) {
      enqueue(task.instruction, actor = task.assignee)
      task.then?.let { enqueue(it, actor = task.assignee) }
    }
    var eventIndex = 0
    val installed = mutableSetOf<String>()
    while (queue.isNotEmpty() || eventIndex < before.work.events.size) {
      while (queue.isNotEmpty()) {
        val work = queue.removeFirst()
        walk(work.tree, before.work, work.filtered, work.actor, work.latch)
        if (before.work.unknown != null) return null
        if (seen.size > MAX_WORK) return null
      }
      while (eventIndex < before.work.events.size) {
        val event = before.work.events[eventIndex++]
        if (event.gain) {
          // A gained component's subscriptions become possible listeners.
          for (klass in concreteClasses(event.pattern.klass)) {
            val context =
                event.pattern.type?.takeIf { it.rootClass == klass }?.expression
                    ?: klass.className.expression
            for (effect in subscribedEffects(klass, context)) {
              if (installed.add("$context|$effect")) {
                before.possibleListeners += Listener(Pattern(klass, event.pattern.type), effect)
              }
            }
          }
        } else {
          before.removedPatterns += event.pattern
          dependentRemovals(event.pattern).forEach { dependent ->
            before.work.add(Op.REMOVE_AMAP, dependent)
            before.work.events += Event(gain = false, dependent)
          }
        }
        val filteredSource = event.gain && filteredReveals.any { related(it, event.pattern.klass) }
        for (effect in selfEffects(event)) {
          triggerReads(effect, before.work)
          enqueue(effect.instruction, filteredSource, latch = latchOf(effect))
        }
        for (listener in liveListeners + before.possibleListeners) {
          if (!matches(listener.effect.trigger, event)) continue
          triggerReads(listener.effect, before.work)
          enqueue(listener.effect.instruction, latch = latchOf(listener.effect))
        }
      }
    }
    return before.takeIf { it.work.unknown == null }
  }

  /** Components that depend on a removed component are removed with its last copy. */
  private fun dependentRemovals(removed: Pattern): List<Pattern> =
      if (removed.klass == component) throw Unanalyzable("an unidentified removal")
      else
          catalogCache.dependents.getOrPut(removed.klass) {
            table
                .allClasses()
                .filter { klass ->
                  !klass.abstract &&
                      klass.baseType.typeDependencies.any { dependency ->
                        related(dependency.boundType.rootClass, removed.klass)
                      }
                }
                .map { Pattern(it, null) }
          }

  /**
   * Whether [task] is gated closed now and nothing in [others] could open it, so it cannot run
   * before the claim.
   */
  private fun blockedWithout(task: Task, others: List<Task>): Boolean {
    val gated = task.instruction as? Gated ?: return false
    val open =
        try {
          reader.has(gated.gate)
        } catch (_: Exception) {
          return false
        }
    if (open) return false
    val gate = Footprint()
    gate(gated.gate, gate)
    if (gate.unknown != null) return false
    val opening = closure(others) ?: return false
    return gate.accesses.none { access ->
      when (access.op) {
        // A maximum opens only through removals, a minimum only through gains.
        Op.GATE_MAX ->
            opening.work.events.any { !it.gain && intersects(it.pattern, access.pattern) }
        Op.GATE_MIN -> opening.work.events.any { it.gain && intersects(it.pattern, access.pattern) }
        else -> opening.work.events.any { intersects(it.pattern, access.pattern) }
      }
    }
  }

  // THE COMPARISON

  private fun conflict(task: Task, claim: Footprint, before: Before): String? {
    // A listener that the earlier work brings into play, or takes out, would see the claim's events
    // differently depending on order.
    for (event in claim.events) {
      before.possibleListeners
          .firstOrNull { matches(it.effect.trigger, event) }
          ?.let {
            return "other work can bring `${it.effect}` into play before `${event.pattern}` changes"
          }
      val firing = liveListeners.filter { matches(it.effect.trigger, event) }
      firing
          .firstOrNull { listener ->
            before.removedPatterns.any { intersects(it, listener.source) }
          }
          ?.let {
            return "other work can remove `${it.source}`, whose `${it.effect}` sees this change"
          }
    }
    // Likewise for listeners the claim itself brings into play or takes out.
    for (event in claim.events) {
      for (klass in if (event.gain) concreteClasses(event.pattern.klass) else emptyList()) {
        val context =
            event.pattern.type?.takeIf { it.rootClass == klass }?.expression
                ?: klass.className.expression
        val effects = subscribedEffects(klass, context)
        effects
            .firstOrNull { effect -> before.work.events.any { matches(effect.trigger, it) } }
            ?.let {
              return "this brings `$it` into play, which other work's changes can trigger"
            }
      }
      if (!event.gain) {
        val leaving = liveListeners.filter { intersects(it.source, event.pattern) }
        leaving
            .firstOrNull { listener ->
              before.work.events.any { matches(listener.effect.trigger, it) }
            }
            ?.let {
              return "this removes `${it.source}`, whose `${it.effect}` other work can trigger"
            }
      }
    }
    val counters = counterTypes(task, claim, before)
    val latches = latches(claim, before)
    for (mine in claim.accesses) {
      for (theirs in before.work.accesses) {
        if (!intersects(mine.pattern, theirs.pattern)) continue
        if (counters.any { counter -> covers(counter, mine) && covers(counter, theirs) }) continue
        if (mine.latch != null && theirs.latch != null && mine.latch in latches) continue
        if (conflicts(mine, theirs, before)) return "`$mine` conflicts with other work's `$theirs`"
      }
    }
    return null
  }

  private fun conflicts(mine: Access, theirs: Access, before: Before): Boolean =
      when (mine.op) {
        Op.GAIN -> theirs.op in setOf(Op.GAIN_AMAP, Op.REMOVE_AMAP, Op.READ, Op.GATE_MAX)
        Op.GAIN_AMAP -> theirs.op !in setOf(Op.GATE_MIN, Op.REVEAL)
        Op.REMOVE ->
            when (theirs.op) {
              Op.GAIN,
              Op.GAIN_AMAP ->
                  // A delayed removal can follow a gain that a removal before it needed.
                  before.work.accesses.any {
                    it.op in REMOVALS && intersects(it.pattern, mine.pattern)
                  }
              Op.REMOVE_AMAP,
              Op.READ,
              Op.GATE_MIN -> true
              else -> false
            }
        Op.REMOVE_AMAP -> theirs.op !in setOf(Op.GATE_MAX, Op.REVEAL)
        Op.REMOVE_CHOICE -> true
        Op.READ -> theirs.op in WRITES
        Op.GATE_MIN,
        Op.GATE_MAX -> false
        Op.REVEAL ->
            theirs.op == Op.REVEAL &&
                (mine.filtered ||
                    theirs.filtered ||
                    mine.pattern.type == null ||
                    mine.pattern.type != theirs.pattern.type)
      }

  /**
   * Types that every access treats as an atomized counter raised by its own gains: steps of one
   * track, each gained one at a time, which only gains of that type and the conditions of reactions
   * to those gains touch. Such raises reach the same counts, firing the same reactions, in either
   * order, and clipping at the track's end limits all of them alike. When a reaction to those gains
   * depends on who performed them, every raise must be written in a task of the same Actor as
   * [task].
   */
  private fun counterTypes(task: Task, claim: Footprint, before: Before): List<Pattern> {
    val atomized = atomized ?: return emptyList()
    return claim.accesses
        .filter { it.op == Op.GAIN_AMAP && it.pattern.klass.isSubtypeOf(atomized) }
        .map { it.pattern }
        .filter { counter ->
          val gain = Event(gain = true, counter)
          val byActor =
              (liveListeners + before.possibleListeners).any { listener ->
                matches(listener.effect.trigger, gain) && filtersActor(listener.effect.trigger)
              }
          (claim.accesses + before.work.accesses)
              .filter { intersects(it.pattern, counter) }
              .all { access ->
                when (access.op) {
                  Op.GAIN_AMAP ->
                      access.pattern.klass == counter.klass &&
                          (!byActor || access.actor == task.assignee)
                  Op.READ -> access.threshold != null && related(access.threshold, counter.klass)
                  Op.GATE_MIN -> true
                  else -> false
                }
              }
        }
  }

  /**
   * Latch markers that only latches touch: each access to them reads their absence in a latch's
   * trigger condition or sets them in a latch's reaction. Whether such a marker ends up set does
   * not depend on order, since only qualifying events set it and the first always finds it absent.
   */
  private fun latches(claim: Footprint, before: Before): Set<Pattern> {
    val all = claim.accesses + before.work.accesses
    return all.mapNotNull { it.latch }
        .filterTo(mutableSetOf()) { marker ->
          all.filter { intersects(it.pattern, marker) }
              .all { access ->
                access.latch != null &&
                    intersects(access.latch, marker) &&
                    (access.op == Op.READ || access.op == Op.GAIN_AMAP)
              }
        }
  }

  private fun filtersActor(trigger: Trigger): Boolean =
      when (trigger) {
        is Trigger.ByTrigger -> true
        is Trigger.WrappingTrigger -> filtersActor(trigger.inner)
        is Trigger.Or -> trigger.triggers.any(::filtersActor)
        else -> false
      }

  private fun covers(counter: Pattern, access: Access): Boolean =
      intersects(counter, access.pattern) &&
          (access.op == Op.GAIN_AMAP || access.op == Op.READ || access.op == Op.GATE_MIN)

  public companion object {
    private const val MAX_WORK = 4_000

    private val WRITES = setOf(Op.GAIN, Op.GAIN_AMAP, Op.REMOVE, Op.REMOVE_AMAP, Op.REMOVE_CHOICE)
    private val REMOVALS = setOf(Op.REMOVE, Op.REMOVE_AMAP, Op.REMOVE_CHOICE)

    /** Signals whose card arrivals an external filter selects; see [CardTracking]. */
    private val FILTERED_REVEALS =
        listOf("SearchForCard", "TakeSelectedCard", "ClaimCardReward").map(::cn)

    /**
     * What each Terraforming Mars custom metric reads, from its Kotlin implementation. A metric not
     * listed here reads everything.
     */
    private val CUSTOM_METRIC_READS: Map<ClassName, List<String>> =
        mapOf(
            // Static card text: printed effects.
            cn("GainsOf") to emptyList(),
            // Printed requirement and the global parameters it names.
            cn("GpRequirementShortfall") to listOf("GlobalParameter"),
            // Area geometry and the pieces occupying neighboring areas.
            cn("Neighbor") to listOf("AreaPiece"),
            // Printed placement bonuses: static map data.
            cn("PlacementBonus") to emptyList(),
            // Static card text: price aspects.
            cn("PriceAspectCount") to emptyList(),
        )

    /**
     * Whether performing [task] now, before any other pending work, cannot lose an outcome that
     * performing it later could reach. False whenever the proof cannot be completed.
     */
    public fun isSafe(world: World, task: Task): Boolean =
        SafeTasks(world, CatalogCache()).objection(task) == null

    /** Why [isSafe] rejects [task], or null if it accepts it. */
    public fun objection(world: World, task: Task): String? =
        SafeTasks(world, CatalogCache()).objection(task)

    /** Reuses class-table analysis across SAFE claims in an Agent and its speculative forks. */
    internal fun prover(): (World, Task) -> Boolean {
      val cache = CatalogCache()
      return { world, task -> SafeTasks(world, cache).objection(task) == null }
    }
  }
}
