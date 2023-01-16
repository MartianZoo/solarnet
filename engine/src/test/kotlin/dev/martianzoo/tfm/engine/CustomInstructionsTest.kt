package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.Parsing.parseScript
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class CustomInstructionsTest {

  @Test
  fun robinson() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 3))
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
    val game = Engine.newGame(GameSetup(Canon, "BM", 3))
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
    val auth = object : Authority.Forwarding(Canon) {
      override val customInstructions = listOf(CustomJavaExample.GainLowestProduction())
    }

    val game = Engine.newGame(GameSetup(Canon, "BRM", 3))
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
    val game = Engine.newGame(GameSetup(Canon, "BRM", 3))
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
    val game = Engine.newGame(GameSetup(Canon, "BM", 2))
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
