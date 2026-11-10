package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.Agents
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.ForcedPrecipitation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class LegacyTfmTestSupportTest : LegacyTfmTestSupport {
  override lateinit var agents: Agents
    private set

  override val game
    get() = agents.world

  private val kim
    get() = agents[PLAYER1]

  @BeforeTest
  fun initializeGame() {
    agents = Agents(setUpGame(canonicalPremise(VenusNextExpansion)))
  }

  @Test
  internal fun `Tile placement accepts identical pending placements`() {

    kim.addTasks("OceanTile<WaterArea>, OceanTile<WaterArea>")

    kim.placeTile(1, 2)

    kim.count("OceanTile") shouldBe 1
  }

  @Test
  internal fun `Tile placement rejects distinct pending placements`() {

    kim.addTasks("OceanTile<WaterArea>, GreeneryTile<LandArea>")

    shouldThrow<IllegalArgumentException> { kim.placeTile(1, 2) }
  }

  @Test
  internal fun `Declining rejects multiple declinable tasks`() {

    kim.addTasks("Plant?, Steel?")

    shouldThrow<TaskException> { kim.declineTask() }
  }

  @Test
  internal fun `Card resources reject multiple pending placements`() {
    kim.runOperation("$ForcedPrecipitation")

    kim.addTasks("Floater?, Floater?")

    shouldThrow<IllegalArgumentException> { kim.addCardResources(ForcedPrecipitation) }
  }
}
