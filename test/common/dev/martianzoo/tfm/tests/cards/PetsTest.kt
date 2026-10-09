package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PetsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Cannot remove a protected animal when another target exists`() {
    stan.exMachina("$Pets, Animal<$Pets>")
    kim.exMachina("$Predators, Animal<$Predators>")

    shouldThrow<DeadEndException> {
      kim.cardAction1(Predators) { doTask("-Animal<Stan, $Pets<Stan>>") }
    }
  }

  @Test
  internal fun `Prevents Predators from acting when its animal is the only target`() {
    stan.exMachina("$Pets, Animal<$Pets>")
    kim.exMachina("$Predators")

    shouldThrow<DeadEndException> { kim.cardAction1(Predators) }
  }
}
