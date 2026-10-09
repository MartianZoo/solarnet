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
  internal fun `T2-9 a class cannot have both custom kinds`() {
    shouldThrow<InvalidPetDefinitionException> {
      testCatalog("CLASS Neighbor : CustomMetric, CustomInstruction").classTable
    }
  }

  @Test
  internal fun `T2-9 runtime binding accepts a matching instruction implementation`() {
    val catalog =
        testCatalog(
            "CLASS Neighbor : CustomInstruction",
            setOf(object : CustomInstruction("Neighbor") {}),
        )

    catalog.classTable.getClass(cn("Neighbor")).declaration.customMetric shouldBe false
    GameWorld(emptyPremise(catalog))
  }

  @Test
  internal fun `T2-9 runtime binding rejects missing and wrong-kind implementations`() {
    shouldThrow<InvalidPetDefinitionException> {
      GameWorld(emptyPremise(testCatalog("CLASS Neighbor : CustomInstruction")))
    }
    shouldThrow<InvalidPetDefinitionException> {
      GameWorld(
          emptyPremise(
              testCatalog(
                  "CLASS Neighbor : CustomInstruction",
                  setOf(metric("Neighbor")),
              )
          )
      )
    }
    shouldThrow<InvalidPetDefinitionException> {
      GameWorld(
          emptyPremise(
              testCatalog(
                  "CLASS Neighbor : CustomMetric",
                  setOf(object : CustomInstruction("Neighbor") {}),
              )
          )
      )
    }
  }

  @Test
  internal fun `T2-9 ordinary and root classes reject unexpected implementations`() {
    shouldThrow<InvalidPetDefinitionException> {
      GameWorld(
          emptyPremise(
              testCatalog(
                  "CLASS Neighbor",
                  setOf(object : CustomInstruction("Neighbor") {}),
              )
          )
      )
    }
    shouldThrow<InvalidPetDefinitionException> {
      GameWorld(emptyPremise(testCatalog("", setOf(object : CustomInstruction(COMPONENT) {}))))
    }
  }

  @Test
  internal fun `T2-9 a computed Signal has a unique implementation`() {
    val first = object : CustomInstruction("Neighbor") {}
    val second = object : CustomInstruction("Neighbor") {}

    shouldThrow<InvalidPetDefinitionException> {
      GameWorld(
          emptyPremise(
              testCatalog(
                  "CLASS Neighbor : CustomInstruction",
                  setOf(first, second),
              )
          )
      )
    }
  }

  @Test
  internal fun catalogLoadingAndConfigurationDoNotRequireCustomImplementations() {
    val source = "CLASS Garden : CustomMetric\nCLASS Replant : CustomInstruction"
    val catalog = testCatalog(source)

    catalog.classTable.getClass(cn("Garden")).declaration.customMetric shouldBe true
    catalog.classTable.getClass(cn("Replant")).declaration.customMetric shouldBe false
    val premise = catalog.gamePremise(GameConfig("Garden, Replant"))
    premise.classTable.getClass(cn("Garden")).declaration.customMetric shouldBe true
    premise.classTable.getClass(cn("Replant")).declaration.customMetric shouldBe false

    val error = assertFailsWith<InvalidPetDefinitionException> { GameWorld(premise) }
    assertEquals("custom class implementation not found for `Garden`", error.detail)
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

  private fun emptyPremise(catalog: Catalog): GamePremise =
      GamePremise(catalog = catalog, classSelections = emptySet())

  private fun metric(name: String): CustomMetric =
      object : CustomMetric(name) {
        override fun count(game: GameReader, type: Type): Int = 0
      }
}
