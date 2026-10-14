package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class PharmacyUnionTest : TfmSandboxTest() {

  private fun initializeGame() {
    newTestGameBeforeCorporationPlay()
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<PharmacyUnion>>",
          "PharmacyUnion FROM CorporationCard",
      )
      doTask("SearchForCard<Kim, TagFilter<Class<ScienceTag>>>!")
      // Resolve the disease loss separately from the project payment.
      selectTask("-4* MC")
      // Resolve the disease loss separately from the project payment.
      selectTask("-4* MC")
      doTasks(
          "Disease<PharmacyUnion>",
          "Disease<PharmacyUnion>",
          "-15 MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }
    advanceUntilActionPhase()
  }

  // Resolved FAQ: starting money must precede the losses from the two microbe tags.
  @Test
  internal fun `Starting cash covers both disease losses as well as the starting projects`() {
    newTestGameBeforeCorporationPlay()

    kim.inTurn {
          doTasks(
              "PlayCard<Class<CorporationCard>, Class<PharmacyUnion>>",
              "PharmacyUnion FROM CorporationCard",
          )
          doTask("SearchForCard<Kim, TagFilter<Class<ScienceTag>>>!")
          // Resolve the disease loss separately from the project payment.
          selectTask("-4* MC")
          // Resolve the disease loss separately from the project payment.
          selectTask("-4* MC")
          doTasks(
              "Disease<PharmacyUnion>",
              "Disease<PharmacyUnion>",
              "-15 MC",
              "5 ProjectCard<Hand FROM Selecting>",
          )
        }
        .expect("31 MC, 6 ProjectCard, 0 RequiredAction")
  }

  @Test
  internal fun `A science tag removes disease and raises TR`() {
    initializeGame()
    kim.inTurn {
          doTasks("PlayProject<Class<PhysicsComplex>>", "-12 MC")
          doTasks("PhysicsComplex FROM ProjectCard")
          doTask("TerraformRating FROM Disease<PharmacyUnion>")
        }
        .expect("-Disease<$PharmacyUnion>, TerraformRating")
  }

  @Test
  internal fun `An opponent's microbe tag adds disease and costs the corporation money`() {
    initializeGame()
    stan.exMachina("3 OxygenStep, ProjectCard, 5 MC")

    stan
        .inTurn {
          doTasks("PlayProject<Class<Decomposers>>", "-5 MC", "Decomposers FROM ProjectCard")
          doTask("Microbe<Decomposers>*")
          kim.doTask("Disease<PharmacyUnion<Kim>>")
          kim.doTask("-4 MC")
        }
        .expect("Disease<$PharmacyUnion<Kim>>, -4 MC<Kim>")
  }

  @Test
  internal fun `Two science tags can remove the last disease and then flip the corporation`() {
    initializeGame()
    kim.setToExMachina(1, "Disease<$PharmacyUnion>")
    kim.exMachina("$Hospitals, Disease<$Hospitals>")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<Research>>",
              "-11 MC",
              "Research FROM ProjectCard",
              "TerraformRating FROM Disease<PharmacyUnion>",
              "PlayedEvent FROM PharmacyUnion",
          )
          doTask("2 ProjectCard<Kim, Hand>")
          doTask("3 TerraformRating")
        }
        .expect("0 Disease<$Hospitals>")
  }

  @Test
  internal fun `Two science tags can flip the corporation only once`() {
    initializeGame()
    kim.setToExMachina(0, "Disease<$PharmacyUnion>")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<Research>>",
          "-11 MC",
          "Research FROM ProjectCard",
          "PlayedEvent FROM PharmacyUnion",
          "Ok",
      )
      doTask("2 ProjectCard<Kim, Hand>")
      doTask("3 TerraformRating")
    }
  }

  @Test
  internal fun `Flipping the corporation does not pay a Media Group rebate`() {
    initializeGame()
    kim.setToExMachina(0, "Disease<$PharmacyUnion>")
    kim.exMachina("$MediaGroup")

    kim.inTurn {
      doTasks("PlayProject<Class<PhysicsComplex>>", "-12 MC")
      doTasks("PhysicsComplex FROM ProjectCard")
      doTasks("PlayedEvent FROM PharmacyUnion", "3 TerraformRating")
    }
  }

  // FAQ: a pending microbe trigger still loses 4 MC after the corporation flips,
  // but cannot place disease on the corporation that has left play.
  @Test
  internal fun `A microbe trigger still costs money after a science tag flips the corporation`() {
    initializeGame()
    kim.setToExMachina(0, "Disease<$PharmacyUnion>")
    kim.setToExMachina(17, "MC")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<RegolithEaters>>",
              "-13 MC",
              "RegolithEaters FROM ProjectCard",
              "PlayedEvent FROM PharmacyUnion",
              "Ok",
          )
          // Resolve the disease loss separately from the project payment.
          selectTask("-4* MC")
          doTask("3 TerraformRating")
        }
        .expect("-17 MC, 0 Disease<$PharmacyUnion>")
  }
}
