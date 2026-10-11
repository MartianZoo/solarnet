package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class EcologyExpertsTest : TfmSandboxTest() {
  // BGG Ecology Experts tag timing:
  // https://boardgamegeek.com/thread/2096075/article/30501757#30501757
  @Ignore // Known defect: the newly played listener misses Ecology Experts' plant tag.
  @Test
  internal fun `Its plant tag triggers the Ecological Zone it plays`() {
    playEcologicalZone().expect("3 Animal<$EcologicalZone>")
  }

  @Test
  internal fun `BUG - Its plant tag does not trigger the Ecological Zone it plays`() {
    playEcologicalZone().expect("2 Animal<$EcologicalZone>")
  }

  private fun playEcologicalZone(): TaskResult {
    newTestGame(addOptions = "PreludeExpansion, EcologyExperts, Unsafe")
    kim.exMachina("GreeneryTile<Tharsis_4_4>")

    return with(kim) {
      inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
            "EcologyExperts FROM PreludeCard",
            "PROD[Plant]",
            "PlayCard<Class<ProjectCard>, Class<EcologicalZone>>",
        )
        selectTask("-Required. / Required")
        doTasks("-12 MC", "EcologicalZone FROM ProjectCard")
        selectTask("Animal<EcologicalZone>.")
        selectTask("Animal<EcologicalZone>.")
        doTasks("EcologicalZone_SpecialTile<Tharsis_4_5>", "2 Plant")
      }
    }
  }

  @Test
  internal fun `Cannot choose another Prelude card`() {
    newTestGame(addOptions = "PreludeExpansion, EcologyExperts, Unsafe")
    val unplayedPreludes = kim.count("PreludeCard")

    shouldThrow<NarrowingException> {
      with(kim) {
        inTurn {
          selectOnlyTask()
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
              "EcologyExperts FROM PreludeCard",
          )
          selectTask("PROD[Plant]")
          selectOnlyTask()
          doTask("PlayCard<Class<PreludeCard>, Class<ExcentricSponsor>>")
        }
      }
    }

    kim.assertCounts(0 to "$EcologyExperts", 0 to "$ExcentricSponsor")
    kim.count("PreludeCard") shouldBe unplayedPreludes
  }

  @Test
  internal fun `Plays Decomposers while ignoring its global requirement`() {
    newTestGame(addOptions = "PreludeExpansion, EcologyExperts, Unsafe")

    with(kim) {
      inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
            "EcologyExperts FROM PreludeCard",
            "PROD[Plant]",
            "PlayCard<Class<ProjectCard>, Class<Decomposers>>",
        )
        selectTask("-Required. / Required")
        doTasks("-5 MC", "Decomposers FROM ProjectCard")
        selectTask("Microbe<Decomposers>.")
      }
    }
  }

  @Test
  internal fun `Splice money from Ecology Experts tags can pay for Decomposers`() {
    newTestGame(addOptions = "PreludeExpansion, EcologyExperts, Unsafe")
    stan.exMachina("$SpliceTacticalGenomics")
    kim.setToExMachina(4, "MC")
    val spliceMoney = stan.count("MC")

    with(kim) {
      inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
            "EcologyExperts FROM PreludeCard",
            "PROD[Plant]",
        )
        selectTask("2 MC<Stan>!")
        stan.selectTask("2 MC!")
        doTasks("2 MC", "PlayCard<Class<ProjectCard>, Class<Decomposers>>")
        selectTask("-Required. / Required")
        doTasks("-5 MC", "Decomposers FROM ProjectCard")
        selectTask("Microbe<Decomposers>.")
        selectTask("2 MC<Stan>!")
        stan.selectTask("2 MC!")
        doTask("2 MC")
      }
    }

    kim.assertCounts(3 to "MC")
    stan.count("MC") shouldBe spliceMoney + 4
  }

  @Test
  internal fun `Can play a card without a bio tag`() {
    newTestGame(addOptions = "PreludeExpansion, EcologyExperts, Unsafe")

    with(kim) {
      inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
            "EcologyExperts FROM PreludeCard",
            "PROD[Plant]",
            "PlayCard<Class<ProjectCard>, Class<DustSeals>>",
        )
        selectTask("-Required. / Required")
        doTasks("-2 MC", "DustSeals FROM ProjectCard")
      }
    }
  }

  @Test
  internal fun `Double Down copies Ecology Experts project play and requirement waiver`() {
    newTestGame(addOptions = "PreludeExpansion, EcologyExperts, Unsafe")

    with(kim) {
      inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
            "EcologyExperts FROM PreludeCard",
            "PROD[Plant]",
            "PlayCard<Class<ProjectCard>, Class<DustSeals>>",
        )
        selectTask("-Required. / Required")
        doTasks("-2 MC", "DustSeals FROM ProjectCard")
      }
      inTurn {
        doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
        selectOnlyTask()
        doTasks(
            "CopyPrelude<EcologyExperts>",
            "PROD[Plant]",
            "PlayCard<Class<ProjectCard>, Class<Decomposers>>",
        )
        selectTask("-Required. / Required")
        doTasks("-5 MC", "Decomposers FROM ProjectCard")
        selectTask("Microbe<Decomposers>.")
      }
    }
  }

  @Test
  internal fun `Ecology Experts does not waive a political requirement`() {
    newTestGame(addOptions = "PreludeExpansion, TurmoilExpansion, EcologyExperts, Unsafe")

    shouldThrow<RequirementException> {
      with(kim) {
        inTurn {
          selectOnlyTask()
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
              "EcologyExperts FROM PreludeCard",
          )
          selectTask("PROD[Plant]")
          selectOnlyTask()
          doTask("PlayCard<Class<ProjectCard>, Class<SupportedResearch>>")
        }
      }
    }
    kim.assertCounts(0 to "$EcologyExperts", 0 to "$SupportedResearch", 10 to "ProjectCard")
  }

  // https://boardgamegeek.com/thread/2096075/article/30501757#30501757
  @Ignore // The newly played listener misses Ecology Experts bio tags.
  @Test
  internal fun `Its bio tags trigger the Viral Enhancers it plays`() {
    playViralEnhancers().expect("3 Plant")
  }

  @Test
  internal fun `BUG - Its bio tags do not trigger the Viral Enhancers it plays`() {
    playViralEnhancers().expect("Plant")
  }

  private fun playViralEnhancers(): TaskResult {
    newTestGame(addOptions = "PreludeExpansion, CorporateEraExpansion, EcologyExperts, Unsafe")
    return with(kim) {
      inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
            "EcologyExperts FROM PreludeCard",
            "PROD[Plant]",
            "PlayCard<Class<ProjectCard>, Class<ViralEnhancers>>",
        )
        selectTask("-Required. / Required")
        doTasks("-9 MC", "ViralEnhancers FROM ProjectCard", "Plant")
      }
    }
  }
}
