package dev.martianzoo.engine

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.HashMultiset
import dev.martianzoo.pets.util.invoke
import dev.martianzoo.state.Component
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.toComponent

/** Maintains the live-effect index and fires matching effects for component changes. */
internal class Effector(
    private val elaborator: PetElaborator,
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
    val pending = selfEffects + otherEffects
    return when {
      automatic != true -> pending
      // A component's own effects retain their authored order. Only independent listeners are
      // siblings for diagnostic randomization and stable display ordering.
      randomAutomaticEffectOrderEnabled -> selfEffects + otherEffects.shuffled()
      else -> selfEffects + otherEffects.sortedWith(stableAutomaticOrder)
    }
  }

  private fun fireSelfEffects(
      triggerEvent: ChangeEvent,
      controller: Actor,
      automatic: Boolean? = null,
      resolvedChange: LiveEffect.ResolvedChange,
  ): List<PendingTask> =
      listOfNotNull(resolvedChange.gaining, resolvedChange.removing)
          .distinct()
          .map(Type::toComponent)
          .flatMap { liveEffects(it) }
          .filter { automatic == null || it.automatic == automatic }
          .mapNotNull { it.onChangeToSelf(triggerEvent, controller, reader(), resolvedChange) }

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
