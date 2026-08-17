package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class MarketManipulationTest : ColoniesCardTest() {
  @Test
  fun `with movable tracks, plays Market Manipulation`() {
    p1.manual("ProjectCard, Megacredit")
    p1.playProject(MarketManipulation, 1) {
          doTask("ColonyProduction<Luna> FROM ColonyProduction<Triton>")
        }
        .expect("ColonyProduction<Luna>, -ColonyProduction<Triton>")
  }

  @Test
  fun `with a track at the bottom, tries to lower it`() {
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
  fun `with a track at the top, tries to raise it`() {
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
  fun `with one track selected twice, tries to play Market Manipulation`() {
    p1.manual("ProjectCard, Megacredit")
    p1.playProject(MarketManipulation, 1) {
      shouldThrow<ExpressionException> {
        doTask("ColonyProduction<Luna> FROM ColonyProduction<Luna>")
      }
      abort()
    }
  }

  @Test
  fun `with Titan delayed, tries to raise its track`() {
    p1.manual("ProjectCard, Megacredit")
    p1.playProject(MarketManipulation, 1) {
      shouldThrow<NotNowException> {
        doTask("ColonyProduction<Titan> FROM ColonyProduction<Luna>")
      }
      abort()
    }
  }

  @Test
  fun `with Titan delayed, tries to lower its track`() {
    p1.manual("ProjectCard, Megacredit")
    p1.playProject(MarketManipulation, 1) {
      shouldThrow<NotNowException> {
        doTask("ColonyProduction<Luna> FROM ColonyProduction<Titan>")
      }
      abort()
    }
  }
}
