package dev.martianzoo.engine

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.SystemClasses.CUSTOM_INSTRUCTION
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.HashMultiset
import dev.martianzoo.pets.util.invoke
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Component
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.toComponent

/** Maintains the live-effect index and fires matching effects for component changes. */
internal class Effector(
    private val elaborator: PetElaborator,
    private val customInstructions: CustomInstructionRuntime,
    readerProvider: () -> GameReader,
) {
  private val reader: Lazy<GameReader> = lazy(readerProvider)

  private val registry = mutableMapOf<LiveEffect.RegistryKey, HashMultiset<LiveEffect>>()

  private val effects = mutableMapOf<Component, List<LiveEffect>>()

  /**
   * Compiles every effect needed to synchronize [change], before authoritative state is changed.
   */
  internal fun prepare(change: ComponentChange) {
    listOfNotNull(change.gaining, change.removing).distinct().forEach(::liveEffects)
  }

  /** Synchronizes the engine's derived effect index after passive state application. */
  internal fun applied(change: ComponentChange) {
    if (change.gaining == change.removing) return
    change.removing?.let { mustRemove(it, change.count) }
    change.gaining?.let { add(it, change.count) }
  }

  internal fun add(component: Component, delta: Int) =
      liveEffects(component).forEach { effect ->
        if (delta == 0) return@forEach
        if (!effect.listensToOtherComponents) return@forEach
        val bucket = registry.getOrPut(effect.registryKey, ::HashMultiset)
        bucket.add(effect, delta)
      }

  internal fun mustRemove(component: Component, delta: Int) =
      liveEffects(component).forEach { effect ->
        if (delta == 0) return@forEach
        if (!effect.listensToOtherComponents) return@forEach
        val key = effect.registryKey
        val bucket = checkNotNull(registry[key])
        bucket.mustRemove(effect, delta)
        if (bucket.isEmpty()) registry.remove(key)
      }

  private fun liveEffects(component: Component): List<LiveEffect> =
      effects.getOrPut(component) { LiveEffect.compile(component, elaborator) }

  /**
   * Returns every matching effect of the requested kind for [triggerEvent].
   *
   * The complete result is materialized before the caller executes any returned effect. Trigger
   * matching, refinements, and trigger-side conditions in one batch therefore all see the same
   * post-event World. A changed component's own effects retain declaration order. The stable or
   * randomized order of independent listeners is diagnostic only and must carry no game meaning.
   */
  internal fun fire(
      triggerEvent: ChangeEvent,
      controller: Actor,
      automatic: Boolean? = null,
  ): List<PendingTask> {
    val resolvedChange =
        LiveEffect.ResolvedChange(
            gaining = triggerEvent.change.gaining?.type,
            removing = triggerEvent.change.removing?.type,
        )
    val selfEffects = fireSelfEffects(triggerEvent, controller, automatic, resolvedChange)
    val otherEffects = fireOtherEffects(triggerEvent, controller, automatic, resolvedChange)
    val pending =
        when {
          automatic != true -> selfEffects + otherEffects
          // A component's own effects retain their authored order. Only independent listeners are
          // siblings for diagnostic randomization and stable display ordering.
          randomAutomaticEffectOrderEnabled -> selfEffects + otherEffects.shuffled()
          else -> selfEffects + otherEffects.sortedWith(stableAutomaticOrder)
        }
    return pending.map { task ->
      task.copy(instruction = InstructionGroup.of(elaborator.atomizeGains(task.instruction)))
    }
  }

  /** The Kotlin output is one queued self-effect of the gained CustomInstruction. */
  private fun customInstructionEffect(triggerEvent: ChangeEvent, controller: Actor): PendingTask? {
    val component = triggerEvent.change.gaining ?: return null
    if (!component.type.rootClass.isSubtypeOf(elaborator.classTable.getClass(CUSTOM_INSTRUCTION))) {
      return null
    }
    val instruction = customInstructions.translateInstruction(component, reader())
    return PendingTask.fromEffect(
        context = component,
        triggerEvent = triggerEvent,
        controller = controller,
        changedComponentPlayer = component.owningPlayer,
        automatic = false,
        instruction = InstructionGroup.of(instruction) * triggerEvent.change.count,
    )
  }

  private fun fireSelfEffects(
      triggerEvent: ChangeEvent,
      controller: Actor,
      automatic: Boolean? = null,
      resolvedChange: LiveEffect.ResolvedChange,
  ): List<PendingTask> {
    val authored =
        listOfNotNull(resolvedChange.gaining, resolvedChange.removing)
            .distinct()
            .map(Type::toComponent)
            .flatMap { liveEffects(it) }
            .filter { automatic == null || it.automatic == automatic }
            .mapNotNull { it.onChangeToSelf(triggerEvent, controller, reader(), resolvedChange) }
    val computed =
        if (automatic != true) listOfNotNull(customInstructionEffect(triggerEvent, controller))
        else emptyList()
    return authored + computed
  }

  private fun fireOtherEffects(
      triggerEvent: ChangeEvent,
      controller: Actor,
      automatic: Boolean? = null,
      resolvedChange: LiveEffect.ResolvedChange,
  ): List<PendingTask> =
      candidatesFor(automatic, resolvedChange).mapNotNull { (effect, count) ->
        effect.onChangeToOther(triggerEvent, controller, reader(), resolvedChange)?.times(count)
      }

  private fun candidatesFor(
      automatic: Boolean?,
      resolvedChange: LiveEffect.ResolvedChange,
  ): List<Pair<LiveEffect, Int>> {
    val changedClasses =
        listOfNotNull(resolvedChange.gaining, resolvedChange.removing)
            .flatMap { it.rootClass.allSuperclasses() }
            .mapTo(linkedSetOf()) { it.className }
    val automaticValues = automatic?.let(::setOf) ?: setOf(true, false)

    // OR and self subscriptions have no single trigger class and remain in the null bucket. The
    // subscription matcher is still authoritative for every selected candidate.
    val candidates = HashMultiset<LiveEffect>()
    automaticValues.forEach { mode ->
      registry[LiveEffect.RegistryKey(mode, null)]?.let(candidates::addAll)
      changedClasses.forEach { changedClass ->
        registry[LiveEffect.RegistryKey(mode, changedClass)]?.let(candidates::addAll)
      }
    }
    return candidates.entries.map { (effect, count) -> effect to count }
  }

  private companion object {
    /** Reproducible history order only; sibling precedence must not depend on this comparator. */
    val stableAutomaticOrder: Comparator<PendingTask> =
        compareBy(
            { it.cause.context.toString() },
            { it.actor.toString() },
            { it.controller.toString() },
            { it.instruction.toString() },
        )
  }
}
