package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnvoysFromVenusTest : TfmSandboxTest() {
  @Test
  internal fun `The two delegates can become a leader and follower in the same party`() {
    playDelegates("PartyFollower<Scientists>").expect("Dominant<Scientists>, 0 LobbyDelegate")
  }

  @Test
  internal fun `The second delegate cannot be placed in another party`() {
    shouldThrow<TaskException> { playDelegates("PartyLeader<Unity>") }
    kim.count("PartyDelegate") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  private fun playDelegates(secondDelegate: String): TaskResult {
    newTestGame("EnvoysFromVenus, TurmoilExpansion")
    kim.exMachina("AerialMappers, VenusMagnetizer, ForcedPrecipitation")
    return kim.inTurn {
      doTasks(
          "PlayProject<Class<EnvoysFromVenus>>",
          "1 Pay<> FROM MC",
          "EnvoysFromVenus FROM ProjectCard",
          "PartyLeader<Scientists>",
          secondDelegate,
      )
      doTask("PlayedEvent<Class<EnvoysFromVenus>> FROM EnvoysFromVenus")
    }
  }
}
