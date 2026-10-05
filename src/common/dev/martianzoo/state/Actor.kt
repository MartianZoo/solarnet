package dev.martianzoo.state

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.api.SystemClasses
import dev.martianzoo.pets.ast.Expression

/** An identity that can initiate or continue game operations. */
public sealed interface Actor : HasClassName, HasExpression {
  public companion object {
    public val ADMIN: Actor = AdminActor
  }

  private data object AdminActor : Actor {
    override val className = SystemClasses.ADMIN
    override val expression: Expression = className.expression

    override fun toString() = className.toString()
  }
}
