package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class MarketManipulationTest : ColoniesCardTest() {
  @Test
  fun `Can raise one colony track and lower another`() {
    p1.manual("ProjectCard, Megacredit")
    p1.playProject(MarketManipulation, 1) {
          doTask("ColonyProduction<Luna> FROM ColonyProduction<Triton>")
        }
        .expect("ColonyProduction<Luna>, -ColonyProduction<Triton>")
  }

  @Test
  fun `Cannot lower a colony track already at its minimum`() {
    p1.manual("ProjectCard, Megacredit")
    engine.manual("-ColonyProduction<Triton>")
    p1.playProject(MarketManipulation, 1) {
      shouldThrow<LimitsException> {
        doTask("ColonyProduction<Luna> FROM ColonyProduction<Triton>")
      }
      abort()
    }
  }

  @Test
  fun `Cannot raise a maxed colony track`() {
    p1.manual("ProjectCard, Megacredit")
    engine.manual("5 ColonyProduction<Luna>")
    p1.playProject(MarketManipulation, 1) {
      shouldThrow<LimitsException> {
        doTask("ColonyProduction<Luna> FROM ColonyProduction<Triton>")
      }
      abort()
    }
  }

  @Test
  fun `Cannot select the same colony track twice`() {
    p1.manual("ProjectCard, Megacredit")
    p1.playProject(MarketManipulation, 1) {
      shouldThrow<ExpressionException> {
        doTask("ColonyProduction<Luna> FROM ColonyProduction<Luna>")
      }
      abort()
    }
  }

  @Test
  fun `Cannot raise Titan's delayed colony track`() {
    p1.manual("ProjectCard, Megacredit")
    p1.playProject(MarketManipulation, 1) {
      shouldThrow<NotNowException> {
        doTask("ColonyProduction<Titan> FROM ColonyProduction<Luna>")
      }
      abort()
    }
  }

  @Test
  fun `Cannot lower Titan's delayed colony track`() {
    p1.manual("ProjectCard, Megacredit")
    p1.playProject(MarketManipulation, 1) {
      shouldThrow<NotNowException> {
        doTask("ColonyProduction<Luna> FROM ColonyProduction<Titan>")
      }
      abort()
    }
  }
}
