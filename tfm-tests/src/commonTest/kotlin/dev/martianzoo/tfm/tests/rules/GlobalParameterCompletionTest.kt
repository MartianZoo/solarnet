package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.*
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GlobalParameterCompletionTest {
  @Test
  internal fun eachTrackRecordsCompletionOnItsFinalStep() {
    val game = setUpGame(VenusNextExpansion)
    val p1 = game.tfm(PLAYER1)
    val waterAreas = p1.list("WaterArea")

    p1.godMode().sneak("18 TemperatureStep, 13 OxygenStep, 14 VenusStep")
    p1.godMode().sneak(waterAreas.take(8).joinToString { "OceanTile<$it>" })

    p1.godMode().manual("TemperatureStep, OxygenStep, VenusStep")

    p1.count("LastCall") shouldBe 0

    p1.godMode().manual("OceanTile<${waterAreas.elementAt(8)}>")

    p1.count("GpComplete<Class<TemperatureStep>>") shouldBe 1
    p1.count("GpComplete<Class<OxygenStep>>") shouldBe 1
    p1.count("GpComplete<Class<OceanTile>>") shouldBe 1
    p1.count("GpComplete<Class<VenusStep>>") shouldBe 1
    p1.count("LastCall") shouldBe 1
  }
}
