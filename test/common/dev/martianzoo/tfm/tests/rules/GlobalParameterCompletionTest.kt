package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GlobalParameterCompletionTest : TfmTest() {
  @Test
  internal fun atomizedGainStopsWhenTheTrackCompletes() {
    game = setUpGame()
    val p1 = game.tfm(PLAYER1)

    p1.runOperation("55 OxygenStep")

    p1.count("OxygenStep") shouldBe 14
    p1.count("GpComplete<Class<OxygenStep>>") shouldBe 1
  }

  @Test
  internal fun eachTrackRecordsCompletionOnItsFinalStep() {
    game = setUpGame(VenusNextExpansion)
    val p1 = game.tfm(PLAYER1)
    val waterAreas = p1.list("WaterArea")
    val landArea = p1.list("LandArea").first()

    p1.sneak("18 TemperatureStep, 13 OxygenStep, 14 VenusStep")
    p1.sneak(waterAreas.take(8).joinToString { "OceanTile<$it>" })

    p1.runOperation("TemperatureStep, OxygenStep, VenusStep")

    p1.count("GameEndBarrier") shouldBe 1

    p1.runOperation("OceanTile<${waterAreas.elementAt(8)}>")

    p1.count("GpComplete<Class<TemperatureStep>>") shouldBe 1
    p1.count("GpComplete<Class<OxygenStep>>") shouldBe 1
    p1.count("GpComplete<Class<OceanTile>>") shouldBe 1
    p1.count("GpComplete<Class<VenusStep>>") shouldBe 1
    p1.count("GpIncomplete") shouldBe 0
    p1.count("GameEndBarrier") shouldBe 0

    p1.runOperation("TemperatureStep, OceanTile<$landArea>")
    shouldThrow<DeadEndException> { p1.runOperation("-TemperatureStep") }
    shouldThrow<DeadEndException> { p1.runOperation("-OxygenStep") }
    shouldThrow<DeadEndException> { p1.runOperation("-OceanTile<${waterAreas.first()}>") }
    shouldThrow<DeadEndException> { p1.runOperation("-VenusStep") }
    p1.count("TemperatureStep") shouldBe 19
    p1.count("OxygenStep") shouldBe 14
    p1.count("OceanTile") shouldBe 9
    p1.count("VenusStep") shouldBe 15
  }
}
