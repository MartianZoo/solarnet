package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.SystemClasses.PLAYER
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Dispatch
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player

/** The participating Actor that [expression] names, for an [assignment] such as `` `BY` ``. */
internal fun GameReader.actorNamed(expression: Expression, assignment: String): Actor {
  val type = resolve(expression)
  if (countComponent(type) != 1) {
    throw ExpressionException(
        "$assignment requires a participating `Actor`; `${type.expression}` has no component in this world",
        sourceLocation = expression.sourceLocation,
    )
  }
  if (type.className == ADMIN.className) return ADMIN
  if (type.rootClass.isSubtypeOf(classTable.getClass(PLAYER))) return Player(type.className)
  throw ExpressionException("unsupported Actor: ${type.expression}")
}

/** The fixed Actor that receives [dispatch]'s work (L2-17). */
internal fun GameReader.recipientOf(dispatch: Dispatch): Actor {
  val keyword = "`${dispatch.deferral.keyword}`"
  val recipient =
      dispatch.actor
          ?: throw ExpressionException(
              "$keyword needs a recipient after elaboration",
              sourceLocation = dispatch.sourceLocation,
          )
  if (resolve(recipient).abstract) {
    throw ExpressionException(
        "$keyword requires a fixed Actor recipient; `$recipient` is open",
        sourceLocation = recipient.sourceLocation ?: dispatch.sourceLocation,
    )
  }
  return actorNamed(recipient, keyword)
}
