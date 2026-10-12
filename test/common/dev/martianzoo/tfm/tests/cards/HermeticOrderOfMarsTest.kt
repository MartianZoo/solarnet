package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class HermeticOrderOfMarsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Treats an area with a community but no tile as empty`() {
    kim.exMachina("NormalCityTile<Kim, Tharsis_1_1>")
    stan.exMachina(
        "OceanTile<Tharsis_1_2>, Community<Tharsis_2_1>, " + "NormalCityTile<Stan, Tharsis_2_2>"
    )

    kim.inTurn {
          doTasks(
              "PlayProject<Class<HermeticOrderOfMars>>",
              "-10 MC",
              "HermeticOrderOfMars FROM ProjectCard",
              "PROD[2 MC]",
          )
          selectOnlyTask()
        }
        .expect("-9 MC")
  }
}
