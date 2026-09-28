package dev.martianzoo.pets.ast

import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.TypeInfo

/**
 * A value or abstract value type assigned to a class property by `name = value`. Per
 * [rule L11-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations)
 * the right-hand side is one of the bound words `Number`, `Metric`, `Requirement` and
 * `Requirement?`, a literal non-negative number, a metric quoted after `COUNT`, or a requirement
 * quoted after `HAS`. What these bounds and values mean is
 * [section 9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties)
 * of the type system specification.
 */
public sealed class PropertyValue : PetNode(), Specification<PropertyValue> {

  /** Whether this is an abstract property type rather than a concrete value. */
  public val abstract: Boolean
    get() =
        this === MetricType ||
            this === NumberType ||
            this === RequirementType ||
            this === OptionalRequirementType

  override fun isAbstract(info: TypeInfo): Boolean = abstract

  /** The abstract type of any numeric property. */
  public data object MetricType : PropertyValue() {
    override fun toString(): String = "Metric"
  }

  /** The abstract type of a numeric property restricted to a world-independent literal. */
  public data object NumberType : PropertyValue() {
    override fun toString(): String = "Number"
  }

  /** The abstract type of a requirement-valued property. */
  public data object RequirementType : PropertyValue() {
    override fun toString(): String = "Requirement"
  }

  /** The abstract type of a requirement-valued property that a concrete class may omit. */
  public data object OptionalRequirementType : PropertyValue() {
    override fun toString(): String = "Requirement?"
  }

  /** The effective value of an omitted optional Requirement property on a concrete class. */
  public data object AbsentRequirementValue : PropertyValue() {
    override fun toString(): String = "<absent Requirement>"
  }

  /** One concrete, non-negative, world-independent property value. */
  public data class NumberValue(public val value: Int) : PropertyValue() {
    init {
      require(value >= 0) { "number property cannot be negative: `$value`" }
    }

    override fun toString(): String = "$value"
  }

  /** One concrete metric-valued property, written as a quoted Metric after `COUNT`. */
  public data class MetricValue(public val value: Metric) : PropertyValue() {
    override fun toString(): String = "COUNT \"$value\""
  }

  /** One concrete requirement-valued property, written as a quoted Requirement after `HAS`. */
  public data class RequirementValue(public val value: Requirement) : PropertyValue() {
    override fun toString(): String = "HAS \"$value\""
  }

  override fun ensureNarrows(that: PropertyValue, info: TypeInfo) {
    if (this != that && !that.accepts(this)) {
      throw NarrowingException("property value `$this` does not narrow `$that`")
    }
  }

  /** Whether this abstract bound may be narrowed directly to [value]. */
  private fun accepts(value: PropertyValue): Boolean =
      when (this) {
        MetricType -> value === NumberType || value is NumberValue || value is MetricValue
        NumberType -> value is NumberValue
        RequirementType -> value is RequirementValue
        OptionalRequirementType -> value === RequirementType || value is RequirementValue
        AbsentRequirementValue,
        is NumberValue,
        is MetricValue,
        is RequirementValue -> false
      }

  override val kind: kotlin.reflect.KClass<out PetNode> = PropertyValue::class

  override fun visitChildren(visitor: Visitor): Unit =
      when (this) {
        MetricType -> Unit
        NumberType,
        RequirementType,
        OptionalRequirementType,
        AbsentRequirementValue,
        is NumberValue -> Unit
        is MetricValue -> visitor.visit(value)
        is RequirementValue -> visitor.visit(value)
      }
}
