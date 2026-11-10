package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GlobalParameterCompletionTest : TfmSandboxTest() {
  @Test
  internal fun atomizedGainStopsWhenTheTrackCompletes() {
    newTestGame()

    kim.runOperation("55 OxygenStep") { autoExecNow(EAGER) }

    kim.count("OxygenStep") shouldBe 14
    kim.count("GpComplete<Class<OxygenStep>>") shouldBe 1
  }

  @Test
  internal fun eachTrackRecordsCompletionOnItsFinalStep() {
    newTestGame()
    val waterAreas = kim.list("WaterArea")
    val landArea = kim.list("LandArea").first()

    kim.exMachina("18 TemperatureStep, 13 OxygenStep, 14 VenusStep")
    kim.exMachina(waterAreas.take(8).joinToString { "OceanTile<$it>" })

    kim.runOperation("TemperatureStep, OxygenStep, VenusStep") { autoExecNow(EAGER) }

    kim.count("GameEndBarrier") shouldBe 1

    kim.runOperation("OceanTile<${waterAreas.elementAt(8)}>") { autoExecNow(EAGER) }

    kim.count("GpComplete<Class<TemperatureStep>>") shouldBe 1
    kim.count("GpComplete<Class<OxygenStep>>") shouldBe 1
    kim.count("GpComplete<Class<OceanTile>>") shouldBe 1
    kim.count("GpComplete<Class<VenusStep>>") shouldBe 1
    kim.count("GpIncomplete") shouldBe 0
    kim.count("GameEndBarrier") shouldBe 0

    kim.runOperation("TemperatureStep, OceanTile<$landArea>") { autoExecNow(EAGER) }
    shouldThrow<DeadEndException> { kim.runOperation("-TemperatureStep") }
    shouldThrow<DeadEndException> { kim.runOperation("-OxygenStep") }
    shouldThrow<DeadEndException> { kim.runOperation("-OceanTile<${waterAreas.first()}>") }
    shouldThrow<DeadEndException> { kim.runOperation("-VenusStep") }
    kim.count("TemperatureStep") shouldBe 19
    kim.count("OxygenStep") shouldBe 14
    kim.count("OceanTile") shouldBe 9
    kim.count("VenusStep") shouldBe 15
  }

  @Test
  internal fun extendedOceanTrackRecordsCompletionOnItsEleventhOcean() {
    newTestGame("AmazonisMap")
    val waterAreas = kim.list("WaterArea")

    kim.exMachina(waterAreas.take(10).joinToString { "OceanTile<$it>" })
    kim.runOperation("OceanTile<${waterAreas.elementAt(10)}>") { autoExecNow(EAGER) }

    kim.count("OceanTile") shouldBe 11
    kim.count("GpComplete<Class<OceanTile>>") shouldBe 1
  }
}
