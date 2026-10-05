package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.AtmoCollectors
import dev.martianzoo.tfm.tests.cards.cardnames.JupiterFloatingStation
import dev.martianzoo.tfm.tests.cards.cardnames.TitanShuttles
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class JupiterFloatingStationTest : ColoniesCardTest() {
  @Test
  internal fun `First action adds a floater to another Jovian card`() {
    p1.runOperation("$JupiterFloatingStation, $TitanShuttles")

    p1.cardAction1(JupiterFloatingStation) { addCardResources(TitanShuttles) }
        .expect("Floater<$TitanShuttles>")
  }

  @Test
  internal fun `First action rejects a card without a Jovian tag`() {
    p1.runOperation("$JupiterFloatingStation, $AtmoCollectors") {
      addCardResources(AtmoCollectors)
    }

    p1.cardAction1(JupiterFloatingStation) {
      shouldThrow<NarrowingException> { doTask("Floater<$AtmoCollectors>") }
      abort()
    }
  }

  @Test
  internal fun `Second action pays at most four mc`() {
    p1.runOperation("$JupiterFloatingStation, 6 Floater<$JupiterFloatingStation>")

    p1.cardAction2(JupiterFloatingStation).expect("4 MC")
  }
}
