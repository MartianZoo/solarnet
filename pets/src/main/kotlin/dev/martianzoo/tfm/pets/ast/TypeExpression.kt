package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.CLASS_NAME_PATTERN
import dev.martianzoo.tfm.pets.SpecialComponent.CLASS
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.util.joinOrEmpty

/**
 * A noun expression. May be a simple type (`ClassName`), a parameterized type
 * (`Foo<Bar, Qux>`) or a *refined* type (`Foo<Bar(HAS 3 Qux)>(HAS Wau)`). A
 * refined type is the combination of a real type with various predicates.
 */
data class TypeExpression(
    val className: String,
    val specs: List<TypeExpression> = listOf(),
    val refinement: Requirement? = null,
) : PetsNode() {
  constructor(className: String, vararg specialization: TypeExpression) :
      this(className, specialization.toList())
  init {
    require(className.matches(Regex(CLASS_NAME_PATTERN))) { className }
    if (className == "$CLASS") { // TODO what about This?
      require(specs.size <= 1) { specs }
      require(specs.isEmpty() || specs.first().isClassOnly())
      require(refinement == null)
    }
  }

  // TODO props
  fun isTypeOnly(): Boolean =
      refinement == null && specs.all { it.isTypeOnly() }

  fun isClassOnly() = (this != THIS.type && this == te(className)) // TODO why This?

  //fun pureTypeOnly(): TypeExpression =
  //    copy(refinement = null, specs = specs.map { it.pureTypeOnly() })

  override fun toString() =
      className +
      specs.joinOrEmpty(wrap="<>") +
      (refinement?.let { "(HAS $it)" } ?: "")

  companion object {
    fun te(s: String) : TypeExpression = TypeExpression(s)
    fun te(s: String, specs: List<String>, ref: Requirement? = null): TypeExpression =
        TypeExpression(s, specs.map(::te), ref)
    fun te(s: String, first: TypeExpression, vararg rest: TypeExpression): TypeExpression =
        TypeExpression(s, listOf(first) + rest)
    fun te(s: String, first: String, vararg rest: String) =
        te(s, listOf(first) + rest)
  }

  override val kind = "TypeExpression"
}

