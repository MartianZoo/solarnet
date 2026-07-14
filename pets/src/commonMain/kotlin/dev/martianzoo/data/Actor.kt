package dev.martianzoo.data

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/** An identity that can initiate or continue game operations. */
sealed class Actor : HasClassName, HasExpression {
  final override val expression by lazy { className.expression }
  final override val expressionFull by ::expression

  final override fun toString() = className.toString()

  private data class NonPlayerActor(override val className: ClassName) : Actor()

  companion object {
    val ENGINE: Actor = NonPlayerActor(cn("Engine"))

    fun from(className: ClassName): Actor =
        if (className == ENGINE.className) ENGINE else Player(className)
  }
}
