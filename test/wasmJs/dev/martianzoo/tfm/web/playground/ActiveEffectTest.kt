package dev.martianzoo.tfm.web.playground

import dev.martianzoo.agent.Agents
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ActiveEffectTest {
  @Test
  fun iconicEffectsInspectWatchedAndChangedRootsOnly() {
    val table = Canon.gamePremise(GameConfig("TharsisMap", "Red")).classTable
    fun iconic(text: String) = effectIsIconic(Parsing.parse<Effect>(text), table)

    assertTrue(iconic("MC: Ok"))
    assertTrue(iconic("-MC: Ok"))
    assertTrue(iconic("ProductionPhase: Ok"))
    assertTrue(iconic("PayingFor<Class<MC>> OR MC: Ok"))
    assertTrue(iconic("PayingFor<Class<MC>>: ProdOffset<Class<MC>>, MC"))
    assertTrue(iconic("PayingFor<Class<MC>>: -MC"))
    assertTrue(iconic("PayingFor<Class<MC>>: ProdOffset<Class<MC>> FROM MC"))
    assertFalse(iconic("PayingFor<Class<MC>>: Ok"))
    assertFalse(iconic("PayingFor<Class<MC>>: ProdOffset<Class<MC>>"))
    assertFalse(iconic("PayingFor<Class<MC>> IF MC: ProdOffset<Class<MC>>"))
    assertFalse(iconic("PayingFor<Class<MC>>: ProdOffset<Class<MC>> / MC"))
  }

  @Test
  fun subscriptionsFollowPresentCardsAndKeepTheirOwnersAndSources() {
    val world =
        TfmEngine.newGame(
            Canon.gamePremise(
                GameConfig("TharsisMap, Pets, RoverConstruction, ImmigrantCity", "Red", "Blue")
            )
        )
    val agents = Agents(world)
    val red = agents[Player(cn("Red"))]
    val blue = agents[Player(cn("Blue"))]
    fun displayed(): List<ActiveEffect> =
        activeEffects(
            world.classTable,
            liveSpecimen(
                "Effects",
                world.reader,
                world.events,
                world.tasks,
                world.timeline.checkpoint().ordinal,
                emptyList(),
                emptyMap(),
            ),
        )

    assertFalse(displayed().any { it.context.className == cn("Pets") })
    red.sneak("Pets, RoverConstruction")
    blue.sneak("ImmigrantCity")
    val cards =
        displayed().filter {
          it.context.className in setOf(cn("Pets"), cn("RoverConstruction"), cn("ImmigrantCity"))
        }
    val cities = cards.filter { it.effect.trigger.toString().startsWith("CityTile") }
    assertEquals(3, cities.size)
    assertEquals(1, cities.groupBy { it.effect.trigger }.size)
    assertEquals(2, cities.count { it.context.owningPlayer == Player(cn("Red")) })
    assertTrue(cards.any { it.effect.instruction.toString().contains("Animal<Pets<Red>>") })
    assertTrue(cards.none { it.effect.trigger.toString().contains("UseAction") })
    assertTrue(cards.none { it.effect.trigger.toString().contains("This") })
    assertTrue(cards.none { it.effect.trigger.toString().contains("Billing") })
    assertTrue(cards.none { it.effect.trigger.toString() == "End" })
    red.sneak("-RoverConstruction")
    assertFalse(displayed().any { it.context.toString() == "RoverConstruction<Red>" })
    blue.sneak("RoverConstruction")
    assertTrue(displayed().any { it.context.toString() == "RoverConstruction<Blue>" })
  }

  @Test
  fun bootstrapFilterHidesStartingEffectsButKeepsLaterCardEffects() {
    val session = PlaySession(Canon.gamePremise(GameConfig("TharsisMap", "Red")), 42)
    val world = session.world
    val baseline = bootstrapEffects(session.premise, world.events.entriesSince(Checkpoint(0)))
    assertTrue(baseline.isNotEmpty())
    val red = Agents(world)[Player(cn("Red"))]
    red.sneak("RoverConstruction")
    val current =
        activeEffects(
            world.classTable,
            liveSpecimen(
                "Effects",
                world.reader,
                world.events,
                world.tasks,
                world.timeline.checkpoint().ordinal,
                emptyList(),
                emptyMap(),
            ),
        )
    assertTrue(current.any { it.effect in baseline })
    val visible = current.filterNot { it.effect in baseline }
    assertTrue(visible.any { it.context.className == cn("RoverConstruction") })
    assertFalse(visible.any { it.context.className == cn("Red") })
    red.sneak("-RoverConstruction")
    assertEquals(
        baseline,
        bootstrapEffects(session.premise, world.events.entriesSince(Checkpoint(0))),
    )
  }

  @Test
  fun tabsReflectTheShownGamesClassComponents() {
    fun tabs(config: GameConfig): List<GlobalTab> {
      val world = TfmEngine.newGame(Canon.gamePremise(config))
      val specimen =
          liveSpecimen(
              "Tabs",
              world.reader,
              world.events,
              world.tasks,
              Checkpoint(0).ordinal,
              emptyList(),
              emptyMap(),
          )
      // Also exercise effect enumeration in solo and expansion games.
      assertTrue(activeEffects(world.classTable, specimen).isNotEmpty())
      return availableTabs(specimen, playing = false)
    }
    val solo = tabs(GameConfig("TharsisMap", "Red"))
    assertFalse(GlobalTab.Goals in solo)
    assertFalse(GlobalTab.Colonies in solo)
    assertFalse(GlobalTab.Turmoil in solo)
    assertTrue(GlobalTab.Effects in solo)
    val expanded =
        tabs(
            GameConfig(
                "TharsisMap, ColoniesExpansion, Ceres, TurmoilExpansion, Mayor, Landlord",
                "Red",
                "Blue",
            )
        )
    assertTrue(GlobalTab.Goals in expanded)
    assertTrue(GlobalTab.Colonies in expanded)
    assertTrue(GlobalTab.Turmoil in expanded)
  }
}
