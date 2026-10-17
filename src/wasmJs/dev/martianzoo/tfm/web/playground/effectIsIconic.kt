package dev.martianzoo.tfm.web.playground

import dev.martianzoo.pets.api.SystemClasses.ICONIC
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.types.ClassTable

/** Only watched or changed root classes count, not dependencies, conditions, or metrics. */
internal fun effectIsIconic(effect: Effect, table: ClassTable): Boolean {
  val iconic = table.getClass(ICONIC)
  fun isIconic(expression: Expression): Boolean =
      table.getClass(expression.className).isSubtypeOf(iconic)
  val watched =
      effect.trigger.descendantsOfType<Trigger>().mapNotNull {
        when (it) {
          is Trigger.OnGainOf -> it.expression
          is Trigger.OnRemoveOf -> it.expression
          else -> null
        }
      }
  return watched.any(::isIconic) ||
      effect.instruction.descendantsOfType<Change>().any {
        listOfNotNull(it.gaining, it.removing).any(::isIconic)
      }
}
