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
    val ctypeName: String,
    val refinements: List<Expression> = listOf()) : PetaformObject {

  constructor(ctypeName: String, vararg refinement: Expression) :
      this(ctypeName, refinement.toList())

  override val asSource : String =
      ctypeName + refinements.map { it.asSource }.joinOrEmpty(prefix = "<", suffix = ">")
}
