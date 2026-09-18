package dev.martianzoo.pets.api

import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ExceptionsTest {
  @Test
  internal fun choicesRequireAtLeastOneRequirementFailure() {
    shouldThrow<IllegalArgumentException> {
      Exceptions.requirementsNotMetInChoices(emptyList())
    }
  }
}
