package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class FakeAppliedScienceTest : TfmSandboxTest() {
  @Test
  internal fun `Can replenish an existing director`() {
    newTestGame(addOptions = "FakeStuffBundle, BoardOfDirectors")
    kim.exMachina(
        "$FakeAppliedScience, Science<$FakeAppliedScience>, $BoardOfDirectors, Director<$BoardOfDirectors>"
    )

    kim.cardAction1(FakeAppliedScience) { addCardResources(BoardOfDirectors) }
        .expect("Director<$BoardOfDirectors>")
  }

  @Test
  internal fun `Cannot replenish an empty board`() {
    newTestGame(addOptions = "FakeStuffBundle, BoardOfDirectors")
    kim.exMachina("$FakeAppliedScience, Science<$FakeAppliedScience>, $BoardOfDirectors")

    kim.cardAction1(FakeAppliedScience) {
          shouldThrow<NarrowingException> { doTask("Director<$BoardOfDirectors>") }
          doTask("Steel")
        }
        .expect("Steel, 0 Director<$BoardOfDirectors>")
  }

  @Test
  internal fun `Spending the last science leaves only the standard resource option`() {
    newTestGame(addOptions = "FakeStuffBundle")
    kim.exMachina("$FakeAppliedScience, Science<$FakeAppliedScience>")

    kim.cardAction1(FakeAppliedScience) {
          shouldThrow<NarrowingException> { doTask("Science<$FakeAppliedScience>") }
          doTask("Steel")
        }
        .expect("-Science<$FakeAppliedScience>, Steel")
  }
}
