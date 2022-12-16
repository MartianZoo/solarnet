package dev.martianzoo.tfm.types

data class DependencyMap(val map: Map<DependencyKey, PetClass> = mapOf()) {

  // Let's maybe have two text formats
  // Foo<Bar> -> much to figure out
  // Foo<name=Bar, other=Qux> -- the complete form
  // now the Foo class determines exactly what the keys will be - the union all from supertypes plus its own, but decomposed

  companion object {
  }

  data class DependencyKey(val declaringClass: PetClass, val index: Int) {
    init { require(index > 0) }
    override fun toString() = "${declaringClass.name}_$index"
  }
}
