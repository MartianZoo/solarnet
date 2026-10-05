package dev.martianzoo.pets.ast

/**
 * A lowerCamelCase name identifying one class property: a lowercase letter followed by letters and
 * digits ([rule
 * L10-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-names)).
 */
public data class PropertyName(public val value: String) : PetNode() {
  internal companion object {
    private val propertyNameRegex = Regex("[a-z][A-Za-z0-9]*")
  }

  init {
    require(value.matches(propertyNameRegex)) { "invalid property name: `$value`" }
  }

  override fun toString(): String = value

  override val kind: kotlin.reflect.KClass<out PetNode> = PropertyName::class

  override fun visitChildren(visitor: Visitor): Unit = Unit
}
