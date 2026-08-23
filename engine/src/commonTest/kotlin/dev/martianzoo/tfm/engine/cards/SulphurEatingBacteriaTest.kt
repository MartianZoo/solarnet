package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SulphurEatingBacteriaTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(VenusNextExpansion)
    engine.phase("Action")
    p1.manual("$SulphurEatingBacteria, 4 Microbe<$SulphurEatingBacteria>")
  }

  @Test
  internal fun `Can add a microbe with its first action`() {
    p1.cardAction1(SulphurEatingBacteria).expect("Microbe")
  }

  @Test
  internal fun `Can convert three microbes into nine megacredits`() {
    p1.cardAction2(SulphurEatingBacteria) {
          doTask("-3 Microbe<$SulphurEatingBacteria> THEN 9")
        }
        .expect("-3 Microbe, 9")
  }

  @Test
  internal fun `Cannot take more than three megacredits per microbe`() {
    assertInvalidPayment<NarrowingException>("-Microbe<$SulphurEatingBacteria> THEN 4")
  }

  @Test
  internal fun `Cannot take fewer than three megacredits per microbe`() {
    assertInvalidPayment<NarrowingException>("-Microbe<$SulphurEatingBacteria> THEN 2")
  }

  @Test
  internal fun `Cannot remove microbes without taking their payment`() {
    assertInvalidPayment<NarrowingException>("-Microbe<$SulphurEatingBacteria>")
  }

  @Test
  internal fun `Must remove microbes from Sulphur-Eating Bacteria`() {
    assertInvalidPayment<NarrowingException>("-3 Microbe THEN 9")
  }

  @Test
  internal fun `Cannot spend more microbes than it has`() {
    assertInvalidPayment<LimitsException>("-5 Microbe<$SulphurEatingBacteria> THEN 15")
  }

  @Test
  internal fun `Cannot spend zero microbes`() {
    assertInvalidPayment<PetSyntaxException>("-0 Microbe<$SulphurEatingBacteria> THEN 0")
  }

  @Test
  internal fun `Must spend microbes rather than an abstract resource`() {
    assertInvalidPayment<ExpressionException>("-3 Resource<$SulphurEatingBacteria> THEN 9")
  }

  @Test
  internal fun `Must remove microbes before taking payment`() {
    assertInvalidPayment<NarrowingException>("9 THEN -3 Microbe<$SulphurEatingBacteria>")
  }

  @Test
  internal fun `Cannot add microbes in exchange for megacredits`() {
    assertInvalidPayment<NarrowingException>("2 Microbe<$SulphurEatingBacteria> THEN -6")
  }

  @Test
  internal fun `Can convert one microbe into three megacredits`() {
    p1.cardAction2(SulphurEatingBacteria) {
          doTask("-Microbe<$SulphurEatingBacteria> THEN 3")
        }
        .expect("-Microbe, 3")
  }

  @Test
  internal fun `Can convert all four microbes into twelve megacredits`() {
    p1.cardAction2(SulphurEatingBacteria) {
          doTask("-4 Microbe<$SulphurEatingBacteria> THEN 12")
        }
        .expect("-4 Microbe, 12")
  }

  private inline fun <reified T : Throwable> assertInvalidPayment(instruction: String) {
    p1.cardAction2(SulphurEatingBacteria) {
      shouldThrow<T> { doTask(instruction) }
      abort()
    }
  }
}
