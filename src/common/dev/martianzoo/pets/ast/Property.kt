package dev.martianzoo.pets.ast

/**
 * Reads one numeric class property, spelled `receiver.name` ([rule
 * L4-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-metrics)). A
 * property with no receiver takes one from the enclosing refinement candidate or context.
 */
public data class Property(
    /** Which property to read. */
    public val propertyName: PropertyName,

    /** Whose property to read, or null to take one from the enclosing candidate or context. */
    public val receiver: Expression? = null,
) : Metric() {

  override fun visitChildren(visitor: Visitor): Unit = visitor.visit(propertyName, receiver)

  override fun toString(): String =
      if (receiver == null) "$propertyName" else "$receiver.$propertyName"

  override fun precedence(): Int = 12
}
