package dev.martianzoo.state

import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Type
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class CustomImplementationValidationTest {
  @Test
  internal fun `T2-9 a CustomInstruction must have a Kotlin implementation, and only a CustomInstruction may`() {
    val declaration = "CLASS Neighbor : CustomInstruction"

    createClassLoader(
            testCatalog(declaration, setOf(object : CustomInstruction(cn("Neighbor")) {}))
        )
        .loadEverything()
        .getClass(cn("Neighbor"))
        .declaration
        .customMetric shouldBe false

    // A declared-but-unimplemented CustomInstruction is rejected by the Catalog lookup itself.
    shouldThrow<InvalidPetDefinitionException> { testCatalog(declaration).classTable }
    shouldThrow<InvalidPetDefinitionException> {
      createClassLoader(
              testCatalog("CLASS Neighbor", setOf(object : CustomInstruction(cn("Neighbor")) {}))
          )
          .loadEverything()
    }
    val wrongKind =
        object : CustomMetric("Neighbor") {
          override fun count(game: GameReader, type: Type): Int = 0
        }
    shouldThrow<InvalidPetDefinitionException> {
      createClassLoader(testCatalog(declaration, setOf(wrongKind))).loadEverything()
    }
    shouldThrow<InvalidPetDefinitionException> {
      createClassLoader(
              testCatalog(
                  "CLASS Neighbor : CustomMetric",
                  setOf(object : CustomInstruction("Neighbor") {}),
              )
          )
          .loadEverything()
    }
  }

  @Test
  internal fun `T2-9 a root class rejects an unexpected implementation`() {
    shouldThrow<InvalidPetDefinitionException> {
      createClassLoader(testCatalog("", setOf(object : CustomInstruction(COMPONENT) {})))
    }
  }

  @Test
  internal fun `T2-9 a computed Signal has one implementation`() {
    val first = object : CustomInstruction("Neighbor") {}
    val second = object : CustomInstruction("Neighbor") {}
    shouldThrow<InvalidPetDefinitionException> {
      createClassLoader(testCatalog("CLASS Neighbor : CustomInstruction", setOf(first, second)))
          .loadEverything()
    }
  }

  @Test
  internal fun missingCustomImplementation() {
    val source = "CLASS Garden : CustomMetric"
    val error = assertFailsWith<InvalidPetDefinitionException> { testCatalog(source).classTable }

    assertEquals("custom class implementation not found for `Garden`", error.detail)
    // Prefer also highlighting `CustomMetric`, which makes this declaration require a Kotlin
    // implementation.
    assertEquals(
        """
        |custom class implementation not found for `Garden` at 1:7
        |CLASS Garden : CustomMetric
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }
}
