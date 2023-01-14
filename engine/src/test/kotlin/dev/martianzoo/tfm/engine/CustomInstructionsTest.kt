package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.CopyProductionBox
import dev.martianzoo.tfm.data.ActionDefinition
import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.MapAreaDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.pets.PetParser.parseScript
import dev.martianzoo.util.Grid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class CustomInstructionsTest {

  @Test
  fun robinson() {
    val game = Engine.newGame(Canon, 3, setOf("B"))
    val s = """
      // The standard hack for every player - ignore it!
      EXEC PROD[5 Megacredit<Player1>]

      EXEC PROD[Steel<Player1>, Titanium<Player1>, Plant<Player1>, Energy<Player1>, Heat<Player1>]
      EXEC $${""}gainLowestProduction(Player1)
      COUNT Production<Player1, Megacredit.CLASS> -> foo      
    """

    assertThat(game.classTable["Player1"].abstract).isFalse()
    assertThat(game.resolve("Player1").abstract).isFalse()
    val script = parseScript(s)
    val results = game.execute(script)
    assertThat(results["foo"]).isEqualTo(6)
  }

  @Test
  fun robinsonCant() {
    val game = Engine.newGame(Canon, 3, setOf("B"))
    val s = """
      // The standard hack for every player - ignore it!
      EXEC PROD[5 Megacredit<Player1>]

      EXEC PROD[Steel<Player1>, Titanium<Player1>, Plant<Player1>, Heat<Player1>]
      EXEC $${""}gainLowestProduction(Player1)
    """
    val script = parseScript(s)
    assertThrows<RuntimeException> { game.execute(script) }
    game.changeLog.forEach(::println)
  }

  // TODO figure out how to make gradle compile the java code
  // It seemed like adding plugins { `java-library` } should have been enough
  fun java() {
    val auth = object : Authority() {
      override val explicitClassDeclarations: Collection<ClassDeclaration> = Canon.explicitClassDeclarations
      override val actionDefinitions: Collection<ActionDefinition> = Canon.actionDefinitions
      override val cardDefinitions: Collection<CardDefinition> = Canon.cardDefinitions
      override val mapAreaDefinitions: Map<String, Grid<MapAreaDefinition>> = Canon.mapAreaDefinitions
      override val milestoneDefinitions: Collection<MilestoneDefinition> = Canon.milestoneDefinitions
      override fun customInstructions() = listOf(CustomJavaExample.GainLowestProduction())
    }

    val game = Engine.newGame(auth, 3, setOf("B"))
    val s = """
      // The standard hack for every player - ignore it!
      EXEC PROD[5 Megacredit<Player1>]
  
      EXEC PROD[Steel<Player1>, Titanium<Player1>, Plant<Player1>, Energy<Player1>, Heat<Player1>]
      EXEC $${""}gainLowestProduction(Player1)
      REQUIRE =6 Production<Player1, Megacredit.CLASS>      
    """

    game.execute(parseScript(s))
  }

  @Test
  fun robinson2() {
    val game = Engine.newGame(Canon, 3, setOf("B"))
    val s = """
      // The standard hack for every player - ignore it!
      EXEC PROD[5 Megacredit<Player1>]

      EXEC PROD[-Megacredit<Player1>]
      EXEC $${""}gainLowestProduction(Player1)
      REQUIRE =5 Production<Player1, Megacredit.CLASS>
    """
    val script = parseScript(s)
    game.execute(script)
  }

  fun roboWork() {
    val game = Engine.newGame(Canon, 2, setOf("B", "R"))
    val s = """
      // The standard hack for every player - ignore it!
      EXEC PROD[5 Megacredit<Player1>]

      EXEC PROD[4 Energy<Player1>]
      
      EXEC StripMine<Player1>
      // we don't have effects working yet so...
      EXEC PROD[-2 Energy<Player1>, 2 Steel<Player1>, Titanium<Player1>]
      
      REQUIRE PROD[=2 Energy<Player1>, 2 Steel<Player1>]
      EXEC $${""}copyProductionBox(StripMine<Player1>)

      REQUIRE PROD[=0 Energy<Player1>, 4 Steel<Player1>]

    """
    val script = parseScript(s)
    game.execute(script)
  }
}
