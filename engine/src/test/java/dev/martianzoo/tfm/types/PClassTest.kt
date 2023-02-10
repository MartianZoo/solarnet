package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.pets.Parsing.parseClassDeclarations
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.classNames
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class PClassTest {
  @Test
  fun nothingness() {
    val loader = loadTypes()
    val cpt = loader.componentClass
    assertThat(cpt.className).isEqualTo(COMPONENT)
    assertThat(cpt.abstract).isTrue()
    assertThat(cpt.directSuperclasses).isEmpty()
    assertThat(cpt.allSuperclasses.toStrings()).containsExactly("$COMPONENT")
    assertThat(cpt.directDependencyKeys).isEmpty()
  }

  @Test
  fun onethingness() {
    val loader = loadTypes("CLASS Foo")
    val foo = loader.getClass(cn("Foo"))
    assertThat(foo.className).isEqualTo(cn("Foo"))
    assertThat(foo.abstract).isFalse()
    assertThat(foo.directSuperclasses.toStrings()).containsExactly("$COMPONENT")
    assertThat(foo.allSuperclasses.toStrings()).containsExactly("$COMPONENT", "Foo")
    assertThat(foo.directDependencyKeys).isEmpty()
  }

  @Test
  fun subclass() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar : Foo")
    val bar = loader.getClass(cn("Bar"))
    assertThat(bar.directSuperclasses.toStrings()).containsExactly("Foo")
    assertThat(bar.allSuperclasses.toStrings()).containsExactly("$COMPONENT", "Foo", "Bar")
    assertThat(bar.directDependencyKeys).isEmpty()
  }

  @Test
  fun forwardReference() {
    val loader = loadTypes("CLASS Bar : Foo", "CLASS Foo")
    val bar = loader.getClass(cn("Bar"))
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
    val bar = loader.getClass(cn("Bar"))
    assertThat(bar.directSuperclasses.classNames()).containsExactly(COMPONENT)
    assertThat(bar.directDependencyKeys).containsExactly(Key(cn("Bar"), 0))
  }

  @Test
  fun inheritedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Qux : Bar")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    assertThat(qux.directSuperclasses.toStrings()).containsExactly("Bar")

    val key = Key(cn("Bar"), 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test
  fun restatedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Qux : Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    assertThat(qux.directSuperclasses.toStrings()).containsExactly("Bar")

    val key = Key(cn("Bar"), 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test
  fun addedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Baz", "CLASS Qux<Baz> : Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))

    assertThat(bar.allDependencyKeys).containsExactly(Key(cn("Bar"), 0))
    assertThat(qux.allDependencyKeys).containsExactly(Key(cn("Bar"), 0), Key(cn("Qux"), 0))
  }

  @Test
  fun refinedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Baz : Foo", "CLASS Qux : Bar<Baz>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    assertThat(qux.directSuperclasses.toStrings()).containsExactly("Bar")

    val key = Key(cn("Bar"), 0)
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
    val supSup = table.resolveType(typeExpr("SuperBar<SuperFoo>"))
    val supFoo = table.resolveType(typeExpr("SuperBar<Foo>"))
    val supSub = table.resolveType(typeExpr("SuperBar<SubFoo>"))
    val barFoo = table.resolveType(typeExpr("Bar<Foo>"))
    val barSub = table.resolveType(typeExpr("Bar<SubFoo>"))
    val subSub = table.resolveType(typeExpr("SubBar<SubFoo>"))

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

    assertFails("outta bounds") { table.resolveType(typeExpr("Foo<Qux>")) }
    assertFails("no deps") { table.resolveType(typeExpr("Foo<Bar>")) }
  }

  private fun checkAutoAdjust(`in`: String, out: String, table: PClassLoader) {
    assertThat(table.resolveType(typeExpr(`in`)).typeExprFull.toString()).isEqualTo(out)
  }
}

private fun loader(petsText: String): PClassLoader {
  val classes = parseClassDeclarations(petsText)
  val authority =
      object : Authority.Empty() {
        override val explicitClassDeclarations = classes
      }
  return PClassLoader(authority).also { it.loadEverything() }
}

// TODO share
private fun assertFails(message: String, shouldFail: () -> Unit) =
    assertThrows<RuntimeException>(message, shouldFail)

// TODO move to shared utils (already being used from PTypeTest)
internal fun loadTypes(vararg decl: String) =
    loader(
        """
        ABSTRACT CLASS $COMPONENT
        CLASS $CLASS<$COMPONENT>
        ${decl.joinToString("") { "$it\n" }}
        """.trimIndent())
