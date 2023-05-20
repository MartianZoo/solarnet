package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.Game.SnReader
import dev.martianzoo.util.HashMultiset

internal class Effector {

  private val registry = HashMultiset<ActiveEffect>()

  fun update(effect: ActiveEffect, delta: Int) {
    registry.setCount(effect, registry.count(effect) + delta)
  }

  internal fun fire(triggerEvent: ChangeEvent, reader: SnReader): List<FiredEffect> =
      fireSelfEffects(triggerEvent, reader) + fireOtherEffects(triggerEvent, reader)

  private fun fireSelfEffects(triggerEvent: ChangeEvent, reader: SnReader): List<FiredEffect> =
      listOfNotNull(triggerEvent.change.gaining, triggerEvent.change.removing)
          .map(reader::resolve)
          .map { it.toComponent() }
          .flatMap { it.activeEffects }
          .mapNotNull { it.onChangeToSelf(triggerEvent, reader) }

  private fun fireOtherEffects(triggerEvent: ChangeEvent, reader: SnReader): List<FiredEffect> =
      registry.entries.mapNotNull { (fx, ct) ->
        fx.onChangeToOther(triggerEvent, reader)?.times(ct)
      }

}
