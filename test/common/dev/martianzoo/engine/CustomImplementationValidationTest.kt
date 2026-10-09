package dev.martianzoo.engine

import dev.martianzoo.catalog.Catalog
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.CustomInstruction
import dev.martianzoo.state.CustomMetric
import dev.martianzoo.state.GameReader
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CustomImplementationValidationTest {
  @Test
  internal fun `T2-9 live startup accepts a matching instruction implementation`() {
    Engine.newGame(
        premise("CLASS Neighbor : CustomInstruction"),
        setOf(object : CustomInstruction("Neighbor") {}),
    )
  }

  @Test
  internal fun `T2-9 live startup rejects a missing implementation`() {
    val error =
        shouldThrow<InvalidPetDefinitionException> {
          Engine.newGame(premise("CLASS Neighbor : CustomInstruction"))
        }

    assertEquals("custom class implementation not found for `Neighbor`", error.detail)
    assertEquals(
        """
        |custom class implementation not found for `Neighbor` at 1:7
        |CLASS Neighbor : CustomInstruction
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun `T2-9 live startup rejects wrong-kind implementations`() {
    shouldThrow<InvalidPetDefinitionException> {
      Engine.newGame(
          premise("CLASS Neighbor : CustomInstruction"),
          setOf(metric("Neighbor")),
      )
    }
    shouldThrow<InvalidPetDefinitionException> {
      Engine.newGame(
          premise("CLASS Neighbor : CustomMetric"),
          setOf(object : CustomInstruction("Neighbor") {}),
      )
    }
  }

  @Test
  internal fun `T2-9 ordinary and root classes reject implementations`() {
    shouldThrow<InvalidPetDefinitionException> {
      Engine.newGame(
          premise("CLASS Neighbor"),
          setOf(object : CustomInstruction("Neighbor") {}),
      )
    }
    shouldThrow<InvalidPetDefinitionException> {
      Engine.newGame(premise(""), setOf(object : CustomInstruction(COMPONENT) {}))
    }
  }

  @Test
  internal fun `T2-9 implementation names are unique`() {
    val first = object : CustomInstruction("Neighbor") {}
    val second = object : CustomInstruction("Neighbor") {}

    shouldThrow<InvalidPetDefinitionException> {
      Engine.newGame(
          premise("CLASS Neighbor : CustomInstruction"),
          setOf(first, second),
      )
    }
  }

  @Test
  internal fun undeclaredImplementationsAreAvailableToConfiguredCatalogSubsets() {
    Engine.newGame(premise(""), setOf(object : CustomInstruction("Unused") {}))
  }

  private fun premise(source: String): GamePremise {
    val declarations = parseClasses(source).toSet()
    val catalog =
        object : Catalog() {
          override val explicitClassDeclarations: Set<ClassDeclaration> = declarations
        }
    return GamePremise(catalog, emptySet())
  }

  private fun metric(name: String): CustomMetric =
      object : CustomMetric(name) {
        override fun count(game: GameReader, type: Type): Int = 0
      }
}
