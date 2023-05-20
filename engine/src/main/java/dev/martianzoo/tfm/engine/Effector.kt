package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Game.SnReader
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MType

internal class Effector(
    val reader: SnReader,
    val instructor: Instructor,
    val effects: (Collection<MClass>) -> List<ActiveEffect>,
    val addTasks: (FiredEffect) -> Unit,
) {

  public fun fireMatchingTriggers(triggerEvent: ChangeEvent) {
    val (now, later) = getFiringEffects(triggerEvent).partition { it.automatic }
    for (fx in now) {
      for (instr in split(fx.instruction)) {
        instructor.execute(instr, this, fx.cause)
      }
    }
    later.forEach(addTasks) // TODO why can't we do this before the for loop??
  }

  private fun getFiringEffects(triggerEvent: ChangeEvent): List<FiredEffect> =
      getFiringSelfEffects(triggerEvent) + getFiringOtherEffects(triggerEvent)

  private fun getFiringSelfEffects(triggerEvent: ChangeEvent): List<FiredEffect> =
      componentsIn(triggerEvent)
          .map { Component.ofType(it) }
          .flatMap { it.activeEffects }
          .mapNotNull { it.onChangeToSelf(triggerEvent, reader) }

  private fun getFiringOtherEffects(triggerEvent: ChangeEvent): List<FiredEffect> {
    val classesInvolved = componentsIn(triggerEvent).map { it.root }
    return effects(classesInvolved).mapNotNull {
      it.onChangeToOther(triggerEvent, reader)
    }
  }

  private fun componentsIn(triggerEvent: ChangeEvent): List<MType> =
      listOfNotNull(triggerEvent.change.gaining, triggerEvent.change.removing).map(reader::resolve)
}
