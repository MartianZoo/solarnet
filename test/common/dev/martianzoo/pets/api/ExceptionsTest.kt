package dev.martianzoo.pets.api

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.types.loadTypes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

internal class ExceptionsTest {
  @Test
  internal fun publicPetsInputsUseTheFailureTypeForTheInvalidArtifact() {
    val syntax = shouldThrow<PetSyntaxException> { parse<Expression>("Plant ???") }

    val table = loadTypes("CLASS Plant")
    val expression = shouldThrow<ExpressionException> { table.resolve(parse("Missing")) }

    val definition =
        shouldThrow<InvalidPetDefinitionException> { loadTypes("CLASS Broken : Missing") }

    listOf(syntax, expression, definition).forEach { it.shouldBeInstanceOf<PetException>() }

    val configuration: Exception =
        shouldThrow<InvalidGameConfigException> { GameConfig("Plant, Plant") }
    (configuration is PetException) shouldBe false
  }

  @Test
  internal fun choicesRequireAtLeastOneRequirementFailure() {
    shouldThrow<IllegalArgumentException> {
      Exceptions.requirementsNotMetInChoices(emptyList())
    }
  }
}
