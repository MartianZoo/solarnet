package dev.martianzoo.tfm.types

data class DependencyKey(val declaringClass: PetClass, val index: Int) {
  init { require(index >= 0) }

  override fun toString() = "${declaringClass.name}_$index"
}

