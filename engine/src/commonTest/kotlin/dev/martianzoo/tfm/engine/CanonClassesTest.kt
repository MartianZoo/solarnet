package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.ACTOR
import dev.martianzoo.api.SystemClasses.ANYONE
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.SystemClasses.HIDDEN
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.SystemClasses.PLAYER
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Gameplay.GodMode
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.Option.*
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.types.ClassLoader
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.te
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

/** Tests for the Canon data set. */
internal class CanonClassesTest {
  companion object {
    val table = ClassLoader(Canon).loadEverything()
  }

  @Test
  fun childlessAbstractClass() {
    val anomalies = table.allClasses().filter { it.abstract && it.directSubclasses().none() }
    anomalies.shouldBeEmpty()
  }

  @Test
  fun abstractClassWithOnlyChild() {
    // In some cases we might like the parent and child to be treated as the same class
    val anomalies = table.allClasses().filter { it.abstract && it.directSubclasses().size == 1 }
    anomalies.classNames().shouldContainExactlyInAnyOrder(ANYONE, cn("NoctisArea"))
  }

  @Test
  fun actorOwnerAndPlayerHierarchy() {
    val actor = table.getClass(ACTOR)
    val owner = table.getClass(OWNER)
    val player = table.getClass(PLAYER)
    val engine = table.getClass(cn("Engine"))

    player.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(ACTOR, OWNER)
    engine
        .allSuperclasses()
        .classNames()
        .shouldContainExactlyInAnyOrder(
            COMPONENT,
            HIDDEN,
            ACTOR,
            cn("System"),
            cn("Engine"),
        )
    (actor glb owner) shouldBe player
  }

  @Test
  fun setupSeparatesPlayersFromActors() {
    val premise = canonicalPremise()
    premise.actors
        .filterIsInstance<dev.martianzoo.data.Player>()
        .shouldContainExactly(PLAYER1, PLAYER2)
    premise.actors.shouldContainExactly(PLAYER1, PLAYER2, ENGINE)
    val game = Engine.newGame(premise)
    game.classTable.allClassNames.shouldNotContain(cn("SoloMode"))
    game.classTable.allClassNames.shouldNotContain(cn("SoloOpponent"))
    game.classTable.allClassNames.shouldNotContain(cn("FakeResourceGiver"))
    game.classTable.allClassNames.shouldNotContain(cn("FakeResourceHolder"))
    game.classTable.allClassNames.shouldNotContain(cn("PreludeCard"))
    game.classTable.allClassNames.shouldNotContain(cn("PreludePhase"))
  }

  @Test
  fun everyMapOffersSixMilestonesAndAwardsWithVenusAndColonies() {
    val maps = Canon.Option.entries.filter { it.name.endsWith("MapOption") }

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
      val gameplay = game.tfm(PLAYER1)

      withClue(map.name) {
        gameplay.count("Class<Milestone>") shouldBe 6
        gameplay.count("Class<Award>") shouldBe 6
      }
    }
  }

  @Test
  fun preludeSetupDealsTwoPreludeCardsToEachPlayer() {
    val game = setUpGame(canonicalPremise(PreludeExpansion, players = 2))

    game.tfm(PLAYER1).phase("Prelude")

    game.tfm(PLAYER1).count("PreludeCard<Player1>") shouldBe 2
    game.tfm(PLAYER2).count("PreludeCard<Player2>") shouldBe 2
  }

  @Test
  fun soloSetupUsesPetsOnlyOpponent() {
    val premise = canonicalPremise(players = 1)
    premise.actors.shouldContainExactly(PLAYER1, ENGINE)
    val game = setUpGame(premise)
    game.classTable.allClassNames.shouldNotContain(cn("Player2"))
    game.reader.count(game.reader.resolve(te("SoloMode"))) shouldBe 1
    game.reader.count(game.reader.resolve(te("StandardSoloVariant"))) shouldBe 1
    game.reader.count(game.reader.resolve(te("SoloOpponent"))) shouldBe 1
    game.gameplay(PLAYER1).count("TerraformRating<Player1>") shouldBe 14
    listOf("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat").forEach {
      game.gameplay(PLAYER1).count("$it<SoloOpponent>") shouldBe 11
      game.gameplay(PLAYER1).count("PROD[$it<SoloOpponent>]") shouldBe 11
    }
    game.gameplay(PLAYER1).count("FakeResourceGiver<SoloOpponent>") shouldBe
        game.gameplay(PLAYER1).count("Class<StandardResource>")
    game.gameplay(PLAYER1).count("FakeResourceHolder<SoloOpponent>") shouldBe
        game.gameplay(PLAYER1).count("Class<CardResource>")
    game.gameplay(PLAYER1).count("FakeResourceHolder<SoloOpponent, Class<Animal>>") shouldBe 1
    game
        .gameplay(PLAYER1)
        .count("Animal<SoloOpponent, FakeResourceHolder<SoloOpponent, Class<Animal>>>") shouldBe 11
    val fakeHolder = game.classTable.getClass(cn("FakeResourceHolder"))
    fakeHolder.isSubtypeOf(game.classTable.getClass(cn("CardFront"))) shouldBe false
    fakeHolder.isSubtypeOf(game.classTable.getClass(cn("ActiveCard"))) shouldBe false

    val engine = game.gameplay(ENGINE) as GodMode
    game.tasks.extract { it.assignee } shouldBe listOf(ENGINE, ENGINE)
    engine.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    engine.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    engine.manual("OceanTile<Tharsis_1_2>")
    game.gameplay(PLAYER1).count("CityTile<SoloOpponent>") shouldBe 2
    game.gameplay(PLAYER1).count("GreeneryTile<SoloOpponent>") shouldBe 2

    val player = game.gameplay(PLAYER1).godMode()
    player.manual("-5 Plant<SoloOpponent>")
    player.manual("PROD[-5 Plant<SoloOpponent>]")
    player.manual("5 Plant<SoloOpponent>")
    player.manual("PROD[5 Plant<SoloOpponent>]")
    player.manual("-5 Animal<SoloOpponent, FakeResourceHolder<SoloOpponent, Class<Animal>>>")
    player.manual("5 Animal<SoloOpponent, FakeResourceHolder<SoloOpponent, Class<Animal>>>")
    listOf("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat").forEach {
      game.gameplay(PLAYER1).count("$it<SoloOpponent>") shouldBe 11
      game.gameplay(PLAYER1).count("PROD[$it<SoloOpponent>]") shouldBe 11
      game.gameplay(PLAYER1).count("$it<Player1>") shouldBe 0
    }
    game
        .gameplay(PLAYER1)
        .count("Animal<SoloOpponent, FakeResourceHolder<SoloOpponent, Class<Animal>>>") shouldBe 11

    engine.manual("End")
    game.gameplay(PLAYER1).count("VictoryPoint<Player1>") shouldBe 14
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun testOwnedTileIsAnIntersectionType() {
    val owned = table.getClass(cn("Owned"))
    val tile = table.getClass(cn("Tile"))
    val ownedTile = table.getClass(cn("OwnedTile"))

    // Nothing can be both Owned and a Tile without being an OwnedTile!
    owned glb tile shouldBe ownedTile
    ownedTile.isIntersectionType() shouldBe true
  }

  @Test
  fun testActionCardIsAnIntersectionType() {
    val cardFront = table.getClass(cn("CardFront"))
    val hasActions = table.getClass(cn("HasActions"))
    val actionCard = table.getClass(cn("ActionCard"))

    // Nothing can be both a CardFront and a HasActions but an ActionCard!
    cardFront glb hasActions shouldBe actionCard
    actionCard.isIntersectionType() shouldBe true
  }

  @Test
  fun cardboundComponentsRequirePlayerOwners() {
    table.resolve(te("ResourceHolder<SoloOpponent, Class<Animal>>"))
    assertFailsWith<ExpressionException> {
      table.resolve(te("Cardbound<SoloOpponent, Predators<Player1>>"))
    }
  }

  @Test
  fun component() {
    val loader = ClassLoader(Canon)

    with(loader.componentClass) {
      abstract shouldBe true
      // directDependencyKeys.shouldBeEmpty()
      // allDependencyKeys.shouldBeEmpty()
      directSuperclasses.shouldBeEmpty()
    }

    with(loader.load(cn("OceanTile"))) {
      // directDependencyKeys.shouldBeEmpty()
      // allDependencyKeys.shouldContainExactlyInAnyOrder(Key(cn("Tile"), 0))
      directSuperclasses
          .classNames()
          .shouldContainExactlyInAnyOrder(cn("GlobalParameter"), cn("Tile"))
      allSuperclasses()
          .classNames()
          .shouldContainExactlyInAnyOrder(
              cn("Component"),
              cn("Atomized"),
              cn("GlobalParameter"),
              cn("Tile"),
              cn("OceanTile"),
          )

      loader.load(cn("MarsArea"))
      baseType shouldBe loader.resolve(te("OceanTile<MarsArea>"))
    }
  }

  @Test
  fun testAllConcreteSubtypes() {
    val table = ClassTable.forPremise(canonicalPremise(players = 2))

    fun checkConcreteSubtypeCount(expr: String, size: Int) {
      val type = table.resolve(te(expr))
      type.allConcreteSubtypes().toList().shouldHaveSize(size)
    }

    checkConcreteSubtypeCount("Plant<Player1>", 1)
    checkConcreteSubtypeCount("Plant", 2)
    checkConcreteSubtypeCount("StandardResource<Player1>", 6)
    checkConcreteSubtypeCount("StandardResource", 12)
    checkConcreteSubtypeCount("Class<StandardResource>", 6)

    checkConcreteSubtypeCount("Class<MarsArea>", 61)
    checkConcreteSubtypeCount("Class<RemoteArea>", 2)
    checkConcreteSubtypeCount("Class<Tile>", 13)
    checkConcreteSubtypeCount("Class<SpecialTile>", 10)

    checkConcreteSubtypeCount("CityTile", 63 * 2)
    checkConcreteSubtypeCount("OceanTile", 61)
    checkConcreteSubtypeCount("GreeneryTile", 61 * 2)
    checkConcreteSubtypeCount("SpecialTile", (10 * 61) * 2)

    // Do this one the long way because the error message is horrific
    val type = table.resolve(te("Tile"))
    type.allConcreteSubtypes().count() shouldBe 61 + (63 * 2) + (61 * 2) + (10 * 61 * 2)
  }

  @Test
  fun phantomClassLiteralCountsZeroWhileUnknownClassLiteralIsInvalid() {
    val game = Engine.newGame(canonicalPremise())
    val gameplay = game.gameplay(PLAYER1) as GodMode
    val withVenus =
        Engine.newGame(canonicalPremise(VenusNextExpansion, players = 2)).gameplay(PLAYER1)
            as GodMode

    assertFailsWith<ExpressionException> { gameplay.count("Class<AnyWordHere>") }
    gameplay.count("Class<VenusStep>") shouldBe 0
    withVenus.count("Class<VenusStep>") shouldBe 1
    assertFailsWith<ExpressionException> { gameplay.count("AnyWordHere") }
    assertFailsWith<ExpressionException> { gameplay.resolve("Class<AnyWordHere>") }
    assertFailsWith<ExpressionException> { gameplay.manual("Class<AnyWordHere>!") }
    assertFailsWith<ExpressionException> { gameplay.manual("-Class<AnyWordHere>!") }
  }
}
