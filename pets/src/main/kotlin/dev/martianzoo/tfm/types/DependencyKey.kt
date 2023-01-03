package dev.martianzoo.tfm.types

data class DependencyKey(val declaringClass: PetClass, val index: Int, val classDep: Boolean = false) {
  init { require(index >= 0) }

  override fun toString(): String {
    return buildString {
      append(declaringClass.name)
      append('_')
      append(index)
      if (classDep) append("[C]")
    }
  }
}

