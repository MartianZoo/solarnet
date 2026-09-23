package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Conveniences of the `dev.martianzoo.pets.types` API that go beyond what
 * `docs/type-system-spec.md` requires. The `Spec*Test` classes check the specification itself.
 */
internal class TypesApiTest {

  private val mars =
      loadTypes(
          """
          ABSTRACT CLASS Area {
            ABSTRACT CLASS LandArea {
              CLASS Tharsis_2_2
              CLASS Tharsis_2_3
            }
            ABSTRACT CLASS WaterArea { CLASS Tharsis_1_2 }
          }
          ABSTRACT CLASS Occupant<Area>
          ABSTRACT CLASS Tile : Occupant {
            CLASS GreeneryTile : Tile<LandArea>
            CLASS OceanTile : Tile<WaterArea>
          }
          ABSTRACT CLASS StandardResource : Owned<Owner> {
            CLASS Plant
            CLASS Steel
          }
          """
              .trimIndent()
      )

  private fun klass(name: String) = mars.getClass(cn(name))

  private fun type(s: String) = mars.resolve(te(s))

  // Classes

  @Test
  internal fun `a class retains the declaration and docstring it was compiled from`() {
    val table = loadTypes("\"A greenery tile\"\nCLASS GreeneryTile")
    val greenery = table.getClass(cn("GreeneryTile"))

    greenery.declaration.className shouldBe cn("GreeneryTile")
    greenery.docstring shouldBe "A greenery tile"
  }

  @Test
  internal fun `a class renders as its name and is found by it`() {
    val table = loadTypes("CLASS GreeneryTile")

    table.getClass(cn("GreeneryTile")) shouldBe table.getClass(cn("GreeneryTile"))
    "${table.getClass(cn("GreeneryTile"))}" shouldBe "GreeneryTile"
    table.componentClass.className shouldBe COMPONENT
  }

  @Test
  internal fun `isSupertypeOf is the converse of isSubtypeOf`() {
    klass("LandArea").isSupertypeOf(klass("Tharsis_2_2")) shouldBe true
    klass("Tharsis_2_2").isSupertypeOf(klass("LandArea")) shouldBe false
  }

  @Test
  internal fun `ensureNarrows reports a failed subclass check`() {
    shouldThrow<Exception> { klass("LandArea").ensureNarrows(klass("WaterArea"), fullWorld) }
    klass("Tharsis_2_2").ensureNarrows(klass("LandArea"), fullWorld)
  }

  // Types

  @Test
  internal fun `knows reports whether a type belongs to this universe`() {
    fun universe() = loadTypes("ABSTRACT CLASS Area", "CLASS GreeneryTile<Area>")

    val left = universe()
    val right = universe()

    left.knows(left.resolve(te("GreeneryTile"))) shouldBe true
    left.knows(right.resolve(te("GreeneryTile"))) shouldBe false
  }

  @Test
  internal fun `a ground type is its own ground view and has no type variable`() {
    val greenery = type("GreeneryTile")

    greenery.groundType shouldBe greenery
    (greenery.groundType === greenery) shouldBe true
    greenery.typeVariable shouldBe null
  }

  @Test
  internal fun `narrows answers, ensureNarrows explains`() {
    type("Tharsis_2_2").narrows(type("LandArea"), NoGameState) shouldBe true
    type("Tharsis_2_2").ensureNarrows(type("LandArea"), NoGameState)

    type("LandArea").narrows(type("Tharsis_2_2"), NoGameState) shouldBe false
    shouldThrow<NarrowingException> {
      type("LandArea").ensureNarrows(type("Tharsis_2_2"), NoGameState)
    }
  }

  @Test
  internal fun `the context-free sentinel rejects every state query`() {
    shouldThrow<IllegalStateException> { NoGameState.isAbstract(te("LandArea")) }
    shouldThrow<IllegalStateException> {
      NoGameState.ensureNarrows(te("LandArea"), te("Tharsis_2_2"))
    }
    shouldThrow<IllegalStateException> { NoGameState.has(parse<Requirement>("LandArea")) }
  }

  // Enumeration

  @Test
  internal fun `same-class enumeration keeps the root class fixed`() {
    mars.concreteSubtypesSameClass(type("GreeneryTile")).map { "$it" }.toList() shouldContainExactly
        listOf("GreeneryTile<Tharsis_2_2>", "GreeneryTile<Tharsis_2_3>")
  }

  @Test
  internal fun `same-class enumeration of an abstract class yields nothing`() {
    mars.concreteSubtypesSameClass(type("Tile")).toList() shouldBe listOf()
  }

  @Test
  internal fun `a caller may supply the dependency targets to consider`() {
    val onlyOneArea: (Type) -> Sequence<Type> = { bound ->
      sequenceOf(type("Tharsis_2_3")).filter { it.isSubtypeOf(bound) }
    }

    mars.allConcreteSubtypes(type("Tile"), onlyOneArea).map { "$it" }.toList() shouldContainExactly
        listOf("GreeneryTile<Tharsis_2_3>")
  }

  // Type-variable scopes

  @Test
  internal fun `a scope reports the variables and spellings visible in it`() {
    val trade =
        mars
            .recordTypeVariableScopes()
            .transformEffect(parse("R@StandardResource: R@StandardResource"))
    val scope = trade.typeVariables
    val variable = scope.variables.single()

    scope.isEmpty shouldBe false
    TypeVariableScope.EMPTY.isEmpty shouldBe true
    scope.expressionsOf(variable).map { "$it" }.toSet() shouldBe
        setOf("R@StandardResource", "R@StandardResource")
    "${scope.expressionOf(variable.declaration)}" shouldBe "R@StandardResource"
    scope.variableAt(scope.expressionOf(variable.usages.single())) shouldBe variable
    scope.variableDeclaredAt(trade.trigger.descendantsOfType<Expression>().first()) shouldBe
        variable
    scope.variableAt(parse<Expression>("Plant")) shouldBe null
  }
}
