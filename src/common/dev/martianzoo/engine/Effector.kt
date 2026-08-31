package dev.martianzoo.engine

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.HashMultiset

/** Maintains the live-effect index and fires matching effects for component changes. */
internal class Effector(
    private val transformers: Transformers,
    readerProvider: () -> GameReader,
) {
  private val reader: GameReader by lazy(readerProvider)
  private val registry = mutableMapOf<LiveEffect.RegistryKey, HashMultiset<LiveEffect>>()

  private val effects = mutableMapOf<Component, List<LiveEffect>>()

  internal fun add(component: Component, delta: Int) =
      liveEffects(component).forEach { effect ->
        if (delta == 0) return@forEach
        val bucket = registry.getOrPut(effect.registryKey, ::HashMultiset)
        bucket.add(effect, delta)
      }

  internal fun mustRemove(component: Component, delta: Int) =
      liveEffects(component).forEach { effect ->
        if (delta == 0) return@forEach
        val key = effect.registryKey
        val bucket = checkNotNull(registry[key])
        bucket.mustRemove(effect, delta)
        if (bucket.isEmpty()) registry.remove(key)
      }

  private fun liveEffects(component: Component): List<LiveEffect> =
      effects.getOrPut(component) { LiveEffect.compile(component, transformers) }

  internal fun fire(triggerEvent: ChangeEvent, automatic: Boolean? = null): List<PendingTask> {
    val resolvedChange =
        LiveEffect.ResolvedChange(
            gaining = triggerEvent.change.gaining?.let(reader::resolve),
            removing = triggerEvent.change.removing?.let(reader::resolve),
        )
    val selfEffects = fireSelfEffects(triggerEvent, automatic, resolvedChange)
    val otherEffects = fireOtherEffects(triggerEvent, automatic, resolvedChange)
    val pending = selfEffects + otherEffects
    return when {
      automatic != true -> pending
      randomAutomaticEffectOrderEnabled -> pending.shuffled()
      else ->
          selfEffects.sortedWith(stableAutomaticOrder) +
              otherEffects.sortedWith(stableAutomaticOrder)
    }
  }

  private fun fireSelfEffects(
      triggerEvent: ChangeEvent,
      automatic: Boolean? = null,
      resolvedChange: LiveEffect.ResolvedChange,
  ): List<PendingTask> =
      listOfNotNull(resolvedChange.gaining, resolvedChange.removing)
          .map(Type::toComponent)
          .flatMap { liveEffects(it) }
          .filter { automatic == null || it.automatic == automatic }
          .mapNotNull { it.onChangeToSelf(triggerEvent, reader, resolvedChange) }

  private fun fireOtherEffects(
      triggerEvent: ChangeEvent,
      automatic: Boolean? = null,
      resolvedChange: LiveEffect.ResolvedChange,
  ): List<PendingTask> =
      candidatesFor(automatic, resolvedChange).mapNotNull { (effect, count) ->
        effect.onChangeToOther(triggerEvent, reader, resolvedChange)?.times(count)
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
            { it.assignee.toString() },
            { it.instruction.toString() },
        )
  }
}
