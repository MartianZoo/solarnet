package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class IndenturedWorkersTest : TfmSandboxTest() {
  @Test
  internal fun `Discounts the next project card played`() {
    newTestGame()
    kim.setToExMachina(27, "MC")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<IndenturedWorkers>>",
          "IndenturedWorkers FROM ProjectCard",
      )
      doTask("IndenturedWorkers_NextCardEffect")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<Soletta>>", "27 Pay<> FROM MC")
      doTasks("Soletta FROM ProjectCard", "PROD[7 Heat]")
    }
  }

  @Test
  internal fun `Keeps its discount available through other actions`() {
    newTestGame()
    kim.setToExMachina(39, "MC")
    kim.setToExMachina(8, "Heat")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<IndenturedWorkers>>",
          "IndenturedWorkers FROM ProjectCard",
      )
      doTask("IndenturedWorkers_NextCardEffect")
    }
    kim.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "14 Pay<> FROM MC")
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }
    kim.inTurn {
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat")
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }
    kim.inTurn {
      doTasks("UseStandardProject<SellPatentsProject>", "2! MC FROM ProjectCard")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<Soletta>>", "27 Pay<> FROM MC")
      doTasks("Soletta FROM ProjectCard", "PROD[7 Heat]")
    }
  }

  @Test
  internal fun `Discounts only one project card`() {
    newTestGame()
    kim.setToExMachina(36, "MC")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<IndenturedWorkers>>",
          "IndenturedWorkers FROM ProjectCard",
      )
      doTask("IndenturedWorkers_NextCardEffect")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<Soletta>>", "27 Pay<> FROM MC")
      doTasks("Soletta FROM ProjectCard", "PROD[7 Heat]")
    }
    kim.inTurn {
      doTasks(
          "PlayProject<Class<AdvancedAlloys>>",
          "9 Pay<> FROM MC",
          "AdvancedAlloys FROM ProjectCard",
      )
    }
  }

  @Test
  internal fun `Expires at the end of the generation`() {
    newTestGame()

    kim.inTurn {
      doTasks(
          "PlayProject<Class<IndenturedWorkers>>",
          "IndenturedWorkers FROM ProjectCard",
      )
      doTask("IndenturedWorkers_NextCardEffect")
    }
    advanceUntilActionPhase()
    kim.setToExMachina(35, "MC")

    kim.inTurn {
      doTasks("PlayProject<Class<Soletta>>", "35 Pay<> FROM MC")
      doTasks("Soletta FROM ProjectCard", "PROD[7 Heat]")
    }
  }

  // Unresolved adjudication (question 11, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3775830/article/48238727#48238727
  @Test
  internal fun `Intervening Prelude leaves the discount for the next project card`() {
    newTestGame(addOptions = "PreludeExpansion, prelude2:cards")
    advanceUntilActionPhase()
    kim.exMachina("$BoardOfDirectors, Director<$BoardOfDirectors>")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<IndenturedWorkers>>",
          "IndenturedWorkers FROM ProjectCard",
      )
      doTask("IndenturedWorkers_NextCardEffect")
    }
    kim.inTurn {
      doTask("UseCardAction<BoardOfDirectors, Action1>")
      doTask("PreludeCard<Kim, Hand>")
      doTask("-12 MC")
      doTask("-Director<BoardOfDirectors>")
      doTasks("PlayCard<Class<PreludeCard>, Class<Donation>>", "Donation FROM PreludeCard", "21 MC")
    }

    kim.inTurn {
      doTasks("PlayProject<Class<Soletta>>", "27 Pay<> FROM MC")
      doTasks("Soletta FROM ProjectCard", "PROD[7 Heat]")
    }
  }
}
