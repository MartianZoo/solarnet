package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.MClass
import dev.martianzoo.types.MClassLoader
import dev.martianzoo.types.MClassTable
import dev.martianzoo.util.toSetStrict
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class MClassTest {
  @Test
  fun nothingness() {
    val loader = loadTypes()
    val cpt = loader.componentClass
    assertThat(cpt.className).isEqualTo(COMPONENT)
    assertThat(cpt.abstract).isTrue()
    assertThat(cpt.directSuperclasses).isEmpty()
    assertThat(cpt.getAllSuperclasses().classNames()).containsExactly(COMPONENT)
    assertThat(cpt.dependencies.keys).isEmpty()
  }

  @Test
  fun onethingness() {
    val loader = loadTypes("CLASS Foo")
    val foo = loader.getClass(cn("Foo"))
    assertThat(foo.className).isEqualTo(cn("Foo"))
    assertThat(foo.abstract).isFalse()
    assertThat(foo.directSuperclasses.classNames()).containsExactly(COMPONENT)
    assertThat(foo.getAllSuperclasses().classNames()).containsExactly(COMPONENT, cn("Foo"))
    assertThat(foo.dependencies.keys).isEmpty()
  }

  @Test
  fun subclass() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar : Foo")
    val bar = loader.getClass(cn("Bar"))
    assertThat(bar.directSuperclasses.classNames()).containsExactly(cn("Foo"))
    assertThat(bar.getAllSuperclasses().classNames())
        .containsExactly(COMPONENT, cn("Foo"), cn("Bar"))
    assertThat(bar.dependencies.keys).isEmpty()
  }

  @Test
  fun forwardReference() {
    val loader = loadTypes("CLASS Bar : Foo", "CLASS Foo")
    val bar = loader.getClass(cn("Bar"))
    assertThat(bar.directSuperclasses.classNames()).containsExactly(cn("Foo"))
    assertThat(bar.getAllSuperclasses().classNames())
        .containsExactly(COMPONENT, cn("Foo"), cn("Bar"))
    assertThat(bar.dependencies.keys).isEmpty()
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
    assertThat(bar.dependencies.keys).containsExactly(Key(cn("Bar"), 0))
  }

  @Test
  fun inheritedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Qux : Bar")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    assertThat(qux.directSuperclasses.classNames()).containsExactly(cn("Bar"))

    val key = Key(cn("Bar"), 0)
    assertThat(bar.dependencies.keys).containsExactly(key)
    assertThat(qux.dependencies.keys).containsExactly(key)
  }

  @Test
  fun restatedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Qux : Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    assertThat(qux.directSuperclasses.classNames()).containsExactly(cn("Bar"))

    val key = Key(cn("Bar"), 0)
    assertThat(bar.dependencies.keys).containsExactly(key)
    assertThat(qux.dependencies.keys).containsExactly(key)
  }

  @Test
  fun addedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Baz", "CLASS Qux<Baz> : Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))

    assertThat(bar.dependencies.keys).containsExactly(Key(cn("Bar"), 0))
    assertThat(qux.dependencies.keys).containsExactly(Key(cn("Bar"), 0), Key(cn("Qux"), 0))
  }

  @Test
  fun refinedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Baz : Foo", "CLASS Qux : Bar<Baz>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    assertThat(qux.directSuperclasses.classNames()).containsExactly(cn("Bar"))

    val key = Key(cn("Bar"), 0)
    assertThat(bar.dependencies.keys).containsExactly(key)
    assertThat(qux.dependencies.keys).containsExactly(key)
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
    val supSup = table.resolve(te("SuperBar<SuperFoo>"))
    val supFoo = table.resolve(te("SuperBar<Foo>"))
    val supSub = table.resolve(te("SuperBar<SubFoo>"))
    val barFoo = table.resolve(te("Bar<Foo>"))
    val barSub = table.resolve(te("Bar<SubFoo>"))
    val subSub = table.resolve(te("SubBar<SubFoo>"))

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

    fun checkAutoAdjust(`in`: String, out: String, table: MClassTable) =
        assertThat(table.resolve(te(`in`)).expressionFull.toString()).isEqualTo(out)

    checkAutoAdjust("Bar<SuperFoo>", "Bar<Foo>", table)
    checkAutoAdjust("SubBar<SuperFoo>", "SubBar<SubFoo>", table)
    checkAutoAdjust("SubBar<Foo>", "SubBar<SubFoo>", table)

    assertFails("outta bounds") { table.resolve(te("Foo<Qux>")) }
    assertFails("no deps") { table.resolve(te("Foo<Bar>")) }
  }

  @Test
  fun testLubOne() {
    val (cpt, foo) = loadAndGetClasses("Foo")
    assertThat(cpt.lub(cpt)).isEqualTo(cpt)
    assertThat(cpt.lub(foo)).isEqualTo(cpt)
    assertThat(foo.lub(cpt)).isEqualTo(cpt)
    assertThat(foo.lub(foo)).isEqualTo(foo)
  }

  @Test
  fun testLubSibling() {
    val (cpt, foo, bar) = loadAndGetClasses("Foo", "Bar")
    assertThat(foo.lub(bar)).isEqualTo(cpt)
  }

  @Test
  fun testLubParent() {
    val (cpt, foo, bar) = loadAndGetClasses("Foo", "Bar : Foo")
    assertThat(cpt.lub(cpt)).isEqualTo(cpt)
    assertThat(cpt.lub(foo)).isEqualTo(cpt)
    assertThat(cpt.lub(bar)).isEqualTo(cpt)
    assertThat(foo.lub(cpt)).isEqualTo(cpt)
    assertThat(foo.lub(foo)).isEqualTo(foo)
    assertThat(foo.lub(bar)).isEqualTo(foo)
    assertThat(bar.lub(cpt)).isEqualTo(cpt)
    assertThat(bar.lub(foo)).isEqualTo(foo)
    assertThat(bar.lub(bar)).isEqualTo(bar)
  }

  @Test
  fun testLubNibling() {
    val (cpt, foo, bar, qux) = loadAndGetClasses("Foo", "Bar", "Qux : Bar")
    assertThat(qux.lub(qux)).isEqualTo(qux)

    assertThat(cpt.lub(qux)).isEqualTo(cpt)
    assertThat(foo.lub(qux)).isEqualTo(cpt)
    assertThat(bar.lub(qux)).isEqualTo(bar)

    assertThat(qux.lub(cpt)).isEqualTo(cpt)
    assertThat(qux.lub(foo)).isEqualTo(cpt)
    assertThat(qux.lub(bar)).isEqualTo(bar)
  }

  @Test
  fun classTypes() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar", "CLASS Qux")

    assertFails { loader.resolve(te("Class<Class<Class>>")) }
    assertFails { loader.resolve(te("Class<Class<Foo>>")) }
    assertFails { loader.resolve(te("Class<Foo<Bar>>")) }
    assertFails { loader.resolve(te("Class<Foo, Bar>")) }
    assertFails { loader.resolve(te("Qux<Class<Foo<Bar>>>")) }
    assertFails { loader.resolve(te("Qux<Class<Foo, Bar>>")) }
    assertFails { loader.resolve(te("Class<Class<Component>>")) }
  }
}

internal fun loader(petsText: String): MClassTable {
  val classes = parseClasses(petsText).toSetStrict()
  val authority =
      object : TfmAuthority.Empty() {
        override val explicitClassDeclarations = classes
      }
  return MClassLoader(authority).loadEverything()
}

val regex = Regex("^(\\w+).*")

internal fun loadAndGetClasses(vararg decl: String): List<MClass> {
  val all =
      """
        ABSTRACT CLASS $COMPONENT
        CLASS $CLASS<$COMPONENT>
        CLASS Ok
        ${decl.joinToString("") { "CLASS $it\n" }}
      """
  val loader = loader(all)
  val strings = listOf("Component") + decl.map { regex.matchEntire(it)!!.groupValues[1] }
  return strings.map { loader.getClass(cn(it)) }
}
