package dev.martianzoo.engine

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.Actor
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
  private val registryOrder = mutableMapOf<LiveEffect, Long>()
  private var nextRegistryOrder = 0L

  private val effects = mutableMapOf<Component, List<LiveEffect>>()

  internal fun add(component: Component, delta: Int) =
      liveEffects(component).forEach { effect ->
        if (delta == 0) return@forEach
        val bucket = registry.getOrPut(effect.registryKey, ::HashMultiset)
        if (bucket.count(effect) == 0) registryOrder[effect] = nextRegistryOrder++
        bucket.add(effect, delta)
      }

  internal fun mustRemove(component: Component, delta: Int) =
      liveEffects(component).forEach { effect ->
        if (delta == 0) return@forEach
        val key = effect.registryKey
        val bucket = checkNotNull(registry[key])
        if (bucket.mustRemove(effect, delta) == 0) registryOrder.remove(effect)
        if (bucket.isEmpty()) registry.remove(key)
      }

  private fun liveEffects(component: Component): List<LiveEffect> =
      effects.getOrPut(component) { LiveEffect.compile(component, transformers) }

  internal fun fire(
      triggerEvent: ChangeEvent,
      controller: Actor,
      automatic: Boolean? = null,
  ): List<PendingTask> {
    val resolvedChange =
        LiveEffect.ResolvedChange(
            gaining = triggerEvent.change.gaining?.let(reader::resolve),
            removing = triggerEvent.change.removing?.let(reader::resolve),
        )
    val pending =
        fireSelfEffects(triggerEvent, controller, automatic, resolvedChange) +
            fireOtherEffects(triggerEvent, controller, automatic, resolvedChange)
    return if (automatic == true && randomAutomaticEffectOrderEnabled) pending.shuffled()
    else pending
  }

  private fun fireSelfEffects(
      triggerEvent: ChangeEvent,
      controller: Actor,
      automatic: Boolean? = null,
      resolvedChange: LiveEffect.ResolvedChange,
  ): List<PendingTask> =
      listOfNotNull(resolvedChange.gaining, resolvedChange.removing)
          .map(Type::toComponent)
          .flatMap { liveEffects(it) }
          .filter { automatic == null || it.automatic == automatic }
          .mapNotNull { it.onChangeToSelf(triggerEvent, controller, reader, resolvedChange) }

  private fun fireOtherEffects(
      triggerEvent: ChangeEvent,
      controller: Actor,
      automatic: Boolean? = null,
      resolvedChange: LiveEffect.ResolvedChange,
  ): List<PendingTask> =
      candidatesFor(automatic, resolvedChange).mapNotNull { (effect, count) ->
        effect.onChangeToOther(triggerEvent, controller, reader, resolvedChange)?.times(count)
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
    // Effects can be selected through different superclass buckets. Preserve the old registry's
    // insertion order because automatic effects may observe changes made by earlier effects.
    return candidates.entries
        .sortedBy { (effect, _) -> checkNotNull(registryOrder[effect]) }
        .map { (effect, count) -> effect to count }
  }
}
