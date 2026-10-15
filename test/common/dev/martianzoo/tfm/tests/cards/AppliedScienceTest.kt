package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class AppliedScienceTest : TfmSandboxTest() {
  @Test
  internal fun `Cannot add a resource to another players stocked card`() {
    newTestGame("AppliedScience")
    kim.exMachina("AppliedScience, Science<AppliedScience>")
    stan.exMachina("Pets, Animal<Pets>")
    kim.inTurn {
          doTasks("UseCardAction<AppliedScience, Action1>", "-Science<AppliedScience>")
          shouldThrow<TaskException> { doTask("Animal<Pets<Stan>>") }
          doTask("Steel")
        }
        .expect("0 Animal<Pets<Stan>>")
  }

  // Unresolved adjudication (question 13, 2026-10-14): may an uncollectible resource be chosen?
  // https://boardgamegeek.com/thread/1650981/article/30704489#30704489
  @Test
  internal fun `Can spend the last science to receive nothing instead of a standard resource`() {
    newTestGame(addOptions = "AppliedScience")
    kim.exMachina("$AppliedScience, Science<$AppliedScience>")

    kim.inTurn {
          doTasks("UseCardAction<AppliedScience, Action1>", "-Science<AppliedScience>")
          // No card holds resources now; the uncollectible card-resource option resolves to Ok.
          doTask("Ok")
        }
        .expect("0 Animal, 0 StandardResource")
  }

  @Test
  internal fun `Can replenish an existing director`() {
    newTestGame(addOptions = "AppliedScience, BoardOfDirectors")
    kim.exMachina(
        "$AppliedScience, Science<$AppliedScience>, $BoardOfDirectors, Director<$BoardOfDirectors>"
    )

    kim.inTurn {
          doTask("UseCardAction<AppliedScience, Action1>")
          doTask("-Science<AppliedScience>")
          doTask("Director<BoardOfDirectors>*")
        }
        .expect("Director<$BoardOfDirectors>")
  }

  @Test
  internal fun `Cannot replenish an empty board`() {
    newTestGame(addOptions = "AppliedScience, BoardOfDirectors")
    kim.exMachina("$AppliedScience, Science<$AppliedScience>, $BoardOfDirectors")

    kim.inTurn {
          doTasks("UseCardAction<AppliedScience, Action1>", "-Science<AppliedScience>")
          shouldThrow<NarrowingException> { doTask("Director<$BoardOfDirectors>") }
          doTask("Steel")
        }
        .expect("0 Director<$BoardOfDirectors>")
  }

  @Test
  internal fun `Spending the last science prevents replenishing that card`() {
    newTestGame(addOptions = "AppliedScience")
    kim.exMachina("$AppliedScience, Science<$AppliedScience>")

    kim.inTurn {
      doTasks("UseCardAction<AppliedScience, Action1>", "-Science<AppliedScience>")
      shouldThrow<NarrowingException> { doTask("Science<$AppliedScience>") }
      doTask("Steel")
    }
  }
}
