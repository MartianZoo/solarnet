package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetsParser.parseComponents
import dev.martianzoo.tfm.pets.SpecialComponent.COMPONENT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PetClassTest {
  @Test fun nothingness() {
    val loader = loadTypes()
    val cpt = loader["$COMPONENT"]
    assertThat(cpt.name).isEqualTo("$COMPONENT")
    assertThat(cpt.abstract).isTrue()
    assertThat(cpt.directSuperclasses).isEmpty()
    assertThat(cpt.allSuperclasses.names()).containsExactly("$COMPONENT")
    assertThat(cpt.directDependencyKeys).isEmpty()
  }

  @Test fun onethingness() {
    val loader = loadTypes("class Foo")
    val foo = loader["Foo"]
    assertThat(foo.name).isEqualTo("Foo")
    assertThat(foo.abstract).isFalse()
    assertThat(foo.directSuperclasses.names()).containsExactly("$COMPONENT")
    assertThat(foo.allSuperclasses.names()).containsExactly("$COMPONENT", "Foo")
    assertThat(foo.directDependencyKeys).isEmpty()
  }

  @Test fun subclass() {
    val loader = loadTypes("class Foo", "class Bar : Foo")
    val bar = loader["Bar"]
    assertThat(bar.directSuperclasses.names()).containsExactly("Foo")
    assertThat(bar.allSuperclasses.names()).containsExactly("$COMPONENT", "Foo", "Bar")
    assertThat(bar.directDependencyKeys).isEmpty()
  }

  @Test fun forwardReference() {
    val loader = loadTypes("class Bar : Foo", "class Foo")
    val bar = loader["Bar"]
    assertThat(bar.directSuperclasses.names()).containsExactly("Foo")
    assertThat(bar.allSuperclasses.names()).containsExactly("$COMPONENT", "Foo", "Bar")
    assertThat(bar.directDependencyKeys).isEmpty()
  }

  @Test fun cycle() {
    val s = """
      abstract class ${"$COMPONENT"}
      class Foo : Bar
      class Bar : Foo
    """
    assertThrows<IllegalArgumentException> { loader(s) }
  }

  @Test fun trivialCycle() {
    val s = """
      abstract class ${"$COMPONENT"}
      class Foo : Foo
    """
    assertThrows<IllegalArgumentException> { loader(s) }
  }

  @Test fun dependency() {
    val loader = loadTypes("class Foo", "class Bar<Foo>")
    val bar = loader["Bar"]
    assertThat(bar.directSuperclasses.names()).containsExactly("$COMPONENT")
    assertThat(bar.directDependencyKeys).containsExactly(DependencyKey(bar, 0))
  }

  @Test fun inheritedDependency() {
    val loader = loadTypes("class Foo", "class Bar<Foo>", "class Qux : Bar")
    val bar = loader["Bar"]
    val qux = loader["Qux"]
    assertThat(qux.directSuperclasses.names()).containsExactly("Bar")

    val key = DependencyKey(bar, 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test fun restatedDependency() {
    val loader = loadTypes("class Foo", "class Bar<Foo>", "class Qux : Bar<Foo>")
    val bar = loader["Bar"]
    val qux = loader["Qux"]
    assertThat(qux.directSuperclasses.names()).containsExactly("Bar")

    val key = DependencyKey(bar, 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test fun addedDependency() {
    val loader = loadTypes("class Foo", "class Bar<Foo>", "class Baz", "class Qux<Baz> : Bar<Foo>")
    val bar = loader["Bar"]
    val qux = loader["Qux"]

    assertThat(bar.allDependencyKeys).containsExactly(DependencyKey(bar, 0))
    assertThat(qux.allDependencyKeys).containsExactly(DependencyKey(bar, 0),  DependencyKey(qux, 0))
  }

  @Test fun refinedDependency() {
    val loader = loadTypes("class Foo", "class Bar<Foo>", "class Baz : Foo", "class Qux : Bar<Baz>")
    val bar = loader["Bar"]
    val qux = loader["Qux"]
    assertThat(qux.directSuperclasses.names()).containsExactly("Bar")

    val key = DependencyKey(bar, 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test fun cycleDependency() {
    val loader = loadTypes("class Foo<Bar>", "class Bar<Foo>")
    val foo = loader["Foo"]
    val bar = loader["Bar"]
  }

  @Test fun depsAndSpecs() {
    val table = loadTypes(
      "abstract class SuperFoo",
      "abstract class Foo : SuperFoo",
      "class SubFoo : Foo",

      "abstract class SuperBar<SuperFoo>",
      "class Bar : SuperBar<Foo>",
      "class SubBar : Bar<SubFoo>",

      "class Qux")

    // abstract: SuperFoo, SuperBar, Foo
    val supSup = table.resolve("SuperBar<SuperFoo>")
    val supFoo = table.resolve("SuperBar<Foo>")
    val supSub = table.resolve("SuperBar<SubFoo>")
    val barFoo = table.resolve("Bar<Foo>")
    val barSub = table.resolve("Bar<SubFoo>")
    val subSub = table.resolve("SubBar<SubFoo>")

    assertThat(supSup.abstract).isTrue()
    assertThat(supSup.isSubtypeOf(supSup)).isTrue()

    assertThat(supFoo.abstract).isTrue()
    assertThat(supFoo.isSubtypeOf(supSup)).isTrue()
    assertThat(supFoo.isSubtypeOf(supFoo)).isTrue()

    assertThat(supSub.abstract).isTrue()
    assertThat(supSub.isSubtypeOf(supSup)).isTrue()
    assertThat(supSub.isSubtypeOf(supFoo)).isTrue()
    assertThat(supSub.isSubtypeOf(supSub)).isTrue()

    assertThat(barFoo.abstract).isTrue()
    assertThat(barFoo.isSubtypeOf(supSup)).isTrue()
    assertThat(barFoo.isSubtypeOf(supFoo)).isTrue()
    assertThat(barFoo.isSubtypeOf(barFoo)).isTrue()

    assertThat(barSub.abstract).isFalse()
    assertThat(barSub.isSubtypeOf(supSup)).isTrue()
    assertThat(barSub.isSubtypeOf(supFoo)).isTrue()
    assertThat(barSub.isSubtypeOf(supSub)).isTrue()
    assertThat(barSub.isSubtypeOf(barFoo)).isTrue()
    assertThat(barSub.isSubtypeOf(barSub)).isTrue()

    assertThat(subSub.abstract).isFalse()
    assertThat(subSub.isSubtypeOf(supSup)).isTrue()
    assertThat(subSub.isSubtypeOf(supFoo)).isTrue()
    assertThat(subSub.isSubtypeOf(supSub)).isTrue()
    assertThat(subSub.isSubtypeOf(barFoo)).isTrue()
    assertThat(subSub.isSubtypeOf(barSub)).isTrue()
    assertThat(subSub.isSubtypeOf(subSub)).isTrue()

    noWork("Bar<SuperFoo>", table)
    noWork("SubBar<SuperFoo>", table)
    noWork("SubBar<Foo>", table)
    noWork("Foo<Bar>", table)
  }

  private fun noWork(s: String, table: PetClassTable) {
    Assertions.assertThrows(RuntimeException::class.java, { table.resolve("s") }, "s")

  }
  private fun loader(petsText: String) =
      PetClassLoader(parseComponents(petsText)).also { it.loadAll() }

  fun loadTypes(vararg decl: String): PetClassTable {
    return loader("abstract class $COMPONENT\n" + decl.joinToString("") { "$it\n" })
  }

  private fun Iterable<PetClass>.names() = map { it.name }

}
