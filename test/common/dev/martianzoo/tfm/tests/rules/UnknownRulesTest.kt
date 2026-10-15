package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Current behavior for questions whose rule target is not settled. */
internal class UnknownRulesTest : TfmSandboxTest() {
  // The adjudication of required-action timing remains unresolved.
  // BGG Valley Trust/Merger first-action discussion:
  // https://boardgamegeek.com/thread/2874012/article/40859020#40859020
  @Test
  internal fun `Valley Trust Merger Tharsis currently places the city in the first required action`() {
    newTestGame(addOptions = "PreludeExpansion, ValleyTrust", kimCorporation = ValleyTrust)
    kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      doTask("4 CorporationCard<Kim, Selecting>")
      doTask("PlayCard<Class<CorporationCard>, Class<TharsisRepublic>>")
      doTask("TharsisRepublic<Kim> FROM CorporationCard<Kim, Selecting>!")
      doTasks("40 MC", "-42 MC")
      doTask("TharsisRepublic_RequiredAction!")
    }
    advanceUntilActionPhase()

    kim.inTurn {
      doTask("RequiredActionsSignal")
      doTask("3 PreludeCard<Kim, Selecting>")
      doTask("PlayCard<Class<PreludeCard>, Class<Donation>>")
      doTask("Donation<Kim> FROM PreludeCard<Kim, Selecting>!")
      doTasks("21 MC", "CityTile<Tharsis_3_3>", "PROD[MC]", "3 MC")
    }
    kim.count("CityTile<Tharsis_3_3>") shouldBe 1
    kim.count("RequiredAction") shouldBe 0
  }

  // BGG Head Start first-action ruling:
  // https://boardgamegeek.com/thread/2993276/article/41447529#41447529
  // Unresolved adjudication (question 4, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/2886401/article/44823945#44823945
  @Test
  internal fun `Head Start Board Merger Tharsis currently uses the next granted action for its city`() {
    newTestGame(addOptions = "PreludeExpansion, fakes:cards, BoardOfDirectors")
    kim.exMachina("$BoardOfDirectors, Director<$BoardOfDirectors>")
    kim.setToExMachina(54, "MC")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
      )
      doTask("20 MC")
      doTask("UseCardAction<BoardOfDirectors, Action1>")
      doTask("PreludeCard<Kim, Hand>")
      doTask("-12 MC")
      doTask("-Director<BoardOfDirectors>")
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      doTask("4 CorporationCard<Kim, Selecting>")
      doTask("-42 MC")
      doTask("PlayCard<Class<CorporationCard>, Class<TharsisRepublic>>")
      doTask("TharsisRepublic<Kim> FROM CorporationCard<Kim, Selecting>!")
      doTask("40 MC")
      doTask("TharsisRepublic_RequiredAction!")
      doTask("RequiredActionsSignal")
      doTasks("CityTile<Tharsis_3_3>", "PROD[MC]", "3 MC")
    }

    kim.count("CityTile<Tharsis_3_3>") shouldBe 1
    kim.count("RequiredAction") shouldBe 0
  }

  // Unresolved adjudication (question 5, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3341272/article/44630207#44630207
  @Test
  internal fun `Original Poseidon currently keeps an impossible first colony action pending`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", kimCorporation = Poseidon)
    fillSelectedColonySlots()

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("RequiredActionsSignal")

        doTask("Colony<Luna>")
      }
    }
    kim.count("RequiredAction") shouldBe 1
    kim.count("Colony") shouldBe 0
  }

  // Unresolved adjudication (question 6, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3341272
  @Test
  internal fun `Merger Poseidon currently remains acquired when its later first colony is impossible`() {
    newTestGame(addOptions = "PreludeExpansion, Luna, Ceres, Triton, Ganymede, Callisto")
    kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      doTask("4 CorporationCard<Kim, Selecting>")
      doTask("-42 MC")
      doTask("PlayCard<Class<CorporationCard>, Class<Poseidon>>")
      doTask("Poseidon FROM CorporationCard<Selecting>")
      doTask("45 MC")
      doTask("Poseidon_RequiredAction")
    }
    kim.setToExMachina(20, "MC")
    fillSelectedColonySlots()
    advanceUntilActionPhase()

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("RequiredActionsSignal")

        doTask("Colony<Luna>")
      }
    }
    kim.count("$Poseidon") shouldBe 1
    kim.count("$Merger") shouldBe 1
    kim.count("MC") shouldBe 20
    kim.count("RequiredAction") shouldBe 1
    kim.count("Colony") shouldBe 0
  }

  // Unresolved adjudication (question 1): may production wait until after seeing the Preludes?
  // https://boardgamegeek.com/thread/2626062
  @Test
  internal fun `New Partner currently permits drawing before raising production`() {
    newTestGame(addOptions = "PreludeExpansion, IndustrialComplex, -QuickStartVariant")
    kim.setToExMachina(0, "PROD[MC]")
    kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<NewPartner>>", "NewPartner FROM PreludeCard")
      doTask("2 PreludeCard<Selecting>")
      doTask("PROD[MC]")
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>",
          "IndustrialComplex FROM PreludeCard<Selecting>",
          "-18 MC",
      )
      // Expand the production box after New Partner has raised MC production to its target.
      selectOnlyTask()
      doTasks("PROD[Steel]", "PROD[Titanium]", "PROD[Plant]", "PROD[Energy]", "PROD[Heat]")
    }
    kim.count("PROD[MC]") - kim.count("ProdOffset<Class<MC>>") shouldBe 1
  }

  // Unresolved adjudication (question 1): completing Industrial Complex first changes the result.
  // https://boardgamegeek.com/thread/2626062
  @Test
  internal fun `New Partner currently permits its production increase after Industrial Complex`() {
    newTestGame(addOptions = "PreludeExpansion, IndustrialComplex, -QuickStartVariant")
    kim.setToExMachina(0, "PROD[MC]")
    kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<NewPartner>>", "NewPartner FROM PreludeCard")
      doTask("2 PreludeCard<Selecting>")
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>",
          "IndustrialComplex FROM PreludeCard<Selecting>",
          "-18 MC",
      )
      // Expand Industrial Complex's per-resource adjustment while New Partner remains pending.
      selectTask(
          "EACH Class<@StandardResource> { PROD[@StandardResource / (Class<@StandardResource> OR QuickStartVariant OR ProdOffset<Class<@StandardResource>>) - Production<Class<@StandardResource>>] }"
      )
      doTasks(
          "PROD[MC]",
          "PROD[Steel]",
          "PROD[Titanium]",
          "PROD[Plant]",
          "PROD[Energy]",
          "PROD[Heat]",
      )
      doTask("PROD[MC]")
    }
    kim.count("PROD[MC]") - kim.count("ProdOffset<Class<MC>>") shouldBe 2
  }

  // Unresolved adjudication (question 2): neither copied box can finish first from zero production.
  // https://boardgamegeek.com/thread/3213894/article/43917527#43917527
  @Test
  internal fun `Cyberia currently interleaves Heat Trappers and GHG Factories production`() {
    newTestGame()
    kim.exMachina("GhgFactories, HeatTrappers")
    kim.setToExMachina(0, "PROD[Energy]")
    kim.setToExMachina(0, "PROD[Heat]")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<CyberiaSystems>>",
          "16 Pay<> FROM MC",
          "CyberiaSystems FROM ProjectCard",
          "PROD[Steel]",
      )
      doTask("CopyProductionBox<GhgFactories>")
      doTask("PROD[4 Heat]")
      doTask("CopyProductionBox<HeatTrappers>")
      doTasks("PROD[Energy]", "PROD[-2 Heat]", "PROD[-Energy]")
    }
  }

  // Unresolved adjudication (question 3): paying Merger before the triggered loss succeeds.
  // https://boardgamegeek.com/thread/3067197
  @Test
  internal fun `Merger currently can be paid before Pharmacy Union takes Recyclon's cash`() {
    newTestGame(addOptions = "PreludeExpansion, BoardOfDirectors")
    advanceUntilActionPhase()
    kim.exMachina(
        "PharmacyUnion, 2 Disease<PharmacyUnion>, BoardOfDirectors, Director<BoardOfDirectors>"
    )
    kim.setToExMachina(17, "MC")
    kim.inTurn {
      doTasks(
          "UseCardAction<BoardOfDirectors, Action1>",
          "PreludeCard<Hand>",
          "-12 MC",
          "-Director<BoardOfDirectors>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<Merger>>",
          "Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
      )
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<Recyclon>>",
          "Recyclon FROM CorporationCard<Selecting>",
          "38 MC",
      )
      doTasks("-42 MC", "Disease<PharmacyUnion>", "-1 MC", "PROD[Steel]", "Microbe<Recyclon>")
    }
    kim.count("MC") shouldBe 0
  }

  // Unresolved adjudication (question 7): current timing retains the corporation until a later
  // action.
  // https://boardgamegeek.com/thread/3478406/article/45816668#45816668
  @Test
  internal fun `Late Merger currently retains Poseidon even when its first colony is already impossible`() {
    newTestGame(
        addOptions = "PreludeExpansion, BoardOfDirectors, Luna, Ceres, Triton, Ganymede, Callisto"
    )
    advanceUntilActionPhase()
    kim.exMachina("BoardOfDirectors, Director<BoardOfDirectors>")
    fillSelectedColonySlots()
    kim.inTurn {
      doTasks(
          "UseCardAction<BoardOfDirectors, Action1>",
          "PreludeCard<Hand>",
          "-12 MC",
          "-Director<BoardOfDirectors>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<Merger>>",
          "Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
      )
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<Poseidon>>",
          "Poseidon FROM CorporationCard<Selecting>",
          "45 MC",
          "-42 MC",
          "Poseidon_RequiredAction",
      )
    }
    shouldThrow<LimitsException> {
      kim.inTurn { doTasks("RequiredActionsSignal", "Colony<Luna>") }
    }
    kim.count("Poseidon") shouldBe 1
    kim.count("Merger") shouldBe 1
    kim.count("RequiredAction") shouldBe 1
    kim.count("MC") shouldBe 33
  }

  // Unresolved adjudication (question 9): starting production currently triggers Manutech.
  // https://boardgamegeek.com/thread/3067197
  @Test
  internal fun `Thorgate acquired through Merger currently gives Manutech energy`() {
    newTestGame(addOptions = "PreludeExpansion", kimCorporation = Manutech)
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<Merger>>",
          "Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
      )
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<ThorGate>>",
          "ThorGate FROM CorporationCard<Selecting>",
          "48 MC",
          "-42 MC",
          "PROD[Energy]",
          "Energy",
      )
    }
  }

  // Unresolved adjudication (question 11): does an intervening Prelude consume Conscription?
  // https://boardgamegeek.com/thread/3775830/article/48238727#48238727
  @Test
  internal fun `Conscription currently survives a Prelude played through Board of Directors`() {
    newTestGame(addOptions = "PreludeExpansion, BoardOfDirectors")
    advanceUntilActionPhase()
    kim.exMachina("BoardOfDirectors, Director<BoardOfDirectors>, AcquiredCompany, Sponsors")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<Conscription>>",
          "5 Pay<> FROM MC",
          "Conscription FROM ProjectCard",
          "Conscription_NextCardEffect",
          "PlayedEvent<Class<Conscription>> FROM Conscription",
      )
    }
    kim.inTurn {
      doTasks(
          "UseCardAction<BoardOfDirectors, Action1>",
          "PreludeCard<Hand>",
          "-12 MC",
          "-Director<BoardOfDirectors>",
      )
      doTasks("PlayCard<Class<PreludeCard>, Class<Donation>>", "Donation FROM PreludeCard", "21 MC")
    }
    kim.inTurn {
      doTasks(
          "PlayProject<Class<Soletta>>",
          "19 Pay<> FROM MC",
          "Soletta FROM ProjectCard",
          "PROD[7 Heat]",
      )
    }
  }

  // Unresolved adjudication (question 13): may Splice's recipient refuse the cash fallback?
  // https://boardgamegeek.com/thread/2240214/article/32499468#32499468
  @Test
  internal fun `Splice currently permits an uncollectible microbe instead of money`() {
    newTestGame()
    kim.exMachina("SpliceTacticalGenomics")
    kim.inTurn {
          doTasks(
              "PlayProject<Class<IndustrialMicrobes>>",
              "12 Pay<> FROM MC",
              "IndustrialMicrobes FROM ProjectCard",
              "PROD[Energy]",
              "PROD[Steel]",
          )
          doTask("Ok") // Choose the microbe that Industrial Microbes cannot hold.
          doTask("2 MC") // Splice's owner's reward, separate from the card player's choice.
        }
        .expect("-10 MC, 0 CardResource")
  }

  // Unresolved adjudication (question 18): red-outline permission versus stealing from another
  // player.
  // https://boardgamegeek.com/thread/3415045/article/48249560#48249560
  @Test
  internal fun `Hired Raiders currently rejects a positive self steal`() {
    newTestGame()
    kim.setToExMachina(2, "Steel")
    shouldThrow<ExpressionException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<HiredRaiders>>",
            "1 Pay<> FROM MC",
            "HiredRaiders FROM ProjectCard",
        )
        doTask("Steel<Kim FROM Kim>")
      }
    }
    kim.count("Steel") shouldBe 2
    kim.count("MC") shouldBe 42
  }

  // Unresolved adjudication (question 29): Innovator's broad card count includes these card
  // families.
  // https://boardgamegeek.com/thread/3336116
  @Test
  internal fun `Innovator currently counts corporations and Preludes as well as projects`() {
    newTestGame(addOptions = "PreludeExpansion, Innovator, Thermalist, Miner")
    kim.exMachina("ThorGate, Donation")
    stan.exMachina("Mine")
    advanceUntilActionPhase()
    kim.inTurn { doTasks("FundAward<Class<Innovator>>", "8 Pay<> FROM MC", "Innovator") }
    victoryPoints() shouldBe listOf(25, 22, 20)
  }

  // Unresolved adjudication (question 30): a dormant negative scoring effect may or may not count.
  // https://boardgamegeek.com/thread/3520555
  @Test
  internal fun `Philanthropist currently excludes Vermin before its penalty activates`() {
    newTestGame(addOptions = "Philantropist, Builder, Engineer")
    kim.exMachina("Vermin, DustSeals, AdaptationTechnology, CryoSleep, SpaceElevator")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks("ClaimMilestone<Class<Philantropist>>", "8 Pay<> FROM MC", "Philantropist")
      }
    }
    kim.count("Philantropist") shouldBe 0
    kim.setToExMachina(7, "OxygenStep")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<Windmills>>",
          "6 Pay<> FROM MC",
          "Windmills FROM ProjectCard",
          "PROD[Energy]",
      )
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Philantropist>>", "8 Pay<> FROM MC", "Philantropist")
    }
  }

  // Unresolved adjudication (question 30): Vermin penalizes opponents but no cityless owner.
  // https://boardgamegeek.com/thread/3520555
  @Test
  internal fun `Philanthropist currently excludes Vermin when only opponents lose points`() {
    newTestGame(addOptions = "Philantropist, Builder, Engineer")
    kim.exMachina(
        "Vermin, 10 Animal<Vermin>, DustSeals, AdaptationTechnology, CryoSleep, SpaceElevator"
    )
    stan.exMachina("NormalCityTile<Tharsis_3_3>")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks("ClaimMilestone<Class<Philantropist>>", "8 Pay<> FROM MC", "Philantropist")
      }
    }
    kim.count("Philantropist") shouldBe 0
    kim.setToExMachina(7, "OxygenStep")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<Windmills>>",
          "6 Pay<> FROM MC",
          "Windmills FROM ProjectCard",
          "PROD[Energy]",
      )
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Philantropist>>", "8 Pay<> FROM MC", "Philantropist")
    }
  }

  // Unresolved adjudication (question 31): Vitor's referenced VP icon is not its own scoring
  // effect.
  // https://boardgamegeek.com/thread/3544105
  @Test
  internal fun `Philanthropist currently excludes Vitor's effect icon`() {
    newTestGame(addOptions = "Philantropist, Builder, Engineer, Vitor")
    kim.exMachina("Vitor, DustSeals, AdaptationTechnology, CryoSleep, SpaceElevator")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks("ClaimMilestone<Class<Philantropist>>", "8 Pay<> FROM MC", "Philantropist")
      }
    }
    kim.count("Philantropist") shouldBe 0
    kim.setToExMachina(7, "OxygenStep")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<Windmills>>",
          "6 Pay<> FROM MC",
          "Windmills FROM ProjectCard",
          "PROD[Energy]",
          "3 MC",
      )
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Philantropist>>", "8 Pay<> FROM MC", "Philantropist")
    }
  }

  // Unresolved adjudication (question 21); current wild-space support also blocks copying.
  // https://boardgamegeek.com/thread/2663453
  @Test
  internal fun `Copying Mining Rights currently rejects a wild resource space`() {
    copyWildSpaceMiningBox("MiningRights")
  }

  // Unresolved adjudication (question 21); no original metal choice is available to revisit here.
  // https://boardgamegeek.com/thread/2663453
  @Test
  internal fun `Copying Mining Area currently rejects a wild resource space`() {
    copyWildSpaceMiningBox("MiningArea")
  }

  private fun copyWildSpaceMiningBox(card: String) {
    newTestGame(addOptions = "AmazonisMap, Unsafe")
    // Prepare the disputed position: the wild placement bonus was taken as a plant.
    kim.exMachina("$card, ${card}_SpecialTile<Amazonis_05_03>, Plant")
    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<RoboticWorkforce>>",
            "9 Pay<> FROM MC",
            "RoboticWorkforce FROM ProjectCard",
        )
        doTask("CopyProductionBox<$card>")
        // Resolve the metal filter, which does not recognize the wild placement bonus.
        selectOnlyTask()
      }
    }
    kim.count("RoboticWorkforce") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  // Unresolved adjudication (question 8). The caller reports search results; there is no deck here.
  // Current search tasks require the complete result and cannot represent partial exhaustion.
  // https://boardgamegeek.com/thread/1705209/article/37732543#37732543
  @Test
  internal fun `Celestic currently rejects reporting only a single first action search result`() {
    newTestGame(kimCorporation = Celestic)
    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("RequiredActionsSignal")
        doTask("SearchForCard<ReferenceFilter<Class<Floater>>>")
      }
    }
    kim.count("RequiredAction") shouldBe 1
    kim.count("ProjectCard") shouldBe 5
    kim.inTurn {
          doTasks("RequiredActionsSignal", "2 SearchForCard<ReferenceFilter<Class<Floater>>>")
        }
        .expect("2 ProjectCard, -RequiredAction")
  }

  // Unresolved adjudication (question 8); this records the same limitation for a three-card search.
  // https://boardgamegeek.com/thread/3433477/article/45421447#45421447
  @Test
  internal fun `Morning Star currently rejects reporting only two first action search results`() {
    newTestGame(kimCorporation = MorningStarInc)
    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("RequiredActionsSignal")
        doTask("2 SearchForCard<TagFilter<Class<VenusTag>>>")
      }
    }
    kim.count("RequiredAction") shouldBe 1
    kim.count("ProjectCard") shouldBe 5
    kim.inTurn {
          doTasks("RequiredActionsSignal", "3 SearchForCard<TagFilter<Class<VenusTag>>>")
        }
        .expect("3 ProjectCard, -RequiredAction")
  }

  // Unresolved adjudication (question 17): this probes zero explicitly, rather than declining with
  // Ok.
  // https://boardgamegeek.com/thread/3415045/article/48261869#48261869
  @Test
  internal fun `Hired Raiders currently rejects stealing zero MC`() {
    newTestGame()
    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<HiredRaiders>>",
            "1 Pay<> FROM MC",
            "HiredRaiders FROM ProjectCard",
        )
        doTask("0 MC<Kim FROM Stan>")
      }
    }
    kim.count("MC") shouldBe 42
    kim.count("PlayedEvent<Class<HiredRaiders>>") shouldBe 0
  }

  private fun fillSelectedColonySlots() {
    fillColonySlots("Luna")
    fillColonySlots("Ceres")
    fillColonySlots("Triton")
    fillColonySlots("Ganymede")
    fillColonySlots("Callisto")
  }

  private fun fillColonySlots(track: String) {
    // Keep separate placements: a batched gain of three colonies overshoots the track.
    stan.setToExMachina(1, "Colony<$track>")
    stan.setToExMachina(2, "Colony<$track>")
    stan.setToExMachina(3, "Colony<$track>")
  }
}
