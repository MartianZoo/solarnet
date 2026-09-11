package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agent
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

/** Tests for the Canon data set. */
internal class CanonClassesTest {
  companion object {
    private fun te(source: String): Expression = parse(source)
  }

  @Test
  internal fun setupSeparatesPlayersFromActors() {
    val premise = canonicalPremise()
    premise.actors
        .filterIsInstance<dev.martianzoo.pets.data.Player>()
        .shouldContainExactly(PLAYER1, PLAYER2)
    premise.actors.shouldContainExactly(PLAYER1, PLAYER2, ADMIN)
    val game = Engine.newGame(premise)
    game.classTable.allClassNames.shouldNotContain(cn("SoloMode"))
    game.classTable.allClassNames.shouldNotContain(cn("SoloOpponent"))
    game.classTable.allClassNames.shouldNotContain(cn("SoloStandardResourceReserve"))
    game.classTable.allClassNames.shouldNotContain(cn("SoloCardResourceReserve"))
    game.classTable.allClassNames.shouldNotContain(cn("PreludeCard"))
    game.classTable.allClassNames.shouldNotContain(cn("PreludePhase"))
  }

  @Test
  internal fun activeOwnedTileKindsInBroadCombinedGameExtendOwnedTile() {
    // Landlord counts `OwnedTile`, so a class that is both a `Tile` and `Owned` but forgets to
    // extend it would look structurally right and silently escape the award. Pets has no structural
    // conjunction to state that directly yet (see TODO.md), so this broad active projection checks
    // the nominal class until every legal configuration family can be covered systematically.
    val table =
        Engine.newGame(
                canonicalPremise(
                    CorporateEraExpansion,
                    Cimmeria,
                    VenusNextExpansion,
                    Prelude2Expansion,
                    ColoniesExpansion,
                    TurmoilCardPack,
                    PromoCardPack,
                    colonyTiles = testColonyTiles(players = 2),
                )
            )
            .classTable
    val tile = table.getClass(cn("Tile"))
    val owned = table.getClass(cn("Owned"))
    val ownedTile = table.getClass(cn("OwnedTile"))

    table
        .allClasses()
        .filter { it.isSubtypeOf(tile) && it.isSubtypeOf(owned) && !it.isSubtypeOf(ownedTile) }
        .map { "$it" }
        .shouldBeEmpty()
  }

  @Test
  internal fun everyMapOffersSixMilestonesAndAwardsWithVenusAndColonies() {
    val maps = listOf(Tharsis, Hellas, Elysium, Utopia, Cimmeria)

    maps.forEach { map ->
      val game =
          Engine.newGame(
              canonicalPremise(
                  map,
                  VenusNextExpansion,
                  ColoniesExpansion,
                  players = 2,
                  colonyTiles = testColonyTiles(2),
              )
          )
      val agent = game.testTfm(PLAYER1)

      withClue(map.name) {
        agent.count("Class<Milestone>") shouldBe 6
        agent.count("Class<Award>") shouldBe 6
      }
    }
  }

  @Test
  internal fun preludeSetupDealsTwoPreludeCardsToEachPlayer() {
    val game = setUpGame(canonicalPremise(PreludeExpansion, players = 2))

    game.testTfm(PLAYER1).phase("Prelude")

    game.testTfm(PLAYER1).count("PreludeCard<Player1>") shouldBe 2
    game.testTfm(PLAYER2).count("PreludeCard<Player2>") shouldBe 2
  }

  @Test
  internal fun soloSetupUsesPetsOnlyOpponent() {
    val premise = canonicalPremise(players = 1)
    premise.actors.shouldContainExactly(PLAYER1, ADMIN)
    val game = setUpGame(premise)
    game.classTable.allClassNames.shouldNotContain(cn("Player2"))
    game.reader.count(game.reader.resolve(te("SoloMode"))) shouldBe 1
    game.reader.count(game.reader.resolve(te("StandardSoloObjective"))) shouldBe 1
    game.reader.count(game.reader.resolve(te("SoloOpponent"))) shouldBe 1
    game.testAgent(PLAYER1).count("TerraformRating<Player1>") shouldBe 14
    listOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat").forEach {
      game.testAgent(PLAYER1).count("$it<SoloOpponent>") shouldBe 42
      game.testAgent(PLAYER1).count("PROD[$it<SoloOpponent>]") shouldBe 42
    }
    game.testAgent(PLAYER1).count("SoloStandardResourceReserve<SoloOpponent>") shouldBe
        game.testAgent(PLAYER1).count("Class<StandardResource>")
    game.testAgent(PLAYER1).count("SoloCardResourceReserve<SoloOpponent>") shouldBe
        game.testAgent(PLAYER1).count("Class<CardResource>")
    game.testAgent(PLAYER1).count("SoloCardResourceReserve<SoloOpponent, Class<Animal>>") shouldBe 1
    game
        .testAgent(PLAYER1)
        .count(
            "Animal<SoloOpponent, SoloCardResourceReserve<SoloOpponent, Class<Animal>>>"
        ) shouldBe 42
    val admin = game.testAgent(ADMIN) as Agent
    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    admin.runOperation("OceanTile<Tharsis_1_2>")
    game.testAgent(PLAYER1).count("CityTile<SoloOpponent>") shouldBe 2
    game.testAgent(PLAYER1).count("GreeneryTile<SoloOpponent>") shouldBe 2

    val player = game.testAgent(PLAYER1)
    player.runOperation("-5 Plant<SoloOpponent>")
    player.runOperation("PROD[-5 Plant<SoloOpponent>]")
    player.runOperation("5 Plant<SoloOpponent>")
    player.runOperation("PROD[5 Plant<SoloOpponent>]")
    player.runOperation(
        "-5 Animal<SoloOpponent, SoloCardResourceReserve<SoloOpponent, Class<Animal>>>"
    )
    player.runOperation(
        "5 Animal<SoloOpponent, SoloCardResourceReserve<SoloOpponent, Class<Animal>>>"
    )
    listOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat").forEach {
      game.testAgent(PLAYER1).count("$it<SoloOpponent>") shouldBe 42
      game.testAgent(PLAYER1).count("PROD[$it<SoloOpponent>]") shouldBe 42
      game.testAgent(PLAYER1).count("$it<Player1>") shouldBe 0
    }
    game
        .testAgent(PLAYER1)
        .count(
            "Animal<SoloOpponent, SoloCardResourceReserve<SoloOpponent, Class<Animal>>>"
        ) shouldBe 42

    admin.runOperation("End FROM Phase")
    game.testAgent(PLAYER1).count("VictoryPoint<Player1>") shouldBe 14
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun inactiveClassLiteralCountsZeroWhileUnknownClassLiteralIsInvalid() {
    val game = Engine.newGame(canonicalPremise())
    val agent = game.testAgent(PLAYER1) as Agent
    val withVenus =
        Engine.newGame(canonicalPremise(VenusNextExpansion, players = 2)).testAgent(PLAYER1)
            as Agent

    assertFailsWith<ExpressionException> { agent.count("Class<AnyWordHere>") }
    agent.count("Class<VenusStep>") shouldBe 0
    withVenus.count("Class<VenusStep>") shouldBe 1
    assertFailsWith<ExpressionException> { agent.count("AnyWordHere") }
    assertFailsWith<ExpressionException> { agent.resolve("Class<AnyWordHere>") }
    assertFailsWith<ExpressionException> { agent.runOperation("Class<AnyWordHere>!") }
    assertFailsWith<ExpressionException> { agent.runOperation("-Class<AnyWordHere>!") }
  }
}
