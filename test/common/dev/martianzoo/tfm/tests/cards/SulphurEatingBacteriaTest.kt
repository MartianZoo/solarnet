package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SulphurEatingBacteriaTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(VenusNextExpansion)
    p1.autoExecPolicy = NONE
    admin.phase("Action")
    p1.runOperation("$SulphurEatingBacteria, 4 Microbe<$SulphurEatingBacteria>")
  }

  @Test
  internal fun `Can add a microbe with its first action`() {
    p1.cardAction1(SulphurEatingBacteria) {
          doTask("Microbe<$SulphurEatingBacteria>")
        }
        .expect("Microbe, 0 MC")
  }

  @Test
  internal fun `Can convert 3 microbes into 9 mc`() {
    p1.cardAction2(SulphurEatingBacteria) {
          doTask("-3 Microbe<$SulphurEatingBacteria>")
          doTask("9 MC")
        }
        .expect("-3 Microbe, 9 MC")
  }

  @Test
  internal fun `Can convert 3 microbes into 9 mc by setting X`() {
    p1.cardAction2(SulphurEatingBacteria, x = 3) {
          doTask("-3 Microbe<$SulphurEatingBacteria>")
          doTask("9 MC")
        }
        .expect("-3 Microbe, 9 MC")
  }

  @Test
  internal fun `Can convert 1 microbe into 3 mc`() {
    p1.cardAction2(SulphurEatingBacteria) {
          doTask("-Microbe<$SulphurEatingBacteria>")
          doTask("3 MC")
        }
        .expect("-Microbe, 3 MC")
  }

  @Test
  internal fun `Can convert 1 microbe into 3 mc by setting X`() {
    p1.cardAction2(SulphurEatingBacteria, x = 1) {
          doTask("-Microbe<$SulphurEatingBacteria>")
          doTask("3 MC")
        }
        .expect("-Microbe, 3 MC")
  }

  @Test
  internal fun `Can convert all 4 microbes into 12 mc`() {
    p1.cardAction2(SulphurEatingBacteria) {
          doTask("-4 Microbe<$SulphurEatingBacteria>")
          doTask("12 MC")
        }
        .expect("-4 Microbe, 12 MC")
  }

  @Test
  internal fun `Can convert all 4 microbes into 12 mc by setting X`() {
    p1.cardAction2(SulphurEatingBacteria, x = 4) {
          doTask("-4 Microbe<$SulphurEatingBacteria>")
          doTask("12 MC")
        }
        .expect("-4 Microbe, 12 MC")
  }

  @Test
  internal fun `Cannot take more than three mc per microbe`() {
    assertInvalidPayment<NarrowingException>("-Microbe<$SulphurEatingBacteria> THEN 4 MC")
  }

  @Test
  internal fun `Cannot take fewer than three mc per microbe`() {
    assertInvalidPayment<NarrowingException>("-Microbe<$SulphurEatingBacteria> THEN 2 MC")
  }

  @Test
  internal fun `Cannot remove microbes without taking their payment`() {
    shouldThrow<TaskException> {
      p1.cardAction2(SulphurEatingBacteria) { doTask("-Microbe<$SulphurEatingBacteria>") }
    }
  }

  @Test
  internal fun `Cannot remove microbes without taking their payment after setting X`() {
    shouldThrow<TaskException> {
      p1.cardAction2(SulphurEatingBacteria, x = 1) { doTask("-Microbe<$SulphurEatingBacteria>") }
    }
  }

  @Test
  internal fun `Cannot spend more microbes than it has`() {
    assertInvalidPayment<LimitsException>("-5 Microbe<$SulphurEatingBacteria> THEN 15 MC")
  }

  @Test
  internal fun `Can set X higher than its microbe count but cannot execute the resulting task`() {
    shouldThrow<LimitsException> {
      p1.cardAction2(SulphurEatingBacteria, x = 5) {
        doTask("-5 Microbe<$SulphurEatingBacteria>")
      }
    }
    p1.assertCounts(4 to "Microbe<$SulphurEatingBacteria>")
  }

  @Test
  internal fun `Cannot spend zero microbes`() {
    assertInvalidPayment<PetSyntaxException>("-0 Microbe<$SulphurEatingBacteria>")
  }

  @Test
  internal fun `Cannot set X to zero`() {
    shouldThrow<IllegalArgumentException> { p1.cardAction2(SulphurEatingBacteria, x = 0) }
    p1.assertCounts(4 to "Microbe<$SulphurEatingBacteria>")
  }

  @Test
  internal fun `Cannot set X to a negative value`() {
    shouldThrow<IllegalArgumentException> { p1.cardAction2(SulphurEatingBacteria, x = -1) }
    p1.assertCounts(4 to "Microbe<$SulphurEatingBacteria>")
  }

  @Test
  internal fun `Must spend microbes rather than an abstract resource`() {
    assertInvalidPayment<ExpressionException>("-3 Resource<$SulphurEatingBacteria> THEN 9 MC")
  }

  @Test
  internal fun `Must remove microbes from Sulphur-Eating Bacteria`() {
    assertInvalidPayment<NarrowingException>("-3 Microbe THEN 9 MC")
  }

  @Test
  internal fun `Cannot add microbes in exchange for mc`() {
    assertInvalidPayment<NarrowingException>("2 Microbe<$SulphurEatingBacteria> THEN -6 MC")
  }

  private inline fun <reified T : Throwable> assertInvalidPayment(instruction: String) {
    p1.cardAction2(SulphurEatingBacteria) {
      p1.selectTask("-X Microbe<$SulphurEatingBacteria> THEN 3X MC")
      shouldThrow<T> { p1.narrowTask(instruction) }
      abort()
    }
  }
}
