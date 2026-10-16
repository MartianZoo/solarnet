package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AmazonisSoloSetupTest : TfmSandboxTest() {
  @Test
  internal fun amazonisTr63SoloStartsWithThirteenGenerationsLeft() {
    newTestGame(addOptions = "AmazonisMap, Tr63SoloObjective", playerCount = 1)

    admin.count("SoloGenerationsLeft") shouldBe 13
  }

  @Test
  internal fun amazonisTr63SoloWithPreludeStartsWithElevenGenerationsLeft() {
    newTestGame(addOptions = "AmazonisMap, Tr63SoloObjective, PreludeExpansion", playerCount = 1)

    admin.count("SoloGenerationsLeft") shouldBe 11
  }

  @Test
  internal fun amazonisStandardSoloStartsWithFourteenGenerationsLeft() {
    newTestGame(addOptions = "AmazonisMap, StandardSoloObjective", playerCount = 1)

    admin.count("SoloGenerationsLeft") shouldBe 14
  }

  @Test
  internal fun amazonisStandardSoloWithPreludeStartsWithTwelveGenerationsLeft() {
    newTestGame(
        addOptions = "AmazonisMap, StandardSoloObjective, PreludeExpansion",
        playerCount = 1,
    )

    admin.count("SoloGenerationsLeft") shouldBe 12
  }
}
