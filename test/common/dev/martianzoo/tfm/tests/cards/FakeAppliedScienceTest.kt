package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.AlliedBank
import dev.martianzoo.tfm.tests.cards.cardnames.Biolab
import dev.martianzoo.tfm.tests.cards.cardnames.BoardOfDirectors
import dev.martianzoo.tfm.tests.cards.cardnames.CrediCor
import dev.martianzoo.tfm.tests.cards.cardnames.DomeFarming
import dev.martianzoo.tfm.tests.cards.cardnames.Donation
import dev.martianzoo.tfm.tests.cards.cardnames.FakeAppliedScience
import dev.martianzoo.tfm.tests.cards.cardnames.PowerGeneration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FakeAppliedScienceTest : CardTest() {

  @Test
  internal fun `Fake Applied Science replenishes an existing director but not an empty board`() {
    newGame(PreludeExpansion, Prelude2CardPack, FakeStuffBundle)
    p1.playCorp(CrediCor, 0)
    admin.phase("Prelude")
    p1.playPrelude(FakeAppliedScience)
    p1.playPrelude(BoardOfDirectors)
    admin.phase("Action")

    p1.cardAction1(FakeAppliedScience) { addCardResources(BoardOfDirectors) }
        .expect("Director<$BoardOfDirectors>")
    p1.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      p1.playPrelude(Donation)
    }
    admin.nextGeneration(0, 0)
    p1.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      p1.playPrelude(AlliedBank)
    }
    admin.nextGeneration(0, 0)
    p1.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      p1.playPrelude(PowerGeneration)
    }
    admin.nextGeneration(0, 0)
    p1.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      p1.playPrelude(Biolab)
    }
    admin.nextGeneration(0, 0)
    p1.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      p1.playPrelude(DomeFarming)
    }
    admin.nextGeneration(0, 0)
    p1.cardAction1(FakeAppliedScience) {
      shouldThrow<NarrowingException> { doTask("Director<$BoardOfDirectors>") }
      doTask("Steel")
    }
    p1.count("Director<$BoardOfDirectors>") shouldBe 0
  }

  @Test
  internal fun `Spending the last science leaves only the standard resource option`() {
    newGame(PreludeExpansion, Prelude2CardPack, FakeStuffBundle)
    p1.playCorp(CrediCor, 0)
    admin.phase("Prelude")
    p1.playPrelude(FakeAppliedScience)
    admin.phase("Action")
    repeat(5) {
      p1.cardAction1(FakeAppliedScience) { doTask("Steel") }
      admin.runOperation("Generation")
    }
    p1.count("Science<$FakeAppliedScience>") shouldBe 1

    p1.cardAction1(FakeAppliedScience) {
          shouldThrow<NarrowingException> {
            doTask("Science<$FakeAppliedScience>")
          }
          doTask("Steel")
        }
        .expect("-Science<$FakeAppliedScience>, Steel")
  }
}
