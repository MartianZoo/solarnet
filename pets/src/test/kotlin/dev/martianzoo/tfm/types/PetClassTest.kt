package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parser.parseComponents
import dev.martianzoo.tfm.pets.rootName
import dev.martianzoo.tfm.types.DependencyMap.DependencyKey
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
    assertThat(foo.allSuperclasses.names()).containsExactly(rootName, "Foo").inOrder()
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
    assertThat(bar.allSuperclasses.names()).containsExactly(rootName, "Foo", "Bar").inOrder()
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
    assertThat(bar.allSuperclasses.names()).containsExactly(rootName, "Foo", "Bar").inOrder()
    assertThat(bar.directDependencyKeys).isEmpty()
  }

  @Test fun cycle() {
    val loader = loader("""
      abstract class $rootName
      class Foo : Bar
      class Bar : Foo
    """)
    val foo = loader.get("Foo")
    assertThat(foo.directSuperclasses.names()).containsExactly("Bar")
    assertThrows<IllegalArgumentException> { foo.allSuperclasses.names() }
    val bar = loader.get("Bar")
    assertThat(bar.directSuperclasses.names()).containsExactly("Foo")
    assertThrows<IllegalArgumentException> { bar.allSuperclasses.names() }
  }

  @Test fun trivialCycle() {
    val loader = loader("""
      abstract class $rootName
      class Foo : Foo
    """)
    val foo = loader.get("Foo")
    assertThat(foo.directSuperclasses.names()).containsExactly("Foo")
    assertThrows<IllegalArgumentException> { foo.allSuperclasses.names() }
  }

  @Test fun dependency() {
    val loader = loader("""
      abstract class $rootName
      class Foo
      class Bar<Foo>
    """)
    val bar = loader.get("Bar")
    assertThat(bar.directSuperclasses.names()).containsExactly(rootName)
    assertThat(bar.directDependencyKeys).containsExactly(DependencyKey(bar, 1))
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

    val key = DependencyKey(bar, 1)
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

    val key = DependencyKey(bar, 1)
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

    assertThat(bar.allDependencyKeys).containsExactly(DependencyKey(bar, 1))
    assertThat(qux.allDependencyKeys).containsExactly(DependencyKey(bar, 1),  DependencyKey(qux, 1))
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

    val key = DependencyKey(bar, 1)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  private fun loader(petsText: String) =
      PetClassLoader(parseComponents(petsText)).also { it.loadAll() }

  private fun Iterable<PetClass>.names() = map { it.name }

}
