package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
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
              "1 Pay<> FROM MC",
              "MarketManipulation FROM ProjectCard",
          )
          // Hand the transfer to Admin, which updates both colony tracks.
          selectTask("ColonyProduction<Luna FROM Triton>!")
          doTask("PlayedEvent<Class<MarketManipulation>> FROM MarketManipulation")
        }
        .expect("ColonyProduction<Luna>, -ColonyProduction<Triton>")
  }

  @Test
  internal fun `Cannot lower a colony track already at its minimum`() {
    admin.setToExMachina(0, "ColonyProduction<Triton>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("PlayProject<Class<MarketManipulation>>")
        doTasks("1 Pay<> FROM MC", "MarketManipulation FROM ProjectCard")
        // Select the track transfer before testing an invalid pair of colonies.
        selectTask("ColonyProduction<ColonyTile FROM ColonyTile>!")
        narrowTask("ColonyProduction<Luna FROM Triton>")
      }
    }
  }

  @Test
  internal fun `Cannot raise a maxed colony track`() {
    admin.setToExMachina(6, "ColonyProduction<Luna>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("PlayProject<Class<MarketManipulation>>")
        doTasks("1 Pay<> FROM MC", "MarketManipulation FROM ProjectCard")
        // Select the track transfer before testing an invalid pair of colonies.
        selectTask("ColonyProduction<ColonyTile FROM ColonyTile>!")
        narrowTask("ColonyProduction<Luna FROM Triton>")
      }
    }
  }

  @Test
  internal fun `Cannot select the same colony track twice`() {
    shouldThrow<ExpressionException> {
      kim.inTurn {
        doTask("PlayProject<Class<MarketManipulation>>")
        doTasks("1 Pay<> FROM MC", "MarketManipulation FROM ProjectCard")
        // Select the track transfer before testing an invalid pair of colonies.
        selectTask("ColonyProduction<ColonyTile FROM ColonyTile>!")
        narrowTask("ColonyProduction<Luna FROM Luna>")
      }
    }
  }

  @Test
  internal fun `Cannot raise Titan's delayed colony track`() {
    shouldThrow<DependencyException> {
      kim.inTurn {
        doTask("PlayProject<Class<MarketManipulation>>")
        doTasks("1 Pay<> FROM MC", "MarketManipulation FROM ProjectCard")
        // Select the track transfer before testing an invalid pair of colonies.
        selectTask("ColonyProduction<ColonyTile FROM ColonyTile>!")
        narrowTask("ColonyProduction<Titan FROM Luna>")
      }
    }
  }

  @Test
  internal fun `Cannot lower Titan's delayed colony track`() {
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("PlayProject<Class<MarketManipulation>>")
        doTasks("1 Pay<> FROM MC", "MarketManipulation FROM ProjectCard")
        // Select the track transfer before testing an invalid pair of colonies.
        selectTask("ColonyProduction<ColonyTile FROM ColonyTile>!")
        narrowTask("ColonyProduction<Luna FROM Titan>")
      }
    }
  }
}
