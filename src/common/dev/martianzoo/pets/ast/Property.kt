package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.PetTokenizer

/**
 * Reads one numeric class property, spelled `receiver.name` ([rule
 * L5-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics)). A
 * property with no receiver takes one from the enclosing refinement candidate or context.
 */
public data class Property(
    /** Which property to read. */
    public val propertyName: PropertyName,

    /** Whose property to read, or null to take one from the enclosing candidate or context. */
    public val receiver: Expression? = null,
) : Metric() {
  internal companion object {
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
