package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parser.parseComponents
import dev.martianzoo.tfm.pets.rootName
import dev.martianzoo.tfm.types.PetType.DependencyKey
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PetClassTest {
  @Test fun nothingness() {
    val loader = loader("abstract class $rootName")
    val cpt = loader.get(rootName)
    assertThat(cpt.name).isEqualTo(rootName)
    assertThat(cpt.abstract).isTrue()
    assertThat(cpt.directSuperclasses).isEmpty()
    assertThat(cpt.allSuperclasses.names()).containsExactly(rootName)
    assertThat(cpt.directDependencyKeys).isEmpty()
  }

  @Test fun onethingness() {
    val loader = loader("""
      abstract class $rootName
      class Foo
    """)
    val foo = loader.get("Foo")
    assertThat(foo.name).isEqualTo("Foo")
    assertThat(foo.abstract).isFalse()
    assertThat(foo.directSuperclasses.names()).containsExactly(rootName)
    assertThat(foo.allSuperclasses.names()).containsExactly(rootName, "Foo")
    assertThat(foo.directDependencyKeys).isEmpty()
  }

  @Test fun subclass() {
    val loader = loader("""
      abstract class $rootName
      class Foo
      class Bar : Foo
    """)
    val bar = loader.get("Bar")
    assertThat(bar.directSuperclasses.names()).containsExactly("Foo")
    assertThat(bar.allSuperclasses.names()).containsExactly(rootName, "Foo", "Bar")
    assertThat(bar.directDependencyKeys).isEmpty()
  }

  @Test fun forwardReference() {
    val loader = loader("""
      abstract class $rootName
      class Bar : Foo
      class Foo
    """)
    val bar = loader.get("Bar")
    assertThat(bar.directSuperclasses.names()).containsExactly("Foo")
    assertThat(bar.allSuperclasses.names()).containsExactly(rootName, "Foo", "Bar")
    assertThat(bar.directDependencyKeys).isEmpty()
  }

  @Test fun cycle() {
    val s = """
      abstract class $rootName
      class Foo : Bar
      class Bar : Foo
    """
    assertThrows<IllegalArgumentException> { loader(s) }
  }

  @Test fun trivialCycle() {
    val s = """
      abstract class $rootName
      class Foo : Foo
    """
    assertThrows<IllegalArgumentException> { loader(s) }
  }

  @Test fun dependency() {
    val loader = loader("""
      abstract class $rootName
      class Foo
      class Bar<Foo>
    """)
    val bar = loader.get("Bar")
    assertThat(bar.directSuperclasses.names()).containsExactly(rootName)
    assertThat(bar.directDependencyKeys).containsExactly(DependencyKey(bar, 0))
  }

  @Test fun inheritedDependency() {
    val loader = loader("""
      abstract class $rootName
      class Foo
      class Bar<Foo>
      class Qux : Bar
    """)
    val bar = loader.get("Bar")
    val qux = loader.get("Qux")
    assertThat(qux.directSuperclasses.names()).containsExactly("Bar")

    val key = DependencyKey(bar, 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test fun restatedDependency() {
    val loader = loader("""
      abstract class $rootName
      class Foo
      class Bar<Foo>
      class Qux : Bar<Foo>
    """)
    val bar = loader.get("Bar")
    val qux = loader.get("Qux")
    assertThat(qux.directSuperclasses.names()).containsExactly("Bar")

    val key = DependencyKey(bar, 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test fun addedDependency() {
    val loader = loader("""
      abstract class $rootName
      class Foo
      class Bar<Foo>
      class Baz
      class Qux<Baz> : Bar<Foo>
    """)
    val bar = loader.get("Bar")
    val qux = loader.get("Qux")

    assertThat(bar.allDependencyKeys).containsExactly(DependencyKey(bar, 0))
    assertThat(qux.allDependencyKeys).containsExactly(DependencyKey(bar, 0),  DependencyKey(qux, 0))
  }

  @Test fun refinedDependency() {
    val loader = loader("""
      abstract class $rootName
      class Foo
      class Bar<Foo>
      class Baz : Foo
      class Qux : Bar<Baz>
    """)
    val bar = loader.get("Bar")
    val qux = loader.get("Qux")
    assertThat(qux.directSuperclasses.names()).containsExactly("Bar")

    val key = DependencyKey(bar, 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test fun cycleDependency() {
    val loader = loader("""
      abstract class $rootName
      class Foo<Bar>
      class Bar<Foo>
    """)
    val foo = loader.get("Foo")
    val bar = loader.get("Bar")
  }

  @Test fun depsAndSpecs() {
    val loader = loader("""
      abstract class $rootName
      class SuperFoo
      class Foo : SuperFoo
      class SubFoo : Foo

      class SuperBar<SuperFoo>
      class Bar : SuperBar<Foo>
      class SubBar : Bar<SubFoo>

      class Qux
    """)

    loader.resolve("SuperBar<SuperFoo>")
    loader.resolve("SuperBar<Foo>")
    loader.resolve("SuperBar<SubFoo>")
    loader.resolve("Bar<Foo>")
    loader.resolve("Bar<SubFoo>")
    loader.resolve("SubBar<SubFoo>")

    assertThrows<RuntimeException> { loader.resolve("Bar<Qux>") }
    // assertThrows<RuntimeException> { loader.resolve("Bar<SuperFoo>") }
    //assertThrows<RuntimeException> { loader.resolve("Foo<Bar>") }
  }


  private fun loader(petsText: String) =
      PetClassLoader(parseComponents(petsText)).also { it.loadAll() }

  private fun Iterable<PetClass>.names() = map { it.name }

}
