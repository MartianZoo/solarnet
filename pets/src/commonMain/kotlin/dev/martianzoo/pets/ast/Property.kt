package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.PetTokenizer

/** Reads one numeric property from [receiver], which may be supplied later by refinement. */
public data class Property(
    public val propertyName: PropertyName,
    public val receiver: Expression? = null,
) : Metric() {
  public companion object {
    internal fun parser(): Parser<Property> = Parsing.parser
  }

  override fun visitChildren(visitor: Visitor): Unit = visitor.visit(propertyName, receiver)

  override fun toString(): String =
      if (receiver == null) "$propertyName" else "$receiver.$propertyName"

  override fun precedence(): Int = 12

  private object Parsing : PetTokenizer() {
    private val explicit: Parser<Property> =
        Expression.parser() and
            skipChar('.') and
            PropertyName.parser() map
            { (receiver, name) ->
              Property(name, receiver)
            }

    val parser: Parser<Property> = explicit or (PropertyName.parser() map ::Property)
  }
}
