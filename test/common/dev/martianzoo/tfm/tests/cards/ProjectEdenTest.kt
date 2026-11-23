package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ProjectEdenTest : TfmSandboxTest() {
  @Test
  internal fun `Project Eden must reserve the only legal city space`() {
    newTestGame(addOptions = "PreludeExpansion, $ProjectEden")
    val city = "Tharsis_8_8"
    val greenery = "Tharsis_3_3"
    val blockingCity = "Tharsis_3_2"
    val filled =
        kim.list("LandArea")
            .map { it.className.toString() }
            .filterNot { it in setOf(city, greenery, blockingCity) }
    kim.exMachina(
        (listOf("NormalCityTile<$blockingCity>") + filled.map { "GreeneryTile<$it>" })
            .joinToString()
    )
    kim.setToExMachina(3, "ProjectCard")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$ProjectEden>>",
            "-3 ProjectCard<Hand>",
            "GreeneryTile<Tharsis_8_8>",
            "OxygenStep",
            "TerraformRating",
            "CityTile<Tharsis_3_3>",
        )
      }
    }
    kim.count("$ProjectEden") shouldBe 0
    kim.count("GreeneryTile<$city>") shouldBe 0
    kim.count("ProjectCard") shouldBe 3

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$ProjectEden>>",
              "-3 ProjectCard<Hand>",
              "CityTile<Tharsis_8_8>",
              "GreeneryTile<Tharsis_3_3>",
              "OxygenStep",
              "TerraformRating",
              "OceanTile<Tharsis_1_5>",
              "TerraformRating",
          )
        }
        .expect("-3 ProjectCard")
  }
}
