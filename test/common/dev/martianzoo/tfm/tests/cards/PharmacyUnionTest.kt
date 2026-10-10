package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PharmacyUnionTest : TfmSandboxTest() {
  @BeforeTest
  fun initializeGame() {
    newTestGame(kimCorporation = PharmacyUnion, startAtCorporation = true)
    kim.playCorp(PharmacyUnion) { repeat(2) { doTask("Disease<$PharmacyUnion>") } }
    startActionPhase()
  }

  // Resolved FAQ: starting money must precede the losses from the two microbe tags.
  @Test
  internal fun `Starting cash covers both disease losses as well as the starting projects`() {
    newTestGame(kimCorporation = PharmacyUnion, startAtCorporation = true)

    kim.playCorp(PharmacyUnion) { repeat(2) { doTask("Disease<$PharmacyUnion>") } }
        .expect("16 MC, 11 ProjectCard, 2 Disease<$PharmacyUnion>, 0 RequiredAction")
  }

  @Test
  internal fun `A science tag removes disease and raises TR`() {
    kim.playProject(PhysicsComplex, 12).expect("-Disease<$PharmacyUnion>, TerraformRating")
  }

  @Test
  internal fun `An opponent's microbe tag adds disease and costs the corporation money`() {
    stan.exMachina("3 OxygenStep, ProjectCard, 5 MC")

    stan.playProject(Decomposers, 5).expect("Disease<$PharmacyUnion<Kim>>, -4 MC<Kim>")
  }

  @Test
  internal fun `Two science tags can remove the last disease and then flip the corporation`() {
    kim.setToExMachina(1, "Disease<$PharmacyUnion>")
    kim.exMachina("$Hospitals, Disease<$Hospitals>")
    kim.autoExecPolicy = CONCRETE

    kim.playProject(Research, 11) {
          doTask("TerraformRating FROM Disease<$PharmacyUnion>")
          doTask("PlayedEvent FROM $PharmacyUnion")
          kim.autoExecPolicy = EAGER
        }
        .expect(
            "-Disease<$PharmacyUnion>, 4 TerraformRating, 0 Disease<$Hospitals>, -$PharmacyUnion"
        )
  }

  @Test
  internal fun `Two science tags can flip the corporation only once`() {
    kim.setToExMachina(0, "Disease<$PharmacyUnion>")
    kim.autoExecPolicy = CONCRETE

    kim.playProject(Research, 11) {
          doTask("PlayedEvent FROM $PharmacyUnion")
          declineTask()
          kim.autoExecPolicy = EAGER
        }
        .expect("3 TerraformRating, -$PharmacyUnion, PlayedEvent<Class<$PharmacyUnion>>")
  }

  @Test
  internal fun `Flipping the corporation does not pay a Media Group rebate`() {
    kim.setToExMachina(0, "Disease<$PharmacyUnion>")
    kim.exMachina("$MediaGroup")

    kim.playProject(PhysicsComplex, 12) { doTask("PlayedEvent FROM $PharmacyUnion") }
        .expect("-12 MC, 3 TerraformRating, -$PharmacyUnion")
  }

  // FAQ: a pending microbe trigger still loses 4 MC after the corporation flips,
  // but cannot place disease on the corporation that has left play.
  @Test
  internal fun `A microbe trigger still costs money after a science tag flips the corporation`() {
    kim.setToExMachina(0, "Disease<$PharmacyUnion>")
    kim.setToExMachina(17, "MC")
    kim.autoExecPolicy = CONCRETE

    kim.playProject(RegolithEaters, 13) {
          doTask("PlayedEvent FROM $PharmacyUnion")
          declineTask()
          kim.autoExecPolicy = EAGER
        }
        .expect("-17 MC, 3 TerraformRating, 0 Disease<$PharmacyUnion>, -$PharmacyUnion")
  }
}
