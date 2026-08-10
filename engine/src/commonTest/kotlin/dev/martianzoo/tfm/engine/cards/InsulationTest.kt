package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class InsulationTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    engine.phase("Action")
    p1.manual("2, ProjectCard, PROD[-1, 3 Heat]")
  }

  @Test
  fun `with heat production, plays Insulation`() {
    p1.playProject("Insulation", 2) { doTask("PROD[Megacredit FROM Heat]") }
        .expect("PROD[Megacredit, -Heat]")
  }

  @Test
  fun `with three heat production, converts two using Insulation`() {
    p1.playProject("Insulation", 2) { doTask("PROD[2 Megacredit FROM Heat]") }
        .expect("PROD[2 Megacredit, -2 Heat]")
  }

  @Test
  fun `with heat production, tries to convert none using Insulation`() {
    p1.playProject("Insulation", 2) {
      shouldThrow<PetSyntaxException> { doTask("PROD[0 Megacredit FROM Heat]") }
      abort()
    }
  }

  @Test
  fun `with heat production, tries a non-production conversion using Insulation`() {
    p1.playProject("Insulation", 2) {
      shouldThrow<PetSyntaxException> { doTask("PROD[Ok]") }
      abort()
    }
  }

  @Test
  fun `with heat production, tries to skip the Insulation conversion`() {
    p1.playProject("Insulation", 2) {
      shouldThrow<NarrowingException> { doTask("Ok") }
      abort()
    }
  }

  @Test
  fun `with heat production, tries to convert p2 production using Insulation`() {
    p1.playProject("Insulation", 2) {
      shouldThrow<NarrowingException> {
        doTask("PROD[2 Megacredit<Player2> FROM Heat<Player2>]")
      }
      abort()
    }
  }
}
