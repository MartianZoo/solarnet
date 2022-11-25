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
      this(ClassName(rootType), specialization.toList())

  override val children = listOf(rootType) + specializations + predicates

  override fun toString() =
      "$rootType" +
          specializations.joinOrEmpty(prefix = "<", suffix = ">") +
          predicates.map { "HAS $it" }.joinOrEmpty(prefix = "(", suffix = ")")

  override val hasProd = false

  companion object {
    val DEFAULT = Expression("Megacredit")
  }
}

sealed class RootType : PetaformNode() {
  override val children = listOf<PetaformNode>()
  override val hasProd = false
}

object This : RootType() {
  override fun toString() = "This"
}

data class ClassName(val ctypeName: String) : RootType() {
  init {
    require(ctypeName.matches(Regex("^[A-Z][a-z][A-Za-z0-9_]*$"))) { ctypeName }
    require(ctypeName != This.toString())
    require(ctypeName != "Production")
  }
  override fun toString() = ctypeName
}
