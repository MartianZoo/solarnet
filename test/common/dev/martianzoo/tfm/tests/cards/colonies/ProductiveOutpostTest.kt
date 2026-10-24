package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ProductiveOutpostTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Pays no bonuses without colonies`() {
    kim.inTurn {
          doTasks(*playCardTasks("ProductiveOutpost"), "ProductiveOutpost FROM ProjectCard")
        }
        .expect("0 MC")
  }

  @Test
  internal fun `Pays each bonus for colonies the player owns`() {
    kim.exMachina("Colony<Luna>, Colony<Io>, Colony<Triton>")

    kim.inTurn {
      doTasks(
          *playCardTasks("ProductiveOutpost"),
          "ProductiveOutpost FROM ProjectCard",
          "2 Heat",
          "2 MC",
          "Titanium",
      )
    }
  }

  @Test
  internal fun `Pays once per colony, not once per colony tile`() {
    kim.exMachina("2 Colony<Luna>")

    kim.inTurn {
      doTasks(
          *playCardTasks("ProductiveOutpost"),
          "ProductiveOutpost FROM ProjectCard",
          "2 MC",
          "2 MC",
      )
    }
  }
}
