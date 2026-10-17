package dev.martianzoo.tfm.web.playground

import dev.martianzoo.agent.Agents
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmEngine
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class EventVisibilityTest {
  @Test
  fun soloStockIsImaginaryButBoardTilesAndPlayerTransfersRemainVisible() {
    val world = TfmEngine.newGame(Canon.gamePremise(GameConfig("TharsisMap", "Red")))
    val admin = Agents(world)[ADMIN]
    val initial = world.events.changesSince(Checkpoint(0))
    val opponent = initial.single { it.change.gaining?.type == admin.resolve("SoloOpponent") }
    val reserve = initial.single {
      it.change.gaining?.type == admin.resolve("SoloCardResourceReserve<Class<Animal>>")
    }
    val heat = admin.sneak("2 Heat<SoloOpponent>").changes.single()
    val animal = admin.sneak("Animal<SoloCardResourceReserve<Class<Animal>>>").changes.single()
    val removal = admin.sneak("-Heat<SoloOpponent>").changes.single()
    val transfer = admin.sneak("Heat<Red> FROM Heat<SoloOpponent>").changes.single()
    val playerHeat = admin.sneak("Heat<Red>").changes.single()
    val city =
        admin.sneak("NormalCityTile<Tharsis_4_1, SoloOpponent>").changes.single {
          it.change.gaining?.type == admin.resolve("NormalCityTile<Tharsis_4_1, SoloOpponent>")
        }
    val greenery =
        admin.sneak("GreeneryTile<Tharsis_5_1, SoloOpponent>").changes.single {
          it.change.gaining?.type == admin.resolve("GreeneryTile<Tharsis_5_1, SoloOpponent>")
        }
    val changes = world.events.changesSince(Checkpoint(0))
    val displayed =
        liveSpecimen(
                "Visibility",
                world.reader,
                world.events,
                world.tasks,
                world.timeline.checkpoint().ordinal,
                changes,
                emptyMap(),
            )
            .events
            .associateBy { it.ordinal }

    for (event in listOf(opponent, reserve, heat, animal, removal)) {
      assertFalse(displayed.getValue(event.ordinal).iconic, event.change.toString())
    }
    assertTrue(displayed.getValue(transfer.ordinal).iconic)
    assertTrue(displayed.getValue(playerHeat.ordinal).iconic)
    assertTrue(displayed.getValue(city.ordinal).iconic)
    assertTrue(displayed.getValue(greenery.ordinal).iconic)
  }
}
