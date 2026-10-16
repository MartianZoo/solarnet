package dev.martianzoo.tfm.tests

import dev.martianzoo.pets.api.Exceptions.TaskException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WildTagContinuationTest : TfmSandboxTest() {
  @Test
  internal fun `Corroder Suits and Maxwell Base can add science to Applied Science through its wild`() {
    newTestGame("VenusNextExpansion, AppliedScience")
    kim.exMachina("AppliedScience, MaxwellBase")
    kim.inTurn {
          doTasks(
              "PlayProject<Class<CorroderSuits>>",
              "8 Pay<> FROM MC",
              "CorroderSuits FROM ProjectCard",
              "PROD[2 MC]",
          )
          kim.count("VenusTag<WildTagElection<WildTag<AppliedScience>>>") shouldBe 1
          doTask("Science<AppliedScience>")
        }
        .expect("Science<AppliedScience>, 0 Tag<WildTagElection<WildTag>>")
    kim.inTurn {
          doTask("UseCardAction<MaxwellBase, Action1>")
          kim.count("VenusTag<WildTagElection<WildTag<AppliedScience>>>") shouldBe 1
          doTask("Science<AppliedScience>")
        }
        .expect("Science<AppliedScience>, 0 Tag<WildTagElection<WildTag>>")
  }

  @Test
  internal fun `Venus Shuttles assigns Venus before calculating its action price`() {
    newTestGame("VenusNextExpansion, prelude2:cards, ResearchCoordination")
    kim.exMachina("ResearchCoordination, VenusShuttles")
    kim.inTurn {
      kim.doTasks("UseCardAction<VenusShuttles, Action1>")
          .expect("VenusTag<WildTagElection<WildTag<ResearchCoordination>>>")
      doTask("10 Pay<> FROM MC")
      doTask("VenusStep")
      doTask("TerraformRating")
    }
    kim.count("VenusTag") shouldBe 1
  }

  @Test
  internal fun `a repeated action inside Project Inspection allocates through the event's cleanup`() {
    newTestGame("ResearchCoordination")
    kim.exMachina("ResearchCoordination, OrbitalCleanup")
    kim.inTurn {
      doTasks("UseCardAction<OrbitalCleanup, Action1>", "MC")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<ProjectInspection>>", "ProjectInspection FROM ProjectCard")
      kim.doTasks("UseAction<OrbitalCleanup, Action1>")
          .expect("ScienceTag<WildTagElection<WildTag<ResearchCoordination>>>")
      kim.count("ScienceTag<WildTagElection<WildTag<ResearchCoordination>>>") shouldBe 1
      doTask("MC")
      doTask("PlayedEvent<Class<ProjectInspection>> FROM ProjectInspection")
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `Solar Probe counts its assigned Science before both event and wild cleanup`() {
    newTestGame("ResearchCoordination")
    kim.exMachina("ResearchCoordination, SearchForLife")
    kim.inTurn {
      doTasks("PlayProject<Class<SolarProbe>>", "9 Pay<> FROM MC", "SolarProbe FROM ProjectCard")
      kim.count("ScienceTag<WildTagElection<WildTag<ResearchCoordination>>>") shouldBe 1
      shouldThrow<TaskException> { doTask("PlayedEvent<Class<SolarProbe>> FROM SolarProbe") }
      doTask("ProjectCard")
      doTask("PlayedEvent<Class<SolarProbe>> FROM SolarProbe")
    }
    kim.count("ScienceTag") shouldBe 1
  }

  @Test
  internal fun `Orbital Cleanup allocates only when its action is used and keeps Science through payout`() {
    newTestGame("ResearchCoordination, ResearchNetwork")
    kim.exMachina("ResearchCoordination, ResearchNetwork, OrbitalCleanup, Research")
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
    kim.inTurn {
      kim.doTasks("UseCardAction<OrbitalCleanup, Action1>")
          .expect(
              "ScienceTag<WildTagElection<WildTag<ResearchCoordination>>>, ScienceTag<WildTagElection<WildTag<ResearchNetwork>>>"
          )
      kim.count("ScienceTag<WildTagElection<WildTag<ResearchCoordination>>>") shouldBe 1
      doTask("4 MC")
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `Kuiper Cooperative uses the same wild as Space on a later action`() {
    newTestGame("ResearchCoordination")
    kim.exMachina("ResearchCoordination, KuiperCooperative, OrbitalCleanup")
    kim.inTurn {
      doTask("UseCardAction<OrbitalCleanup, Action1>")
      doTask("MC")
    }
    kim.inTurn {
      kim.doTasks("UseCardAction<KuiperCooperative, Action1>")
          .expect("SpaceTag<WildTagElection<WildTag<ResearchCoordination>>>")
      doTask("4 Asteroid<KuiperCooperative>")
    }
  }
}
