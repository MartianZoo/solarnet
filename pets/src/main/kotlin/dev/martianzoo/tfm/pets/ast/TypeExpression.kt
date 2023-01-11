package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetParser.parsePets
import dev.martianzoo.util.joinOrEmpty

/**
 * A noun expression. May be a simple type (`ClassName`), a parameterized type
 * (`Foo<Bar, Qux>`) or a *refined* type (`Foo<Bar(HAS 3 Qux)>(HAS Wau)`). A
 * refined type is the combination of a real type with various predicates.
 */
sealed class TypeExpression : PetNode() {
  abstract val className: ClassName

  data class GenericTypeExpression(
      override val className: ClassName,
      val specs: List<TypeExpression> = listOf(),
      val refinement: Requirement? = null,
  ) : TypeExpression() {
    override fun toString() =
        "$className" + specs.joinOrEmpty(wrap = "<>") + (refinement?.let { "(HAS $it)" } ?: "")

    fun specialize(specs: List<TypeExpression>): GenericTypeExpression {
      require(this.specs.isEmpty())
      return copy(specs = specs)
    }
    fun refine(ref: Requirement?): GenericTypeExpression {
      return if (ref == null) {
        this
      } else {
        require(this.refinement == null)
        copy(refinement = ref)
      }
    }
  }

  data class ClassLiteral(override val className: ClassName) : TypeExpression() {
    override fun toString() = "$className.CLASS"
  }

  companion object {
    fun gte(className: ClassName, specs: List<TypeExpression>) =
        GenericTypeExpression(className, specs)
    fun gte(className: String, specs: List<TypeExpression>) = gte(ClassName(className), specs)

    @JvmName("gteFromStrings")
    fun gte(className: String, specs: List<String>) = gte(ClassName(className), specs)

    @JvmName("gteFromStrings")
    fun gte(className: ClassName, specs: List<String>) = gte(className, specs.map(::parsePets))

    fun gte(className: ClassName, first: String, vararg rest: String) =
        gte(className, listOf(first) + rest)

    fun gte(className: String, first: String, vararg rest: String) =
        gte(className, listOf(first) + rest)

    fun gte(className: ClassName, vararg specs: TypeExpression):GenericTypeExpression = gte(className, *specs)
    fun gte(className: String, vararg specs: TypeExpression) = gte(className, specs.toList())

    fun gte(className: ClassName) = gte(className, listOf<TypeExpression>())
    fun gte(className: String) = gte(className, listOf<String>())
  }

  override val kind = "TypeExpression"
}
