package dev.martianzoo.tfm.engine

import dev.martianzoo.api.SystemClasses.ACTOR
import dev.martianzoo.api.SystemClasses.ANYONE
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.SystemClasses.PLAYER
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Gameplay.GodMode
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.types.MClassLoader
import dev.martianzoo.types.te
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Tests for the Canon data set. */
internal class CanonClassesTest {
  companion object {
    val table = MClassLoader(Canon).loadEverything()
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
    anomalies.classNames().shouldContainExactlyInAnyOrder(ANYONE, cn("NoctisArea"), cn("Barrier"))
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
        .shouldContainExactlyInAnyOrder(COMPONENT, ACTOR, cn("System"), cn("Engine"))
    (actor glb owner) shouldBe player
  }

  @Test
  fun setupSeparatesPlayersFromActors() {
    Canon.SIMPLE_GAME.players().shouldContainExactly(PLAYER1, PLAYER2)
    Canon.SIMPLE_GAME.actors().shouldContainExactly(PLAYER1, PLAYER2, ENGINE)
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    game.classes.allClassNamesAndIds.shouldNotContain(cn("SoloMode"))
    game.classes.allClassNamesAndIds.shouldNotContain(cn("Opponent"))
  }

  @Test
  fun soloSetupUsesPetsOnlyOpponent() {
    val game = setUpGame(Canon.SIMPLE_SOLO_GAME)
    game.setup.players().shouldContainExactly(PLAYER1)
    game.setup.actors().shouldContainExactly(PLAYER1, ENGINE)
    game.classes.allClassNamesAndIds.shouldNotContain(cn("Player2"))
    game.reader.count(game.reader.resolve(te("SoloMode"))) shouldBe 1
    game.reader.count(game.reader.resolve(te("Opponent"))) shouldBe 1
    game.gameplay(PLAYER1).count("TerraformRating<Player1>") shouldBe 14
    listOf("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat").forEach {
      game.gameplay(PLAYER1).count("$it<Opponent>") shouldBe 99
      game.gameplay(PLAYER1).count("PROD[$it<Opponent>]") shouldBe 99
    }

    val engine = game.gameplay(ENGINE) as GodMode
    game.tasks.extract { it.assignee } shouldBe listOf(ENGINE, ENGINE)
    engine.doFirstTask("CityTile<Tharsis_4_1, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, Opponent>")
    engine.doFirstTask("CityTile<Tharsis_2_2, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, Opponent>")
    engine.manual("OceanTile<Tharsis_1_2>")
    game.gameplay(PLAYER1).count("CityTile<Opponent>") shouldBe 2
    game.gameplay(PLAYER1).count("GreeneryTile<Opponent>") shouldBe 2
    listOf("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat").forEach {
      game.gameplay(PLAYER1).count("$it<Opponent>") shouldBe 99
      game.gameplay(PLAYER1).count("$it<Player1>") shouldBe 0
    }

    engine.manual("End")
    game.gameplay(PLAYER1).count("VictoryPoint<Player1>") shouldBe 14
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun concreteExtendingConcrete() {
    val map = mutableListOf<Pair<ClassName, ClassName>>()
    table
        .allClasses()
        .filterNot { it.abstract }
        .forEach { sup ->
          (sup.allSubclasses() - setOf(sup)).forEach { map += sup.className to it.className }
        }
    map.shouldBeEmpty()
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
  fun component() {
    val loader = MClassLoader(Canon)

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
    val table = MClassLoader(Canon.fromOptionCodes("BRM", 2))

    fun checkConcreteSubtypeCount(expr: String, size: Int) {
      val mtype = table.resolve(te(expr))
      mtype.allConcreteSubtypes().toList().shouldHaveSize(size)
    }

    checkConcreteSubtypeCount("Plant<Player1>", 1)
    checkConcreteSubtypeCount("Plant", 2)
    checkConcreteSubtypeCount("StandardResource<Player1>", 6)
    checkConcreteSubtypeCount("StandardResource", 12)
    checkConcreteSubtypeCount("Class<StandardResource>", 6)

    checkConcreteSubtypeCount("Class<MarsArea>", 61)
    checkConcreteSubtypeCount("Class<RemoteArea>", 2)
    checkConcreteSubtypeCount("Class<Tile>", 11)
    checkConcreteSubtypeCount("Class<SpecialTile>", 8)

    checkConcreteSubtypeCount("CityTile", 63 * 2)
    checkConcreteSubtypeCount("OceanTile", 61)
    checkConcreteSubtypeCount("GreeneryTile", 61 * 2)
    checkConcreteSubtypeCount("SpecialTile", (8 * 61) * 2)

    // Do this one the long way because the error message is horrific
    val type = table.resolve(te("Tile"))
    type.allConcreteSubtypes().count() shouldBe 61 + (63 * 2) + (61 * 2) + (8 * 61 * 2)
  }
}
