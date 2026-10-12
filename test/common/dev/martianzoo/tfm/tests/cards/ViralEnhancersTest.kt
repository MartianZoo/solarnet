package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ViralEnhancersTest : TfmSandboxTest() {
  // Resolved FAQ: the uncollectible animal option permits taking nothing on a microbe card.
  @Test
  internal fun `Can choose the animal option to take nothing on a microbe collector`() {
    newTestGame()
    kim.exMachina("$ViralEnhancers")

    // The uncollectible animal branch resolves to Ok, selected through declineTask().
    kim.inTurn {
          doTasks("PlayProject<Class<RegolithEaters>>", "-13 MC", "RegolithEaters FROM ProjectCard")
          selectOnlyTask()
          doTask("Ok")
        }
        .expect("0 Microbe<$RegolithEaters>, 0 Plant, 0 Animal")
  }

  @Test
  internal fun `When Viral Enhancers enters play, adds a plant`() {
    newTestGame()
    kim.inTurn {
      doTasks(
          "PlayProject<Class<ViralEnhancers>>",
          "-9 MC",
          "ViralEnhancers FROM ProjectCard",
          "Plant",
      )
    }
  }

  @Test
  internal fun `Triggers once for each bio tag on a card`() {
    newTestGame()
    kim.exMachina("$ViralEnhancers")

    kim.exMachina("$Fish, $AdaptedLichen")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<AdvancedEcosystems>>",
          "-11 MC",
          "AdvancedEcosystems FROM ProjectCard",
          "Plant",
          "Plant",
          "Plant",
      )
    }
  }

  @Test
  internal fun `Each Ecological Zone tag may choose a different Viral Enhancers bonus`() {
    newTestGame()
    kim.exMachina("GreeneryTile<Tharsis_4_4>")
    kim.exMachina("$ViralEnhancers")

    kim.inTurn {
          doTasks("PlayProject<Class<EcologicalZone>>", "-12 MC", "EcologicalZone FROM ProjectCard")
          selectTask("Animal<EcologicalZone>.")
          selectTask("Animal<EcologicalZone>.")
          doTasks("EcologicalZone_SpecialTile<Tharsis_4_5>", "2 Plant", "Plant", "Animal")
        }
        // One plant from Viral Enhancers and two from the Tharsis 4,5 placement bonus.
        .expect("3 Plant, 3 Animal<$EcologicalZone>")
  }

  @Test
  internal fun `Viral Enhancers plant can pay for Moss played with no plants`() {
    newTestGame()
    kim.exMachina("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>")
    kim.exMachina("$ViralEnhancers")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<Moss>>",
              "-4 MC",
              "Moss FROM ProjectCard",
              "PROD[Plant]",
              "Plant",
              "-Plant",
          )
        }
        .expect("0 Plant")
  }

  @Test
  internal fun `Cannot add a microbe to a different card`() {
    initializeExistingMicrobeCard()
    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<RegolithEaters>>")
        selectTask("-X MC?")
        doTasks("-13 MC", "RegolithEaters FROM ProjectCard")
        selectOnlyTask()
        doTask("Microbe<NitriteReducingBacteria>")
      }
    }
  }

  @Test
  internal fun `Adds the chosen microbe to the entering card`() {
    initializeExistingMicrobeCard()
    kim.inTurn {
      doTasks(
          "PlayProject<Class<RegolithEaters>>",
          "-13 MC",
          "RegolithEaters FROM ProjectCard",
          "Microbe<RegolithEaters>!",
      )
    }
  }

  private fun initializeExistingMicrobeCard() {
    newTestGame()
    kim.exMachina("$ViralEnhancers")
    kim.exMachina("$NitriteReducingBacteria")
  }

  @Test
  internal fun `Viral Enhancers responds to both Pharmacy Union tags`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.exMachina("$ViralEnhancers")

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
          selectTask("4 CorporationCard<Kim, Selecting>!")
          doTask("-42 MC")
          selectTask("-3 CorporationCard<Kim, Selecting>!")
          doTask("PlayCard<Class<CorporationCard>, Class<PharmacyUnion>>")
          doTask("PharmacyUnion<Kim> FROM CorporationCard<Kim, Selecting>!")
          selectTask("SearchForCard<Kim, TagFilter<Class<ScienceTag>>>!")
          selectTask("Disease<PharmacyUnion>! OR (MAX 0 PharmacyUnion: Ok)")
          selectTask("-4 MC.")
          selectTask("Disease<PharmacyUnion>! OR (MAX 0 PharmacyUnion: Ok)")
          selectTask("-4 MC.")
          doTasks("Plant", "Plant")
        }
        .expect("2 Disease<$PharmacyUnion>")
  }

  @Test
  internal fun `Viral Enhancers cannot add a disease to Pharmacy Union`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.exMachina("$ViralEnhancers")

    shouldThrow<TaskException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
        selectTask("4 CorporationCard<Kim, Selecting>!")
        selectTask("-42 MC!")
        selectTask("-3 CorporationCard<Kim, Selecting>!")
        selectTask("PlayCard<Kim, Class<CorporationCard>, Class<CardFront>, Selecting>!")
        doTask("PlayCard<Class<CorporationCard>, Class<PharmacyUnion>>")
        selectTask("MAX 0 Barrier: PharmacyUnion FROM CorporationCard<Kim, Selecting>!")
        selectTask("SearchForCard<Kim, TagFilter<Class<ScienceTag>>>!")
        selectTask("Disease<PharmacyUnion>! OR (MAX 0 PharmacyUnion: Ok)")
        selectTask("-4 MC.")
        selectTask("Disease<PharmacyUnion>! OR (MAX 0 PharmacyUnion: Ok)")
        selectTask("-4 MC.")
        doTask("Disease<PharmacyUnion>")
      }
    }
  }

  @Test
  internal fun `Viral Enhancers may choose a plant for a card that cannot hold animals or microbes`() {
    newTestGame()
    kim.exMachina("$ViralEnhancers")
    kim.inTurn {
      doTasks("PlayProject<Class<IndustrialMicrobes>>", "-12 MC")
      selectTask("-X Steel?")
      doTasks("Ok", "IndustrialMicrobes FROM ProjectCard", "PROD[Energy]", "PROD[Steel]", "Plant")
    }
  }

  // Resolved FAQ: choosing an uncollectible animal or microbe may yield nothing.
  @Test
  internal fun `Viral Enhancers may choose nothing even when a plant is available`() {
    newTestGame()
    kim.exMachina("$ViralEnhancers")
    // Choose the uncollectible bonus instead of the available plant.
    kim.inTurn {
          doTasks("PlayProject<Class<IndustrialMicrobes>>", "-12 MC")
          selectTask("-X Steel?")
          doTasks("Ok", "IndustrialMicrobes FROM ProjectCard", "PROD[Energy]", "PROD[Steel]")
          selectOnlyTask()
          doTask("Ok")
        }
        .expect("0 Plant, 0 CardResource")
  }
}
