package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GlobalParameterCompletionTest : TfmSandboxTest() {
  @Test
  internal fun atomizedGainStopsWhenTheTrackCompletes() {
    newTestGame()

    // Deliberately inject an oversized gain to exercise atomization at the track limit.
    kim.runOperation("55 OxygenStep") { doTasks("TemperatureStep", "15 TerraformRating") }

    kim.count("OxygenStep") shouldBe 14
    kim.count("GpComplete<Class<OxygenStep>>") shouldBe 1
  }

  @Test
  internal fun eachTrackRecordsCompletionOnItsFinalStep() {
    newTestGame()

    kim.exMachina("18 TemperatureStep, 13 OxygenStep, 14 VenusStep")
    agents.correct(
        "OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, " +
            "OceanTile<Tharsis_4_8>, OceanTile<Tharsis_5_4>, OceanTile<Tharsis_5_5>, " +
            "OceanTile<Tharsis_5_6>, OceanTile<Tharsis_6_7>"
    )

    kim.runOperation("TemperatureStep, OxygenStep, VenusStep") { doTask("3 TerraformRating") }

    kim.count("GameEndBarrier") shouldBe 1

    kim.runOperation("OceanTile<Tharsis_1_2>") { doTasks("TerraformRating", "2 Steel") }

    kim.count("GpComplete<Class<TemperatureStep>>") shouldBe 1
    kim.count("GpComplete<Class<OxygenStep>>") shouldBe 1
    kim.count("GpComplete<Class<OceanTile>>") shouldBe 1
    kim.count("GpComplete<Class<VenusStep>>") shouldBe 1
    kim.count("GpIncomplete") shouldBe 0
    kim.count("GameEndBarrier") shouldBe 0

    kim.runOperation("TemperatureStep, OceanTile<Tharsis_3_3>")
    shouldThrow<DeadEndException> { kim.runOperation("-TemperatureStep") }
    shouldThrow<DeadEndException> { kim.runOperation("-OxygenStep") }
    shouldThrow<DeadEndException> { kim.runOperation("-OceanTile<Tharsis_1_2>") }
    shouldThrow<DeadEndException> { kim.runOperation("-VenusStep") }
    kim.count("TemperatureStep") shouldBe 19
    kim.count("OxygenStep") shouldBe 14
    kim.count("OceanTile") shouldBe 9
    kim.count("VenusStep") shouldBe 15
  }

  @Test
  internal fun extendedOceanTrackRecordsCompletionOnItsEleventhOcean() {
    newTestGame("AmazonisMap")

    agents.correct(
        "OceanTile<Amazonis_02_06>, OceanTile<Amazonis_02_07>, OceanTile<Amazonis_03_06>, " +
            "OceanTile<Amazonis_04_02>, OceanTile<Amazonis_05_05>, OceanTile<Amazonis_05_10>, " +
            "OceanTile<Amazonis_06_05>, OceanTile<Amazonis_06_06>, OceanTile<Amazonis_06_11>, OceanTile<Amazonis_07_04>"
    )
    kim.runOperation("OceanTile<Amazonis_02_01>") { doTask("TerraformRating") }

    kim.count("OceanTile") shouldBe 11
    kim.count("GpComplete<Class<OceanTile>>") shouldBe 1
  }
}
