package dev.martianzoo.tfm.web.playground

import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameWorld

/** Subscriptions present just before BootstrapPhase ends, independent of copy counts. */
internal fun bootstrapEffects(premise: GamePremise, events: List<GameEvent>): Set<Effect> {
  val world = GameWorld(premise)
  for (event in events) {
    if (event is ChangeEvent && event.change.removing?.className == cn("BootstrapPhase")) {
      val specimen =
          liveSpecimen(
              "Bootstrap",
              world.reader,
              world.events,
              world.tasks,
              event.ordinal,
              emptyList(),
              emptyMap(),
          )
      return activeEffects(world.classTable, specimen).mapTo(hashSetOf()) { it.effect }
    }
    world.apply(event)
  }
  return emptySet()
}
