package dev.martianzoo.tfm.petaform.api

import dev.martianzoo.util.joinOrEmpty

/**
 * A component type expression in Petaform. This is parsed purely textually, with no knowledge of
 * the symbol table. In adding TfmObjects to a `ComponentTable`, all expressions are checked at
 * that time.
 */
data class Expression(
    val rootType: RootType,
    val specializations: List<Expression> = listOf(),
    val predicates: List<Predicate> = listOf()) : PetaformNode() {

  constructor(rootType: RootType, vararg specialization: Expression) :
      this(rootType, specialization.toList())
  constructor(rootType: String, vararg specialization: Expression) :
      this(RootType(rootType), specialization.toList())

  override val children = listOf(rootType) + specializations + predicates

  override fun toString() =
      "$rootType" +
          specializations.joinOrEmpty(surround = "<>") +
          predicates.map { "HAS $it" }.joinOrEmpty(surround = "()")

  override val hasProd = false

  companion object {
    val DEFAULT = Expression("Megacredit")
  }
}

data class RootType(val ctypeName: String) : PetaformNode() {
  override val children = listOf<PetaformNode>()
  override val hasProd = false
  init {
    require(ctypeName.matches(Regex("^[A-Z][a-z][A-Za-z0-9_]*$"))) { ctypeName }
//    require(ctypeName != "Production")
  }
  override fun toString() = ctypeName
}
