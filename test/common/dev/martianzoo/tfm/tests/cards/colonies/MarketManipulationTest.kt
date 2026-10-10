package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.MarketManipulation
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MarketManipulationTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Can raise one colony track and lower another`() {
    kim.playProject(MarketManipulation, 1) {
          selectTask("ColonyProduction(NOT Source@) FROM Source@ColonyProduction")
          narrowTask("ColonyProduction<Luna> FROM ColonyProduction<Triton>")
        }
        .expect("ColonyProduction<Luna>, -ColonyProduction<Triton>")
  }

  @Test
  internal fun `Cannot lower a colony track already at its minimum`() {
    admin.sneak("-ColonyProduction<Triton>")

    shouldThrow<LimitsException> {
      kim.playProject(MarketManipulation, 1) {
        selectTask("ColonyProduction(NOT Source@) FROM Source@ColonyProduction")
        narrowTask("ColonyProduction<Luna> FROM ColonyProduction<Triton>")
      }
    }
  }

  @Test
  internal fun `Cannot raise a maxed colony track`() {
    admin.sneak("5 ColonyProduction<Luna>")

    shouldThrow<LimitsException> {
      kim.playProject(MarketManipulation, 1) {
        selectTask("ColonyProduction(NOT Source@) FROM Source@ColonyProduction")
        narrowTask("ColonyProduction<Luna> FROM ColonyProduction<Triton>")
      }
    }
  }

  @Test
  internal fun `Cannot select the same colony track twice`() {
    shouldThrow<ExpressionException> {
      kim.playProject(MarketManipulation, 1) {
        selectTask("ColonyProduction(NOT Source@) FROM Source@ColonyProduction")
        narrowTask("ColonyProduction<Luna> FROM ColonyProduction<Luna>")
      }
    }
  }

  @Test
  internal fun `Cannot raise Titan's delayed colony track`() {
    shouldThrow<GameplayException> {
      kim.playProject(MarketManipulation, 1) {
        selectTask("ColonyProduction(NOT Source@) FROM Source@ColonyProduction")
        narrowTask("ColonyProduction<Titan> FROM ColonyProduction<Luna>")
      }
    }
  }

  @Test
  internal fun `Cannot lower Titan's delayed colony track`() {
    shouldThrow<GameplayException> {
      kim.playProject(MarketManipulation, 1) {
        selectTask("ColonyProduction(NOT Source@) FROM Source@ColonyProduction")
        narrowTask("ColonyProduction<Luna> FROM ColonyProduction<Titan>")
      }
    }
  }
}
