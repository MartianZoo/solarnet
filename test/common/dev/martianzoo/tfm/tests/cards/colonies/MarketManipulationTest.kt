package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MarketManipulationTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Can raise one colony track and lower another`() {
    kim.inTurn {
          doTasks(
              "PlayProject<Class<MarketManipulation>>",
              "-1 MC",
              "MarketManipulation FROM ProjectCard",
          )
          selectTask("ColonyProduction<ColonyTile FROM ColonyTile>!")
          selectTask("ColonyProduction<Luna FROM Triton>!")
          doTask("PlayedEvent<Class<MarketManipulation>> FROM MarketManipulation")
        }
        .expect("ColonyProduction<Luna>, -ColonyProduction<Triton>")
  }

  @Test
  internal fun `Cannot lower a colony track already at its minimum`() {
    admin.sneak("-ColonyProduction<Triton>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectTask("StandardAction OR Pass")
        doTask("PlayProject<Class<MarketManipulation>>")
        selectTask("-X MC?")
        doTasks("-1 MC", "MarketManipulation FROM ProjectCard")
        selectTask("ColonyProduction<ColonyTile FROM ColonyTile>!")
        narrowTask("ColonyProduction<Luna FROM Triton>")
      }
    }
  }

  @Test
  internal fun `Cannot raise a maxed colony track`() {
    admin.sneak("5 ColonyProduction<Luna>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectTask("StandardAction OR Pass")
        doTask("PlayProject<Class<MarketManipulation>>")
        selectTask("-X MC?")
        doTasks("-1 MC", "MarketManipulation FROM ProjectCard")
        selectTask("ColonyProduction<ColonyTile FROM ColonyTile>!")
        narrowTask("ColonyProduction<Luna FROM Triton>")
      }
    }
  }

  @Test
  internal fun `Cannot select the same colony track twice`() {
    shouldThrow<ExpressionException> {
      kim.inTurn {
        selectTask("StandardAction OR Pass")
        doTask("PlayProject<Class<MarketManipulation>>")
        selectTask("-X MC?")
        doTasks("-1 MC", "MarketManipulation FROM ProjectCard")
        selectTask("ColonyProduction<ColonyTile FROM ColonyTile>!")
        narrowTask("ColonyProduction<Luna FROM Luna>")
      }
    }
  }

  @Test
  internal fun `Cannot raise Titan's delayed colony track`() {
    shouldThrow<GameplayException> {
      kim.inTurn {
        selectTask("StandardAction OR Pass")
        doTask("PlayProject<Class<MarketManipulation>>")
        selectTask("-X MC?")
        doTasks("-1 MC", "MarketManipulation FROM ProjectCard")
        selectTask("ColonyProduction<ColonyTile FROM ColonyTile>!")
        narrowTask("ColonyProduction<Titan FROM Luna>")
      }
    }
  }

  @Test
  internal fun `Cannot lower Titan's delayed colony track`() {
    shouldThrow<GameplayException> {
      kim.inTurn {
        selectTask("StandardAction OR Pass")
        doTask("PlayProject<Class<MarketManipulation>>")
        selectTask("-X MC?")
        doTasks("-1 MC", "MarketManipulation FROM ProjectCard")
        selectTask("ColonyProduction<ColonyTile FROM ColonyTile>!")
        narrowTask("ColonyProduction<Luna FROM Titan>")
      }
    }
  }
}
