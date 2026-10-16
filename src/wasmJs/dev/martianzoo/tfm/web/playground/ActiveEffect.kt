package dev.martianzoo.tfm.web.playground

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Component

/** A displayed subscription, retaining its live source and multiplicity. */
internal data class ActiveEffect(val context: Component, val count: Int, val effect: Effect)

/** UI exclusions only: these do not change which effects the engine executes. */
private val hiddenTriggerClasses = listOf("Placement", "UseAction", "Billing", "End")

internal fun activeEffects(table: ClassTable, specimen: Specimen): List<ActiveEffect> {
  val elaborator = PetElaborator(table)
  val hidden = hiddenTriggerClasses.mapNotNull { table.findClass(cn(it)) }
  fun skip(trigger: Trigger): Boolean =
      when (trigger) {
        is Trigger.SelfTrigger -> true
        is Trigger.WrappingTrigger -> skip(trigger.inner)
        is Trigger.Or -> trigger.triggers.all(::skip)
        is Trigger.OnGainOf ->
            hidden.any {
              table.getClass(trigger.expression.className).isSubtypeOf(it)
            }
        is Trigger.OnRemoveOf ->
            hidden.any {
              table.getClass(trigger.expression.className).isSubtypeOf(it)
            }
      }

  return specimen.components.flatMap { entry ->
    val type = table.resolve(Parsing.parse<Expression>(entry.type))
    val context = Component(type)
    elaborator
        .classEffects(type.rootClass)
        .filterNot { skip(it.trigger) }
        .map { effect ->
          ActiveEffect(
              context,
              entry.count,
              elaborator.specializeEffect(
                  type.rootClass.defaultType,
                  type,
                  effect,
                  context.expression,
              ),
          )
        }
  }
}
