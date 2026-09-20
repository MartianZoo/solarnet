package dev.martianzoo.tfm.web.gameviewer

import dev.martianzoo.agent.Agents
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.toComponent
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.isVisibleInLog as isVisibleInEngineLog
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GameQueriesParityTest {
  @Test
  internal fun corporationImageClassificationIncludesBothCorporationBacks() {
    val game = Engine.newGame(Canon.gamePremise(GameConfig("BeginnerVariant", "Player1")))

    mapOf(
            "CrediCor" to "corporations",
            "BeginnerCorporation1" to "corporations",
            "EarthCatapult" to "projects",
        )
        .forEach { (cardName, directory) ->
          val card = game.reader.resolve(parse<Expression>(cardName))
          assertEquals(directory, cardImageDirectory(card), cardName)
        }
  }

  @Test
  internal fun passiveActorQueriesMatchTheAgentAndTfmGameplay() {
    val game = Engine.newGame(Canon.gamePremise(GameConfig("", "Player1")))
    val agents = Agents(game)
    val player = game.actors.filterIsInstance<Player>().single()
    val agent = agents[player]
    val queries = GameQueries(game.reader)

    agent.sneak("7 MC, 2 Plant, PROD[3 MC]")

    listOf("MC", "Plant", "PROD[MC]", "Class<MC>").forEach { metric ->
      assertEquals(agent.count(metric), queries.count(player, metric), metric)
    }
    assertEquals(agents.tfm(player).production(cn("MC")), queries.production(player, cn("MC")))
  }

  @Test
  internal fun visibleLogSelectionMatchesTheEngineRule() {
    val game = Engine.newGame(Canon.gamePremise(GameConfig("", "Player1")))

    fun event(ordinal: Int, expression: String): ChangeEvent =
        ChangeEvent(
            ordinal,
            ADMIN,
            ComponentChange.Gain(
                component = game.reader.resolve(parse<Expression>(expression)).toComponent()
            ),
            cause = null,
        )

    val events =
        listOf(
            event(0, "MC<Player1>"),
            event(1, "ActionPhase"),
            event(2, "GpIncomplete<Class<TemperatureStep>>"),
        )
    val engineSelection = events.filter { it.isVisibleInEngineLog(game.reader) }

    assertEquals(listOf(true, true, false), events.map { it in engineSelection })
    assertEquals(engineSelection, visibleLogEvents(events, game.reader))
  }
}
