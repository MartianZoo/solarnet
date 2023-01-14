package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.data.ActionDefinition
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.MapAreaDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.pets.Parsing.parseClassDeclarations
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.util.Grid
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
    assertThat(cpt.allSuperclasses.names()).containsExactly(COMPONENT.asString)
    assertThat(cpt.directDependencyKeys).isEmpty()
  }

  @Test
  fun onethingness() {
    val loader = loadTypes("CLASS Foo")
    val foo = loader["Foo"]
    assertThat(foo.name).isEqualTo(ClassName("Foo"))
    assertThat(foo.abstract).isFalse()
    assertThat(foo.directSuperclasses.names()).containsExactly(COMPONENT.asString)
    assertThat(foo.allSuperclasses.names()).containsExactly(COMPONENT.asString, "Foo")
    assertThat(foo.directDependencyKeys).isEmpty()
  }

  @Test
  fun subclass() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar : Foo")
    val bar = loader["Bar"]
    assertThat(bar.directSuperclasses.names()).containsExactly("Foo")
    assertThat(bar.allSuperclasses.names()).containsExactly(COMPONENT.asString, "Foo", "Bar")
    assertThat(bar.directDependencyKeys).isEmpty()
  }

  @Test
  fun forwardReference() {
    val loader = loadTypes("CLASS Bar : Foo", "CLASS Foo")
    val bar = loader["Bar"]
    assertThat(bar.directSuperclasses.names()).containsExactly("Foo")
    assertThat(bar.allSuperclasses.names()).containsExactly(COMPONENT.asString, "Foo", "Bar")
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
    assertThat(bar.directSuperclasses.names()).containsExactly(COMPONENT.asString)
    assertThat(bar.directDependencyKeys).containsExactly(Key(bar, 0))
  }

  @Test
  fun inheritedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Qux : Bar")
    val bar = loader["Bar"]
    val qux = loader["Qux"]
    assertThat(qux.directSuperclasses.names()).containsExactly("Bar")

    val key = Key(bar, 0)
    assertThat(bar.allDependencyKeys).containsExactly(key)
    assertThat(qux.allDependencyKeys).containsExactly(key)
  }

  @Test
  fun restatedDependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>", "CLASS Qux : Bar<Foo>")
    val bar = loader["Bar"]
    val qux = loader["Qux"]
    assertThat(qux.directSuperclasses.names()).containsExactly("Bar")

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
    assertThat(qux.directSuperclasses.names()).containsExactly("Bar")

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
    val table = loadTypes(
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

    noWork("Bar<SuperFoo>", table)
    noWork("SubBar<SuperFoo>", table)
    noWork("SubBar<Foo>", table)
    noWork("Foo<Bar>", table)
  }

  private fun noWork(s: String, table: PetClassTable) {
    // TODO fix this
    // Assertions.assertThrows(RuntimeException::class.java, { table.resolve(s) }, s)
  }

  private fun Iterable<PetClass>.names() = map { it.name.asString }
}

private fun loader(petsText: String): PetClassLoader {
  val classes: List<ClassDeclaration> = parseClassDeclarations(petsText)
  val authority = FakeAuthority(classes)
  return PetClassLoader(authority).also { it.loadEverything() }
}

class FakeAuthority(classes: List<ClassDeclaration>) : Authority() {
  override val explicitClassDeclarations = classes
  override val mapAreaDefinitions = mapOf<String, Grid<MapAreaDefinition>>()
  override val actionDefinitions = listOf<ActionDefinition>()
  override val cardDefinitions = listOf<CardDefinition>()
  override val milestoneDefinitions = listOf<MilestoneDefinition>()
  override fun customInstructions() = listOf<CustomInstruction>()
}

// TODO move to shared utils
internal fun loadTypes(vararg decl: String): PetClassTable {
  return loader("ABSTRACT CLASS $COMPONENT\n" + decl.joinToString("") { "$it\n" })
}
