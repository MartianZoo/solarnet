package dev.martianzoo.types

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ClassSynonyms
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.Bundle
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.util.toSetStrict
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ClassTest {
  @Test
  fun clientClassSynonymsAreOptIn() {
    val classes = parseClasses("CLASS Foo").toSetStrict()
    val ruleset =
        object : TfmRuleset.Empty() {
          override val explicitClassDeclarations = classes
        }

    ClassLoader(ruleset, ClassSynonyms.of("F" to "Foo"))
        .apply { load(cn("F")) }
        .freeze()
        .getClass(cn("F"))
        .className shouldBe cn("Foo")
    shouldThrow<PetException> { ClassLoader(ruleset).load(cn("F")) }
  }

  @Test
  fun `root classes reject unexpected custom implementations`() {
    val ruleset =
        object : TfmRuleset.Empty() {
          override val customClasses = setOf(object : CustomClass(COMPONENT) {})
        }

    shouldThrow<PetException> { ClassLoader(ruleset) }
  }

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
  fun classTableEnumerationRequiresFreezeWithoutCapturingAnEarlySnapshot() {
    val classes = parseClasses("CLASS Foo").toSetStrict()
    val ruleset =
        object : TfmRuleset.Empty() {
          override val explicitClassDeclarations = classes
        }
    val loader = ClassLoader(ruleset)

    loader.findClass(cn("Foo")) shouldBe null
    shouldThrow<IllegalArgumentException> { loader.allClasses() }
    val foo = loader.load(cn("Foo"))
    loader.findClass(cn("Foo")) shouldBe foo

    loader
        .freeze()
        .allClasses()
        .classNames()
        .shouldContainExactlyInAnyOrder(COMPONENT, cn("Class"), cn("Foo"))
  }

  @Test
  fun `custom class requirements load with the custom class only`() {
    val declarations =
        parseClasses(
                """
                CLASS DependencySource : Custom
                CLASS RuntimeDependency
                """
                    .trimIndent()
            )
            .toSetStrict()
    val implementation =
        object : CustomClass(cn("DependencySource")) {
          override val requiredClassNames = setOf(cn("RuntimeDependency"))
        }
    val ruleset =
        object : TfmRuleset.Empty() {
          override val explicitClassDeclarations = declarations
          override val customClasses = setOf(implementation)
        }

    val inactive = ClassLoader(ruleset).freeze().getClass(cn("RuntimeDependency"))
    inactive.phantom shouldBe true

    val loaded = ClassLoader(ruleset).apply { load(cn("DependencySource")) }.freeze()
    loaded.getClass(cn("RuntimeDependency")).phantom shouldBe false
  }

  @Test
  fun `authority-known inactive classes resolve as phantoms but are not enumerated`() {
    val activeBundle = bundle("ActiveBundle", "CLASS Active")
    val inactiveBundle = bundle("InactiveBundle", "CLASS Inactive")
    val ruleset =
        TfmRuleset.compose(activeBundle, inactiveBundle).resolve(setOf(cn("ActiveBundle")))
    val table = ClassLoader(ruleset).apply { load(cn("Active")) }.freeze()

    table.getClass(cn("Inactive")).phantom shouldBe true
    table.resolve(te("Inactive")).phantom shouldBe true
    table.resolve(te("Class<Inactive>")).phantom shouldBe true
    table.resolve(te("Inactive")).allConcreteSubtypes().toList() shouldBe emptyList()
    table.allClasses().classNames() shouldBe setOf(COMPONENT, cn("Class"), cn("Active"))
  }

  @Test
  fun `dependency signatures activate available vocabulary`() {
    val activeBundle =
        bundle(
            "ActiveBundle",
            """
            CLASS SelectedContent<AvailableVocabulary>
            CLASS AvailableVocabulary
            """,
        )
    val ruleset = TfmRuleset.compose(activeBundle).resolve(setOf(cn("ActiveBundle")))
    val table = ClassLoader(ruleset).apply { load(cn("SelectedContent")) }.freeze()

    table.getClass(cn("AvailableVocabulary")).phantom shouldBe false
  }

  @Test
  fun `excluding an inactive type does not make a complement dependency phantom`() {
    val activeBundle =
        bundle(
            "ActiveBundle",
            """
            ABSTRACT CLASS Domain
            ABSTRACT CLASS Holder<Domain>
            """,
        )
    val inactiveBundle = bundle("InactiveBundle", "CLASS Inactive : Domain")
    val ruleset =
        TfmRuleset.compose(activeBundle, inactiveBundle).resolve(setOf(cn("ActiveBundle")))
    val table = ClassLoader(ruleset).apply { load(cn("Holder")) }.freeze()

    table.resolve(te("Holder<!Inactive>")).phantom shouldBe false
  }

  @Test
  fun `active classes cannot structurally depend on inactive classes`() {
    val activeBundle = bundle("ActiveBundle", "CLASS Active<Inactive>")
    val inactiveBundle = bundle("InactiveBundle", "CLASS Inactive")
    val ruleset =
        TfmRuleset.compose(activeBundle, inactiveBundle).resolve(setOf(cn("ActiveBundle")))

    shouldThrow<PetException> { ClassLoader(ruleset).load(cn("Active")) }
  }

  @Test
  fun `class metrics do not activate the represented class`() {
    val activeBundle = bundle("ActiveBundle", "CLASS Querying { HAS MAX 0 Class<Inactive> }")
    val inactiveBundle = bundle("InactiveBundle", "CLASS Inactive")
    val ruleset =
        TfmRuleset.compose(activeBundle, inactiveBundle).resolve(setOf(cn("ActiveBundle")))
    val table = ClassLoader(ruleset).apply { load(cn("Querying")) }.freeze()

    table.getClass(cn("Inactive")).phantom shouldBe true
  }

  @Test
  fun `active classes cannot extend inactive classes`() {
    val activeBundle = bundle("ActiveBundle", "CLASS Active : Inactive")
    val inactiveBundle = bundle("InactiveBundle", "ABSTRACT CLASS Inactive")
    val ruleset =
        TfmRuleset.compose(activeBundle, inactiveBundle).resolve(setOf(cn("ActiveBundle")))

    shouldThrow<PetException> { ClassLoader(ruleset).load(cn("Active")) }
  }

  @Test
  fun subclass() {
    val loader = loadTypes("ABSTRACT CLASS Foo", "CLASS Bar : Foo")
    val bar = loader.getClass(cn("Bar"))
    bar.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Foo"))
    bar.allSuperclasses()
        .classNames()
        .shouldContainExactlyInAnyOrder(COMPONENT, cn("Foo"), cn("Bar"))
    bar.dependencies.keys.shouldBeEmpty()
  }

  @Test
  fun forwardReference() {
    val loader = loadTypes("CLASS Bar : Foo", "ABSTRACT CLASS Foo")
    val bar = loader.getClass(cn("Bar"))
    bar.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Foo"))
    bar.allSuperclasses()
        .classNames()
        .shouldContainExactlyInAnyOrder(COMPONENT, cn("Foo"), cn("Bar"))
    bar.dependencies.keys.shouldBeEmpty()
  }

  @Test
  fun concreteSuperclassRejected() {
    shouldThrow<PetException> { loadTypes("CLASS Foo", "CLASS Bar : Foo") }
    shouldThrow<PetException> {
      loadTypes("CLASS Foo", "ABSTRACT CLASS Bar : Foo")
    }
  }

  @Test
  fun cycle() {
    val s =
        """
      CLASS Foo : Bar
      CLASS Bar : Foo
    """
    shouldThrow<PetException> { loader(s) }
  }

  @Test
  fun trivialCycle() {
    val s =
        """
      CLASS Foo : Foo
    """
    shouldThrow<PetException> { loader(s) }
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
    val loader = loadTypes("CLASS Foo", "ABSTRACT CLASS Bar<Foo>", "CLASS Qux : Bar")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    qux.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Bar"))

    val key = Key(cn("Bar"), 0)
    bar.dependencies.keys.shouldContainExactlyInAnyOrder(key)
    qux.dependencies.keys.shouldContainExactlyInAnyOrder(key)
  }

  @Test
  fun restatedDependency() {
    val loader = loadTypes("CLASS Foo", "ABSTRACT CLASS Bar<Foo>", "CLASS Qux : Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    qux.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Bar"))

    val key = Key(cn("Bar"), 0)
    bar.dependencies.keys.shouldContainExactlyInAnyOrder(key)
    qux.dependencies.keys.shouldContainExactlyInAnyOrder(key)
  }

  @Test
  fun addedDependency() {
    val loader =
        loadTypes("CLASS Foo", "ABSTRACT CLASS Bar<Foo>", "CLASS Baz", "CLASS Qux<Baz> : Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))

    bar.dependencies.keys.shouldContainExactlyInAnyOrder(Key(cn("Bar"), 0))
    qux.dependencies.keys.shouldContainExactlyInAnyOrder(
        Key(cn("Bar"), 0),
        Key(cn("Qux"), 0),
    )
  }

  @Test
  fun refinedDependency() {
    val loader =
        loadTypes(
            "ABSTRACT CLASS Foo",
            "ABSTRACT CLASS Bar<Foo>",
            "CLASS Baz : Foo",
            "CLASS Qux : Bar<Baz>",
        )
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
            "ABSTRACT CLASS Bar : SuperBar<Foo>",
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

    barSub.abstract shouldBe true
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

    fun checkAutoAdjust(`in`: String, out: String, classTable: ClassTable) =
        classTable.resolve(te(`in`)).expressionFull.toString() shouldBe out

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
    val table = loadTypes("ABSTRACT CLASS Foo", "CLASS Bar : Foo")
    val cpt = table.componentClass
    val foo = table.getClass(cn("Foo"))
    val bar = table.getClass(cn("Bar"))
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
    val table = loadTypes("CLASS Foo", "ABSTRACT CLASS Bar", "CLASS Qux : Bar")
    val cpt = table.componentClass
    val foo = table.getClass(cn("Foo"))
    val bar = table.getClass(cn("Bar"))
    val qux = table.getClass(cn("Qux"))
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
    val loaderWithOptionalClass =
        loadTypes(
            *declarations,
            "CLASS AnyWordHere",
        )
    val known = loaderWithOptionalClass.resolve(te("Class<AnyWordHere>"))

    known.abstract shouldBe false
    known.allConcreteSubtypes().toList().single() shouldBe known
    shouldThrow<PetException> { loadTypes(*declarations) }
    loaderWithOptionalClass.resolve(te("Foo<Class<AnyWordHere>>")).abstract shouldBe false
  }
}

private fun bundle(name: String, declarations: String): Bundle =
    object : Bundle(cn(name)) {
      override val explicitClassDeclarations = parseClasses(declarations).toSetStrict()
    }

internal fun loader(petsText: String): ClassTable {
  val classes = parseClasses(petsText).toSetStrict()
  val ruleset =
      object : TfmRuleset.Empty() {
        override val explicitClassDeclarations = classes
      }
  return ClassLoader(ruleset).loadEverything()
}

val regex = Regex("^(\\w+).*")

internal fun loadAndGetClasses(vararg decl: String): List<Class> {
  val all =
      """
        ${decl.joinToString("") { "CLASS $it\n" }}
      """
  val loader = loader(all)
  val strings = listOf("Component") + decl.map { regex.matchEntire(it)!!.groupValues[1] }
  return strings.map { loader.getClass(cn(it)) }
}
