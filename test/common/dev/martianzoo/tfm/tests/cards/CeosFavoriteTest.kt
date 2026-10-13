package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CeosFavoriteTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  // FAQ: "This card can be played to add an additional resource to 'Search for Life'."
  @Test
  internal fun `Adds a resource to Search for Life`() {
    kim.exMachina("$SearchForLife, Science<$SearchForLife>")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<CeosFavoriteProject>>",
          "-1 MC",
          "CeosFavoriteProject FROM ProjectCard",
          "Science<SearchForLife>",
      )
      doTask("PlayedEvent<Class<CeosFavoriteProject>> FROM CeosFavoriteProject")
    }
  }

  // FAQ: "this card can still be played without effect."
  @Test
  internal fun `Can be played without an eligible resource-bearing card`() {
    kim.exMachina("$Tardigrades")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<CeosFavoriteProject>>",
              "-1 MC",
              "CeosFavoriteProject FROM ProjectCard",
          )
          selectTask("CardResource<CardFront(HAS CardResource)>*")
          doTask("PlayedEvent<Class<CeosFavoriteProject>> FROM CeosFavoriteProject")
        }
        .expect("0 Microbe<$Tardigrades>")
  }

  @Test
  internal fun `Cannot decline when an eligible resource exists`() {
    kim.exMachina("$SearchForLife, Science<$SearchForLife>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<CeosFavoriteProject>>")
        selectTask("-X? MC")
        doTasks("-1 MC", "CeosFavoriteProject FROM ProjectCard")
        selectTask("CardResource<CardFront(HAS CardResource)>*")
        doTask("Ok")
      }
    }
  }
}
