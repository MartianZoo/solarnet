package dev.martianzoo.tfm.petaform.api

import dev.martianzoo.util.joinOrEmpty

/**
 * A component type expression in Petaform. This is parsed purely textually, with no knowledge of
 * the symbol table.
 *
 * @param ctypeName the root type, e.g. `"Foo"` for the expression `Foo<Bar, Qux>`
 * @param refinements the ordered list of refinements, e.g. `["Bar", "Qux"]` for the same example
 */
data class Expression(
    val rootType: RootType,
    val refinements: List<Expression> = listOf(),
    val predicates: List<Predicate> = listOf()) : PetaformObject {

  constructor(rootType: RootType, vararg refinement: Expression) :
      this(rootType, refinement.toList())
  constructor(rootType: String, vararg refinement: Expression) :
      this(ClassName(rootType), refinement.toList())

  override val petaform : String =
      rootType.petaform +
          refinements.map { it.petaform }.joinOrEmpty(prefix = "<", suffix = ">") +
          predicates.map { "HAS ${it.petaform}" }.joinOrEmpty(prefix = "(", suffix = ")")

  companion object {
    val DEFAULT = Expression("Megacredit")
  }
}

sealed interface RootType : PetaformObject

object This : RootType {
  override val petaform = "This"
}

data class ClassName(val ctypeName: String) : RootType {
  init {
    require(ctypeName.matches(Regex("^[A-Z][a-z][A-Za-z0-9_]*$"))) { ctypeName }
    require(ctypeName != This.petaform)
    require(ctypeName != "Production")
  }
  override val petaform = ctypeName
}
