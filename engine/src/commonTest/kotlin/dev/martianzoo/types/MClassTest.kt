package dev.martianzoo.types

import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.util.toSetStrict
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MClassTest {
  @Test
  fun nothingness() {
    val loader = loadTypes()
    val cpt = loader.componentClass
    cpt.abstract shouldBe true
    cpt.directSuperclasses.shouldBeEmpty()
    cpt.allSuperclasses().classNames().shouldContainExactlyInAnyOrder(COMPONENT)
    cpt.dependencies.keys.shouldBeEmpty()
  }

  @Test
  fun onethingness() {
    val loader = loadTypes("CLASS Foo")
    val foo = loader.getClass(cn("Foo"))
    foo.abstract shouldBe false
    foo.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(COMPONENT)
    foo.allSuperclasses().classNames().shouldContainExactlyInAnyOrder(COMPONENT, cn("Foo"))
    foo.dependencies.keys.shouldBeEmpty()
  }

  @Test
  fun subclass() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar : Foo")
    val bar = loader.getClass(cn("Bar"))
    bar.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Foo"))
    bar.allSuperclasses()
        .classNames()
        .shouldContainExactlyInAnyOrder(COMPONENT, cn("Foo"), cn("Bar"))
    bar.dependencies.keys.shouldBeEmpty()
  }

  @Test
  fun forwardReference() {
    val loader = loadTypes("CLASS Bar : Foo", "CLASS Foo")
    val bar = loader.getClass(cn("Bar"))
    bar.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Foo"))
    bar.allSuperclasses()
        .classNames()
        .shouldContainExactlyInAnyOrder(COMPONENT, cn("Foo"), cn("Bar"))
    bar.dependencies.keys.shouldBeEmpty()
  }

  @Test
  fun cycle() {
    val s =
        """
      CLASS Foo : Bar
      CLASS Bar : Foo
    """
    shouldThrow<IllegalStateException> { loader(s) }
  }

  @Test
  fun trivialCycle() {
    val s =
        """
      CLASS Foo : Foo
    """
    shouldThrow<IllegalStateException> { loader(s) }
  }

  @Test
  fun dependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    bar.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(COMPONENT)
    bar.dependencies.keys.shouldContainExactlyInAnyOrder(Key(cn("Bar"), 0))
  }

  @Test
  fun inheritedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Qux : Bar")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    qux.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Bar"))

    val key = Key(cn("Bar"), 0)
    bar.dependencies.keys.shouldContainExactlyInAnyOrder(key)
    qux.dependencies.keys.shouldContainExactlyInAnyOrder(key)
  }

  @Test
  fun restatedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Qux : Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    qux.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Bar"))

    val key = Key(cn("Bar"), 0)
    bar.dependencies.keys.shouldContainExactlyInAnyOrder(key)
    qux.dependencies.keys.shouldContainExactlyInAnyOrder(key)
  }

  @Test
  fun addedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Baz", "CLASS Qux<Baz> : Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))

    bar.dependencies.keys.shouldContainExactlyInAnyOrder(Key(cn("Bar"), 0))
    qux.dependencies.keys.shouldContainExactlyInAnyOrder(Key(cn("Bar"), 0), Key(cn("Qux"), 0))
  }

  @Test
  fun refinedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Baz : Foo", "CLASS Qux : Bar<Baz>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    qux.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Bar"))

    val key = Key(cn("Bar"), 0)
    bar.dependencies.keys.shouldContainExactlyInAnyOrder(key)
    qux.dependencies.keys.shouldContainExactlyInAnyOrder(key)
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
            "CLASS Qux",
        )

    // abstract: SuperFoo, SuperBar, Foo
    val supSup = table.resolve(te("SuperBar<SuperFoo>"))
    val supFoo = table.resolve(te("SuperBar<Foo>"))
    val supSub = table.resolve(te("SuperBar<SubFoo>"))
    val barFoo = table.resolve(te("Bar<Foo>"))
    val barSub = table.resolve(te("Bar<SubFoo>"))
    val subSub = table.resolve(te("SubBar<SubFoo>"))

    supSup.abstract shouldBe true
    supSup.isSubtypeOf(supSup) shouldBe true

    supFoo.abstract shouldBe true
    supFoo.isSubtypeOf(supSup) shouldBe true
    supFoo.isSubtypeOf(supFoo) shouldBe true

    supSub.abstract shouldBe true
    supSub.isSubtypeOf(supSup) shouldBe true
    supSub.isSubtypeOf(supFoo) shouldBe true
    supSub.isSubtypeOf(supSub) shouldBe true

    barFoo.abstract shouldBe true
    barFoo.isSubtypeOf(supSup) shouldBe true
    barFoo.isSubtypeOf(supFoo) shouldBe true
    barFoo.isSubtypeOf(barFoo) shouldBe true

    barSub.abstract shouldBe false
    barSub.isSubtypeOf(supSup) shouldBe true
    barSub.isSubtypeOf(supFoo) shouldBe true
    barSub.isSubtypeOf(supSub) shouldBe true
    barSub.isSubtypeOf(barFoo) shouldBe true
    barSub.isSubtypeOf(barSub) shouldBe true

    subSub.abstract shouldBe false
    subSub.isSubtypeOf(supSup) shouldBe true
    subSub.isSubtypeOf(supFoo) shouldBe true
    subSub.isSubtypeOf(supSub) shouldBe true
    subSub.isSubtypeOf(barFoo) shouldBe true
    subSub.isSubtypeOf(barSub) shouldBe true
    subSub.isSubtypeOf(subSub) shouldBe true

    fun checkAutoAdjust(`in`: String, out: String, table: MClassTable) =
        table.resolve(te(`in`)).expressionFull.toString() shouldBe out

    checkAutoAdjust("Bar<SuperFoo>", "Bar<Foo>", table)
    checkAutoAdjust("SubBar<SuperFoo>", "SubBar<SubFoo>", table)
    checkAutoAdjust("SubBar<Foo>", "SubBar<SubFoo>", table)

    assertFails("outta bounds") { table.resolve(te("Foo<Qux>")) }
    assertFails("no deps") { table.resolve(te("Foo<Bar>")) }
  }

  @Test
  fun testLubOne() {
    val (cpt, foo) = loadAndGetClasses("Foo")
    cpt.lub(cpt) shouldBe cpt
    cpt.lub(foo) shouldBe cpt
    foo.lub(cpt) shouldBe cpt
    foo.lub(foo) shouldBe foo
  }

  @Test
  fun testLubSibling() {
    val (cpt, foo, bar) = loadAndGetClasses("Foo", "Bar")
    foo.lub(bar) shouldBe cpt
  }

  @Test
  fun testLubParent() {
    val (cpt, foo, bar) = loadAndGetClasses("Foo", "Bar : Foo")
    cpt.lub(cpt) shouldBe cpt
    cpt.lub(foo) shouldBe cpt
    cpt.lub(bar) shouldBe cpt
    foo.lub(cpt) shouldBe cpt
    foo.lub(foo) shouldBe foo
    foo.lub(bar) shouldBe foo
    bar.lub(cpt) shouldBe cpt
    bar.lub(foo) shouldBe foo
    bar.lub(bar) shouldBe bar
  }

  @Test
  fun testLubNibling() {
    val (cpt, foo, bar, qux) = loadAndGetClasses("Foo", "Bar", "Qux : Bar")
    qux.lub(qux) shouldBe qux

    cpt.lub(qux) shouldBe cpt
    foo.lub(qux) shouldBe cpt
    bar.lub(qux) shouldBe bar

    qux.lub(cpt) shouldBe cpt
    qux.lub(foo) shouldBe cpt
    qux.lub(bar) shouldBe bar
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

  @Test
  fun unknownClassTypes() {
    val declarations =
        arrayOf(
            "CLASS Foo<Class<Component>>",
            "CLASS Querying { HAS MAX 0 Class<AnyWordHere> }",
        )
    val loader = loadTypes(*declarations)
    val loaderWithOptionalClass =
        loadTypes(
            *declarations,
            "CLASS AnyWordHere",
        )
    val known = loaderWithOptionalClass.resolve(te("Class<AnyWordHere>"))

    known.abstract shouldBe false
    known.allConcreteSubtypes().toList().single() shouldBe known
    assertFails { loader.resolve(te("Class<AnyWordHere>")) }
    assertFails { loader.resolve(te("Foo<Class<AnyWordHere>>")) }
    loaderWithOptionalClass.resolve(te("Foo<Class<AnyWordHere>>")).abstract shouldBe false
    assertFails { loader.resolve(te("AnyWordHere")) }
  }
}

internal fun loader(petsText: String): MClassTable {
  val classes = parseClasses(petsText).toSetStrict()
  val ruleset =
      object : TfmRuleset.Empty() {
        override val explicitClassDeclarations = classes
      }
  return MClassLoader(ruleset).loadEverything()
}

val regex = Regex("^(\\w+).*")

internal fun loadAndGetClasses(vararg decl: String): List<MClass> {
  val all =
      """
        ${decl.joinToString("") { "CLASS $it\n" }}
      """
  val loader = loader(all)
  val strings = listOf("Component") + decl.map { regex.matchEntire(it)!!.groupValues[1] }
  return strings.map { loader.getClass(cn(it)) }
}
