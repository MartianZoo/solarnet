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
    val predicate: Predicate? = null) : PetaformNode() {

  constructor(rootType: RootType, vararg specialization: Expression) :
      this(rootType, specialization.toList())
  constructor(rootType: String, vararg specialization: Expression) :
      this(RootType(rootType), specialization.toList())

  override val children = listOfNotNull(predicate) + specializations + rootType

  override fun toString() =
      "$rootType" +
      specializations.joinOrEmpty(surround = "<>") +
      (predicate?.let { "(HAS $it)" } ?: "")


  companion object {
    val DEFAULT = Expression("Megacredit")
  }
}

data class RootType(val name: String) : PetaformNode() {
  override val children = listOf<PetaformNode>()
  init {
    require(name.matches(NAME_REGEX)) { name }
  }
  override fun toString() = name
}

val NAME_REGEX = Regex("^[A-Z][a-z][A-Za-z0-9_]*$")
