package dev.martianzoo.tfm.web.playground

import dev.martianzoo.agent.Agents
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmEngine
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EventBalanceTest {
  @Test
  fun exactBalancesIncludeTransfersAndHiddenActorsAndSurviveSeeking() {
    val premise = Canon.gamePremise(GameConfig("TharsisMap", "Red", "Blue"))
    val world = TfmEngine.newGame(premise)
    val agents = Agents(world)
    val red = agents[Player(cn("Red"))]
    val blue = agents[Player(cn("Blue"))]
    val start = world.timeline.checkpoint()
    val gain = red.sneak("5 Heat").changes.single()
    val otherPlayer = blue.sneak("9 Heat").changes.single()
    red.sneak("3 Steel")
    val transfer = red.sneak("2 Steel FROM Heat").changes.single()
    val remove = red.sneak("-3 Heat").changes.single()
    agents[ADMIN].sneak("4 Heat<Red>")
    val laterGain = red.sneak("Heat").changes.single()
    val afterTransfer = red.sneak("Steel").changes.single()
    val signal =
        red.sneak("ConvertPlants").changes.single {
          it.change.gaining?.className == cn("ConvertPlants")
        }
    val changes = world.events.changesSince(Checkpoint(0))
    val history = world.events.entriesSince(Checkpoint(0))
    val measuredBalances = eventBalances(premise, history)
    val incremental = EventBalances(premise)
    incremental.extend(history.take(history.size / 2))
    assertEquals(measuredBalances, incremental.extend(history))
    assertEquals(false, incremental.canExtend(history.dropLast(1)))

    fun displayed() =
        liveSpecimen(
                "Balances",
                world.reader,
                world.events,
                world.tasks,
                world.timeline.checkpoint().ordinal,
                changes,
                measuredBalances,
            )
            .events
            .associate { it.ordinal to it.balance }

    val balances = displayed()
    assertEquals(5, balances[gain.ordinal])
    assertEquals(9, balances[otherPlayer.ordinal])
    assertEquals(5, balances[transfer.ordinal])
    assertEquals(0, balances[remove.ordinal])
    assertEquals(5, balances[laterGain.ordinal])
    assertEquals(6, balances[afterTransfer.ordinal])
    assertEquals(null, balances[signal.ordinal])

    world.timeline.seek(start)
    assertEquals(balances, displayed())
  }
}
