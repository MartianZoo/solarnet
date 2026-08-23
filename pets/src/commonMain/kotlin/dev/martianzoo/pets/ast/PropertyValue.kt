package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetTokenizer

/** A value or abstract value type assigned to a class property. */
public sealed class PropertyValue : PetNode() {
  public companion object {
    internal fun parser(): Parser<PropertyValue> = Parsers.parser
  }

  /** Whether this is an abstract property type rather than a concrete value. */
  public val abstract: Boolean
    get() =
        this === MetricType ||
            this === NumberType ||
            this === RequirementType ||
            this === OptionalRequirementType

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
      require(value >= 0) { "Number property cannot be negative: $value" }
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

  /** Whether this abstract bound may be narrowed directly to [value]. */
  internal fun accepts(value: PropertyValue): Boolean =
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

  private object Parsers : PetTokenizer() {
    val requirement: Parser<PropertyValue> =
        _has and
            quotedText map
            { (_, source) ->
              RequirementValue(parse<Requirement>(source))
            }

    val parser: Parser<PropertyValue> =
        (_metric map { MetricType }) or
            (_number map { NumberType }) or
            (_requirement and skipChar('?') map { OptionalRequirementType }) or
            (_requirement map { RequirementType }) or
            requirement or
            (skip(_count) and quotedText map { MetricValue(parse<Metric>(it)) }) or
            (rawScalar map { NumberValue(it) })
  }
}
