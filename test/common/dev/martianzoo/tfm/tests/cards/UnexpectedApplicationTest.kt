package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class UnexpectedApplicationTest : TfmSandboxTest() {
  // https://boardgamegeek.com/thread/3577088/article/46624092#46624092
  @Test
  internal fun `Follows the designers intended discard-first sequence`() {
    // The source also allows discarding later under the printed icons; no formal erratum
    // revoking that allowance was established. This tests the designer's intended sequence.
    newTestGame("UnexpectedApplication")
    kim.setToExMachina(1, "ProjectCard")
    kim.setToExMachina(3, "VenusStep")

    shouldThrow<LimitsException> { kim.playProject(UnexpectedApplication, 4) }

    // With a card available to discard, Venus can advance to the card-draw bonus.
    kim.setToExMachina(2, "ProjectCard")
    kim.playProject(UnexpectedApplication, 4).expect("-ProjectCard, VenusStep, TerraformRating")
  }
}
