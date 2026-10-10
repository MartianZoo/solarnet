package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class SpliceTacticalGenomicsTest : TfmSandboxTest() {
  @Test
  internal fun `Can choose money for its own microbe tag`() {
    newTestGame(kimCorporation = SpliceTacticalGenomics, startAtCorporation = true)

    kim.playCorp(SpliceTacticalGenomics) { doTask("2 MC") }.expect("18 MC")
  }

  @Test
  internal fun `Another player's microbe tag can pay both players`() {
    newTestGame(kimCorporation = SpliceTacticalGenomics, startAtCorporation = true)
    kim.playCorp(SpliceTacticalGenomics) { doTask("2 MC") }
    startActionPhase()
    stan.exMachina("3 OxygenStep")
    stan.setToExMachina(5, "MC")
    stan.setToExMachina(1, "ProjectCard")

    stan
        .playProject(Decomposers, 5) { doTask("2 MC") }
        .expect("2 MC<Kim>, -3 MC<Stan>, Microbe<$Decomposers<Stan>>")
  }

  @Test
  internal fun `The other player can take a microbe instead of money`() {
    newTestGame(kimCorporation = SpliceTacticalGenomics, startAtCorporation = true)
    kim.playCorp(SpliceTacticalGenomics) { doTask("2 MC") }
    startActionPhase()
    stan.exMachina("3 OxygenStep")
    stan.setToExMachina(5, "MC")
    stan.setToExMachina(1, "ProjectCard")

    stan
        .playProject(Decomposers, 5) { addCardResources(Decomposers) }
        .expect("2 MC<Kim>, -5 MC<Stan>, 2 Microbe<$Decomposers<Stan>>")
  }

  @Test
  internal fun `The microbe must go on the card just played`() {
    newTestGame(kimCorporation = SpliceTacticalGenomics, startAtCorporation = true)
    kim.playCorp(SpliceTacticalGenomics) { doTask("2 MC") }
    startActionPhase()
    stan.exMachina("3 OxygenStep, $RegolithEaters")
    stan.setToExMachina(5, "MC")
    stan.setToExMachina(1, "ProjectCard")

    stan
        .playProject(Decomposers, 5) {
          shouldThrow<NarrowingException> { doTask("Microbe<$RegolithEaters>!") }
          addCardResources(Decomposers)
        }
        .expect("2 MC<Kim>, 2 Microbe<$Decomposers<Stan>>, 0 Microbe<$RegolithEaters<Stan>>")
  }

  @Test
  internal fun `Can forgo money even when the played card cannot hold microbes`() {
    newTestGame(kimCorporation = SpliceTacticalGenomics, startAtCorporation = true)
    kim.playCorp(SpliceTacticalGenomics) { doTask("2 MC") }
    startActionPhase()
    stan.setToExMachina(12, "MC")
    stan.setToExMachina(1, "ProjectCard")

    stan
        .playProject(IndustrialMicrobes, 12) { declineTask() }
        .expect("2 MC<Kim>, -12 MC<Stan>, PROD[Energy<Stan>, Steel<Stan>], 0 Microbe<Stan>")
  }
}
