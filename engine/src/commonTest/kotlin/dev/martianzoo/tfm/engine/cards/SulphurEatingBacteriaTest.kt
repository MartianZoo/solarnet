package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class SulphurEatingBacteriaTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame("TerraformingMars,TharsisMapOption,VenusNextExpansion")
    engine.phase("Action")
    p1.manual("SulphurEatingBacteria, 4 Microbe<SulphurEatingBacteria>")
  }

  @Test
  fun `with Sulphur-Eating Bacteria, adds a microbe`() {
    p1.cardAction1("SulphurEatingBacteria").expect("Microbe")
  }

  @Test
  fun `with four microbes, uses Sulphur-Eating Bacteria`() {
    p1.cardAction2("SulphurEatingBacteria") {
          doTask("-3 Microbe<SulphurEatingBacteria> THEN 9")
        }
        .expect("-3 Microbe, 9")
  }

  @Test
  fun `with four microbes, tries to take too much payment`() {
    assertInvalidPayment<NarrowingException>("-Microbe<SulphurEatingBacteria> THEN 4")
  }

  @Test
  fun `with four microbes, tries to take too little payment`() {
    assertInvalidPayment<NarrowingException>("-Microbe<SulphurEatingBacteria> THEN 2")
  }

  @Test
  fun `with four microbes, tries to take income without payment`() {
    assertInvalidPayment<NarrowingException>("-Microbe<SulphurEatingBacteria>")
  }

  @Test
  fun `with four microbes, tries an unspecified-card payment`() {
    assertInvalidPayment<NarrowingException>("-3 Microbe THEN 9")
  }

  @Test
  fun `with four microbes, tries to spend five`() {
    assertInvalidPayment<LimitsException>("-5 Microbe<SulphurEatingBacteria> THEN 15")
  }

  @Test
  fun `with four microbes, tries to spend zero`() {
    assertInvalidPayment<PetSyntaxException>("-0 Microbe<SulphurEatingBacteria> THEN 0")
  }

  @Test
  fun `with four microbes, tries an unspecified-resource payment`() {
    assertInvalidPayment<ExpressionException>("-3 Resource<SulphurEatingBacteria> THEN 9")
  }

  @Test
  fun `with four microbes, tries to take income before paying`() {
    assertInvalidPayment<NarrowingException>("9 THEN -3 Microbe<SulphurEatingBacteria>")
  }

  @Test
  fun `with four microbes, tries to add microbes for a cost`() {
    assertInvalidPayment<NarrowingException>("2 Microbe<SulphurEatingBacteria> THEN -6")
  }

  private inline fun <reified T : Throwable> assertInvalidPayment(instruction: String) {
    p1.cardAction2("SulphurEatingBacteria") {
      shouldThrow<T> { doTask(instruction) }
      abort()
    }
  }
}
