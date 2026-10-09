package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class VirusTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  // FAQ: "you must choose a single card from which to remove animals."
  @Test
  internal fun `Cannot split animal removal across two cards`() {
    stan.exMachina("$Birds, $Fish, Animal<$Birds>, Animal<$Fish>")

    shouldThrow<NarrowingException> {
      kim.playProject(Virus, 1) {
        doTask("-Animal<Stan, $Birds<Stan>>, -Animal<Stan, $Fish<Stan>>")
      }
    }
  }
}
