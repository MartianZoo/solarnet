package dev.martianzoo.tfm.pets

import com.google.common.base.CaseFormat.UPPER_CAMEL
import com.google.common.base.CaseFormat.UPPER_UNDERSCORE
import com.google.common.base.Converter
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

enum class SpecialComponent {
  COMPONENT,
  THIS,
  DEFAULT,
  OK,

  PRODUCTION,
  STANDARD_RESOURCE,
  USE_ACTION,

  END,
  ;

  private val converter: Converter<String, String> = UPPER_UNDERSCORE.converterTo(UPPER_CAMEL)
  override fun toString() = converter.convert(super.toString())!!

  val type: GenericTypeExpression = gte(toString())
}
