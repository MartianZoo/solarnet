package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.engine.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.Game.SnReader
import dev.martianzoo.util.HashMultiset

internal class Effector {

  private val registry = HashMultiset<ActiveEffect>()

  lateinit var reader: SnReader // for conditionals, refinements, specialization

  fun update(component: Component, delta: Int) {
    component.activeEffects.forEach { registry.setCount(it, registry.count(it) + delta) }
  }

  fun fire(triggerEvent: ChangeEvent): List<Task> =
      fireSelfEffects(triggerEvent) + fireOtherEffects(triggerEvent)

  private fun fireSelfEffects(triggerEvent: ChangeEvent): List<Task> =
      listOfNotNull(triggerEvent.change.gaining, triggerEvent.change.removing)
          .map(reader::resolve)
          .map { it.toComponent() }
          .flatMap { it.activeEffects }
          .mapNotNull { it.onChangeToSelf(triggerEvent, reader) }

  private fun fireOtherEffects(triggerEvent: ChangeEvent): List<Task> =
      registry.entries.mapNotNull { (fx, ct) ->
        fx.onChangeToOther(triggerEvent, reader)?.times(ct)
      }
}
