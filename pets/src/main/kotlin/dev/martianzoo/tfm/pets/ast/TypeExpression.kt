package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.classNameRegex
import dev.martianzoo.util.joinOrEmpty

/**
 * A noun expression. May be a simple type (`ClassName`), a parameterized type
 * (`Foo<Bar, Qux>`) or a *refined* type (`Foo<Bar(HAS 3 Qux)>(HAS Wau)`). A
 * refined type is the combination of a real type with various predicates.
 */
sealed class TypeExpression : PetsNode() {
  abstract val className: String

  data class GenericTypeExpression(
      override val className: String,
      val specs: List<TypeExpression> = listOf(),
      val refinement: Requirement? = null,
  ) : TypeExpression() {
    init {
      require(className.matches(classNameRegex())) { className }
    }

    override fun toString() =
        className + specs.joinOrEmpty(wrap = "<>") + (refinement?.let { "(HAS $it)" } ?: "")
  }

  data class ClassExpression(override val className: String) : TypeExpression() {
    init {
      require(className.matches(classNameRegex())) { className }
    }

    override fun toString() = "$className.CLASS"
  }

  companion object {
    fun gte(s: String) = GenericTypeExpression(s)

    @JvmName("whoCares")
    fun gte(s: String, specs: List<String>, ref: Requirement?): GenericTypeExpression =
        GenericTypeExpression(s, specs.map(::gte), ref)

    fun gte(s: String, first: TypeExpression, vararg rest: TypeExpression) =
        GenericTypeExpression(s, listOf(first) + rest)

    fun gte(s: String, first: String, vararg rest: String) = gte(s, listOf(first) + rest, null)

    fun gte(s: String, specs: List<TypeExpression>, ref: Requirement?) =
        GenericTypeExpression(s, specs, ref)
  }

  override val kind = "TypeExpression"
}
