package dev.martianzoo.state

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.api.SystemClasses
import dev.martianzoo.pets.ast.Expression

/** A Player or Admin identity whose client can initiate an engine transaction. */
public sealed interface Actor : HasClassName, HasExpression {
  public companion object {
    /** Actor that handles required machinery and neutral game work before ordinary Player tasks. */
    public val ADMIN: Actor = AdminActor
  }

  private data object AdminActor : Actor {
    override val className = SystemClasses.ADMIN
    override val expression: Expression = className.expression

    override fun toString() = className.toString()
  }
}
