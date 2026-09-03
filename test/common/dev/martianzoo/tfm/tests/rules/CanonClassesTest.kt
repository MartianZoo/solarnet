package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.engine.Agent
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

/** Tests for the Canon data set. */
internal class CanonClassesTest {
  companion object {
    val table = Canon.classTable

    private fun te(source: String): Expression = parse(source)
  }

  @Test
  internal fun setupSeparatesPlayersFromActors() {
    val premise = canonicalPremise()
    premise.actors
        .filterIsInstance<dev.martianzoo.pets.data.Player>()
        .shouldContainExactly(PLAYER1, PLAYER2)
    premise.actors.shouldContainExactly(PLAYER1, PLAYER2, ENGINE)
    val game = Engine.newGame(premise)
    game.classTable.allClassNames.shouldNotContain(cn("SoloMode"))
    game.classTable.allClassNames.shouldNotContain(cn("SoloOpponent"))
    game.classTable.allClassNames.shouldNotContain(cn("SoloStandardResourceReserve"))
    game.classTable.allClassNames.shouldNotContain(cn("SoloCardResourceReserve"))
    game.classTable.allClassNames.shouldNotContain(cn("PreludeCard"))
    game.classTable.allClassNames.shouldNotContain(cn("PreludePhase"))
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
      val agent = game.tfm(PLAYER1)

      withClue(map.name) {
        agent.count("Class<Milestone>") shouldBe 6
        agent.count("Class<Award>") shouldBe 6
      }
    }
  }

  @Test
  internal fun preludeSetupDealsTwoPreludeCardsToEachPlayer() {
    val game = setUpGame(canonicalPremise(PreludeExpansion, players = 2))

    game.tfm(PLAYER1).phase("Prelude")

    game.tfm(PLAYER1).count("PreludeCard<Player1>") shouldBe 2
    game.tfm(PLAYER2).count("PreludeCard<Player2>") shouldBe 2
  }

  @Test
  internal fun soloSetupUsesPetsOnlyOpponent() {
    val premise = canonicalPremise(players = 1)
    premise.actors.shouldContainExactly(PLAYER1, ENGINE)
    val game = setUpGame(premise)
    game.classTable.allClassNames.shouldNotContain(cn("Player2"))
    game.reader.count(game.reader.resolve(te("SoloMode"))) shouldBe 1
    game.reader.count(game.reader.resolve(te("StandardSoloObjective"))) shouldBe 1
    game.reader.count(game.reader.resolve(te("SoloOpponent"))) shouldBe 1
    game.agent(PLAYER1).count("TerraformRating<Me>") shouldBe 14
    listOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat").forEach {
      game.agent(PLAYER1).count("$it<SoloOpponent>") shouldBe 42
      game.agent(PLAYER1).count("PROD[$it<SoloOpponent>]") shouldBe 42
    }
    game.agent(PLAYER1).count("SoloStandardResourceReserve<SoloOpponent>") shouldBe
        game.agent(PLAYER1).count("Class<StandardResource>")
    game.agent(PLAYER1).count("SoloCardResourceReserve<SoloOpponent>") shouldBe
        game.agent(PLAYER1).count("Class<CardResource>")
    game.agent(PLAYER1).count("SoloCardResourceReserve<SoloOpponent, Class<Animal>>") shouldBe 1
    game
        .agent(PLAYER1)
        .count(
            "Animal<SoloOpponent, SoloCardResourceReserve<SoloOpponent, Class<Animal>>>"
        ) shouldBe 42
    val soloReserve = game.classTable.getClass(cn("SoloCardResourceReserve"))
    soloReserve.isSubtypeOf(game.classTable.getClass(cn("CardFront"))) shouldBe false
    soloReserve.isSubtypeOf(game.classTable.getClass(cn("ActiveCard"))) shouldBe false

    val engine = game.agent(ENGINE) as Agent
    game.tasks.extract { it.assignee } shouldBe listOf(ENGINE, ENGINE)
    engine.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    engine.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    engine.manual("OceanTile<Tharsis_1_2>")
    game.agent(PLAYER1).count("CityTile<SoloOpponent>") shouldBe 2
    game.agent(PLAYER1).count("GreeneryTile<SoloOpponent>") shouldBe 2

    val player = game.agent(PLAYER1)
    player.manual("-5 Plant<SoloOpponent>")
    player.manual("PROD[-5 Plant<SoloOpponent>]")
    player.manual("5 Plant<SoloOpponent>")
    player.manual("PROD[5 Plant<SoloOpponent>]")
    player.manual("-5 Animal<SoloOpponent, SoloCardResourceReserve<SoloOpponent, Class<Animal>>>")
    player.manual("5 Animal<SoloOpponent, SoloCardResourceReserve<SoloOpponent, Class<Animal>>>")
    listOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat").forEach {
      game.agent(PLAYER1).count("$it<SoloOpponent>") shouldBe 42
      game.agent(PLAYER1).count("PROD[$it<SoloOpponent>]") shouldBe 42
      game.agent(PLAYER1).count("$it<Me>") shouldBe 0
    }
    game
        .agent(PLAYER1)
        .count(
            "Animal<SoloOpponent, SoloCardResourceReserve<SoloOpponent, Class<Animal>>>"
        ) shouldBe 42

    engine.manual("End FROM Phase")
    game.agent(PLAYER1).count("VictoryPoint<Me>") shouldBe 14
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun testOwnedTileIsAnIntersectionType() {
    val owned = table.getClass(cn("Owned"))
    val tile = table.getClass(cn("Tile"))
    val ownedTile = table.getClass(cn("OwnedTile"))

    // Nothing can be both Owned and a Tile without being an OwnedTile!
    owned glb tile shouldBe ownedTile
    ownedTile.isIntersectionType() shouldBe true
  }

  @Test
  internal fun testActionCardIsAnIntersectionType() {
    val cardFront = table.getClass(cn("CardFront"))
    val hasActions = table.getClass(cn("HasActions"))
    val actionCard = table.getClass(cn("ActionCard"))

    // Nothing can be both a CardFront and a HasActions but an ActionCard!
    cardFront glb hasActions shouldBe actionCard
    actionCard.isIntersectionType() shouldBe true
  }

  @Test
  internal fun cardboundComponentsRequirePlayerOwners() {
    table.resolve(te("ResourceHolder<SoloOpponent, Class<Animal>>"))
    assertFailsWith<ExpressionException> {
      table.resolve(te("Cardbound<SoloOpponent, $Predators<Player1>>"))
    }
  }

  @Test
  internal fun inactiveClassLiteralCountsZeroWhileUnknownClassLiteralIsInvalid() {
    val game = Engine.newGame(canonicalPremise())
    val agent = game.agent(PLAYER1) as Agent
    val withVenus =
        Engine.newGame(canonicalPremise(VenusNextExpansion, players = 2)).agent(PLAYER1) as Agent

    assertFailsWith<ExpressionException> { agent.count("Class<AnyWordHere>") }
    agent.count("Class<VenusStep>") shouldBe 0
    withVenus.count("Class<VenusStep>") shouldBe 1
    assertFailsWith<ExpressionException> { agent.count("AnyWordHere") }
    assertFailsWith<ExpressionException> { agent.resolve("Class<AnyWordHere>") }
    assertFailsWith<ExpressionException> { agent.manual("Class<AnyWordHere>!") }
    assertFailsWith<ExpressionException> { agent.manual("-Class<AnyWordHere>!") }
  }
}
