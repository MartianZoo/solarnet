package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class AridorTest : TfmSandboxTest() {
  @Test
  internal fun `First action adds a selected colony tile`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", kimCorporation = Aridor)

    kim.inTurn {
          doTask("RequiredActionsSignal")
          // Hand colony selection to Admin so the unused tiles become available.
          selectTask("ColonyTileSelection!")
          // Hand the chosen tile to Admin to activate its colony track.
          selectTask("Europa!")
        }
        .expect("Europa, ColonyProduction, -RequiredAction")
  }

  @Test
  internal fun `A delayed colony activates immediately if its resource card is already in play`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", kimCorporation = Aridor)
    kim.exMachina("$TitanShuttles, Floater<$TitanShuttles>")

    kim.inTurn {
          doTask("RequiredActionsSignal")
          // Hand colony selection to Admin so the unused tiles become available.
          selectTask("ColonyTileSelection!")
          // Hand the chosen tile to Admin; Titan remains delayed until floaters are in play.
          selectTask("DelayedTitan!")
        }
        .expect("Titan, ColonyProduction, 0 DelayedTitan")
  }

  @Test
  internal fun `New tag classes count for its owner even if another player already has them`() {
    newTestGame(kimCorporation = Aridor)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      // Hand colony selection to Admin so the unused tiles become available.
      selectTask("ColonyTileSelection!")
      // Hand the chosen tile to Admin to activate its colony track.
      selectTask("Europa!")
    }
    stan.exMachina("$Mine")
    kim.setToExMachina(11, "MC")

    kim.inTurn {
      doTasks("PlayProject<Class<DevelopmentCenter>>", "11 Pay<> FROM MC")
      doTasks(
          "DevelopmentCenter FROM ProjectCard",
          "PROD[MC]",
          "PROD[MC]",
      )
    }
  }

  @Test
  internal fun `Acquiring Aridor does not reward existing tag classes`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.exMachina("$PharmacyUnion, $EarthCatapult")

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
          doTask("4 CorporationCard<Kim, Selecting>")
          doTask("-42 MC")
          doTask("PlayCard<Class<CorporationCard>, Class<Aridor>>")
          doTask("Aridor<Kim> FROM CorporationCard<Kim, Selecting>!")
          doTask("40 MC")
        }
        .expect("PROD[0 MC]")
  }

  // Unresolved adjudication (question 14, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3068230
  @Test
  internal fun `A microbe tag lost when Pharmacy Union flips can be rewarded again`() {
    newTestGame(kimCorporation = Aridor)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      // Hand colony selection to Admin so the unused tiles become available.
      selectTask("ColonyTileSelection!")
      // Hand the chosen tile to Admin to activate its colony track.
      selectTask("Europa!")
    }
    kim.exMachina("$PharmacyUnion, 3 OxygenStep")
    kim.setToExMachina(16, "MC")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<CryoSleep>>",
          "10 Pay<> FROM MC",
          "CryoSleep FROM ProjectCard",
          "PROD[MC]",
      )
      doTasks("PlayedEvent FROM PharmacyUnion", "3 TerraformRating")
    }

    kim.inTurn {
      doTasks(
          "PlayProject<Class<Decomposers>>",
          "5 Pay<> FROM MC",
          "Decomposers FROM ProjectCard",
          "PROD[MC]",
      )
      doTask("Microbe<Decomposers>*")
    }
  }

  // Unresolved adjudication (question 14, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3068230
  @Test
  internal fun `A surviving microbe tag prevents a fresh reward after Pharmacy Union flips`() {
    newTestGame(kimCorporation = Aridor)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      // Hand colony selection to Admin so the unused tiles become available.
      selectTask("ColonyTileSelection!")
      // Hand the chosen tile to Admin to activate its colony track.
      selectTask("Europa!")
    }
    kim.exMachina("$PharmacyUnion, $Decomposers, $Mine")
    kim.setToExMachina(23, "MC")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<CryoSleep>>",
          "10 Pay<> FROM MC",
          "CryoSleep FROM ProjectCard",
          "PROD[MC]",
      )
      doTasks("PlayedEvent FROM PharmacyUnion", "3 TerraformRating")
    }

    kim.inTurn {
          doTasks("PlayProject<Class<IndustrialMicrobes>>", "12 Pay<> FROM MC")
          doTasks(
              "IndustrialMicrobes FROM ProjectCard",
              "PROD[Energy]",
              "PROD[Steel]",
          )
          doTask("Microbe<Decomposers>*")
        }
        .expect("PROD[0 MC]")
  }

  @Test
  internal fun `An event's printed tags do not reward production`() {
    newTestGame(kimCorporation = Aridor)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      // Hand colony selection to Admin so the unused tiles become available.
      selectTask("ColonyTileSelection!")
      // Hand the chosen tile to Admin to activate its colony track.
      selectTask("Europa!")
    }

    kim.inTurn {
          doTasks(
              "PlayProject<Class<BribedCommittee>>",
              "7 Pay<> FROM MC",
              "BribedCommittee FROM ProjectCard",
              "2 TerraformRating",
          )
          doTask("PlayedEvent<Class<BribedCommittee>> FROM BribedCommittee")
        }
        .expect("PROD[0 MC]")
  }

  @Test
  internal fun `Two copies of a new tag on Pharmacy Union reward production only once`() {
    newTestGame(addOptions = "PreludeExpansion", kimCorporation = Aridor)

    kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      doTask("4 CorporationCard<Kim, Selecting>")
      doTask("PlayCard<Class<CorporationCard>, Class<PharmacyUnion>>")
      doTask("PharmacyUnion<Kim> FROM CorporationCard<Kim, Selecting>!")
      doTask("54 MC")
      doTask("-42 MC")
      doTask("SearchForCard<Kim, TagFilter<Class<ScienceTag>>>!")
      doTask("PROD[MC]")
      doTask("Disease<PharmacyUnion>")
      doTask("-4* MC")
      doTask("Disease<PharmacyUnion>")
      doTask("-4* MC")
    }
  }

  @Test
  internal fun `A Venus tag can introduce a new tag class`() {
    newTestGame(kimCorporation = Aridor)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      // Hand colony selection to Admin so the unused tiles become available.
      selectTask("ColonyTileSelection!")
      // Hand the chosen tile to Admin to activate its colony track.
      selectTask("Europa!")
    }

    kim.setToExMachina(11, "MC")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<AerialMappers>>",
          "11 Pay<> FROM MC",
          "AerialMappers FROM ProjectCard",
          "PROD[MC]",
      )
    }
  }
}
