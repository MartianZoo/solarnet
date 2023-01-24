package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.pets.Parsing.parseClassDeclarations
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class PetClassTest {
  @Test
  fun nothingness() {
    val loader = loadTypes()
    val cpt = loader[COMPONENT]
    assertThat(cpt.name).isEqualTo(COMPONENT)
    assertThat(cpt.abstract).isTrue()
    assertThat(cpt.directSuperclasses).isEmpty()
    assertThat(cpt.allSuperclasses.toStrings()).containsExactly("$COMPONENT")
    assertThat(cpt.directDependencyKeys).isEmpty()
  }

  @Test
  fun onethingness() {
    val loader = loadTypes("CLASS Foo")
    val foo = loader["Foo"]
    assertThat(foo.name).isEqualTo(cn("Foo"))
    assertThat(foo.abstract).isFalse()
    assertThat(foo.directSuperclasses.toStrings()).containsExactly("$COMPONENT")
    assertThat(foo.allSuperclasses.toStrings()).containsExactly("$COMPONENT", "Foo")
    assertThat(foo.directDependencyKeys).isEmpty()
  }

  @Test
  fun subclass() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar : Foo")
    val bar = loader["Bar"]
    assertThat(bar.directSuperclasses.toStrings()).containsExactly("Foo")
    assertThat(bar.allSuperclasses.toStrings()).containsExactly("$COMPONENT", "Foo", "Bar")
    assertThat(bar.directDependencyKeys).isEmpty()
  }

  @Test
  fun forwardReference() {
    val loader = loadTypes("CLASS Bar : Foo", "CLASS Foo")
    val bar = loader["Bar"]
    assertThat(bar.directSuperclasses.toStrings()).containsExactly("Foo")
    assertThat(bar.allSuperclasses.toStrings()).containsExactly("$COMPONENT", "Foo", "Bar")
    assertThat(bar.directDependencyKeys).isEmpty()
  }

  @Test
  fun cycle() {
    val s = """
      ABSTRACT CLASS $COMPONENT
      CLASS Foo : Bar
      CLASS Bar : Foo
    """
    assertThrows<IllegalArgumentException> { loader(s) }
  }

  @Test
  fun trivialCycle() {
    val s = """
      ABSTRACT CLASS $COMPONENT
      CLASS Foo : Foo
    """
    assertThrows<IllegalArgumentException> { loader(s) }
  }

  @Test
  fun dependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>")
    val bar = loader["Bar"]
    assertThat(bar.directSuperclasses.map { it.name }).containsExactly(COMPONENT)
    assertThat(bar.directDependencyKeys).containsExactly(Key(bar, 0))
  }

  @Test
  fun inheritedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Qux : Bar")
    val bar = loader["Bar"]
    val qux = loader["Qux"]
    assertThat(qux.directSuperclasses.toStrings()).containsExactly("Bar")

    val key = Key(bar, 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test
  fun restatedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Qux : Bar<Foo>")
    val bar = loader["Bar"]
    val qux = loader["Qux"]
    assertThat(qux.directSuperclasses.toStrings()).containsExactly("Bar")

    val key = Key(bar, 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test
  fun addedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Baz", "CLASS Qux<Baz> : Bar<Foo>")
    val bar = loader["Bar"]
    val qux = loader["Qux"]

    assertThat(bar.allDependencyKeys).containsExactly(Key(bar, 0))
    assertThat(qux.allDependencyKeys).containsExactly(Key(bar, 0), Key(qux, 0))
  }

  @Test
  fun refinedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Baz : Foo", "CLASS Qux : Bar<Baz>")
    val bar = loader["Bar"]
    val qux = loader["Qux"]
    assertThat(qux.directSuperclasses.toStrings()).containsExactly("Bar")

    val key = Key(bar, 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test
  fun cycleDependency() {
    loadTypes("CLASS Foo<Bar>", "CLASS Bar<Foo>")
  }

  @Test
  fun depsAndSpecs() {
    val table =
        loadTypes(
            "ABSTRACT CLASS SuperFoo",
            "ABSTRACT CLASS Foo : SuperFoo",
            "CLASS SubFoo : Foo",
            "ABSTRACT CLASS SuperBar<SuperFoo>",
            "CLASS Bar : SuperBar<Foo>",
            "CLASS SubBar : Bar<SubFoo>",
            "CLASS Qux")

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

    checkAutoAdjust("Bar<SuperFoo>", "Bar<Foo>", table)
    checkAutoAdjust("SubBar<SuperFoo>", "SubBar<SubFoo>", table)
    checkAutoAdjust("SubBar<Foo>", "SubBar<SubFoo>", table)

    assertThrows<RuntimeException> { table.resolve("Foo<Qux>") }
    assertThrows<RuntimeException> { table.resolve("Foo<Bar>") }
  }

  private fun checkAutoAdjust(`in`: String, out: String, table: PetClassTable) {
    assertThat(table.resolve(`in`).toTypeExpression().toString()).isEqualTo(out)
  }
}

private fun loader(petsText: String): PetClassLoader {
  val classes = parseClassDeclarations(petsText)
  val authority =
      object : Authority.Empty() {
        override val explicitClassDeclarations = classes
      }
  return PetClassLoader(authority).also { it.loadEverything() }
}

// TODO move to shared utils (already being used from PetTypeTest)
internal fun loadTypes(vararg decl: String): PetClassTable {
  return loader("ABSTRACT CLASS $COMPONENT\n" + decl.joinToString("") { "$it\n" })
}
