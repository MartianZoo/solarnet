package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Metric.Companion.metric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

private class ReplSessionTest {
  @Test
  fun testProductionPhase() {
    val repl = ReplSession(Canon, GameSetup(Canon, "BMX", 2)) // TODO
    val commands = """
      become Player1
      exec PROD[1, 2 S, 3 T, 4 P, 5 E, 6 H]
      exec 8, 6 S, 7 T, 5 P, 3 E
      exec 9 TR
    """.trimIndent()
    for (cmd in commands.split("\n")) {
      repl.command(cmd)
    }
    repl.command("exec ProductionPhase")
    assertThat(repl.session.count(metric("S"))).isEqualTo(8)
    assertThat(repl.session.count(metric("T"))).isEqualTo(10)
    assertThat(repl.session.count(metric("P"))).isEqualTo(9)
    assertThat(repl.session.count(metric("E"))).isEqualTo(5)
    assertThat(repl.session.count(metric("H"))).isEqualTo(9)
    assertThat(repl.session.count(metric("M"))).isEqualTo(38) // 8 + 29 + 1
  }

  @Test
  fun testAstrodrill() {
    val repl = ReplSession(Canon, GameSetup(Canon, "BMX", 2)) // TODO
    val commands = """
      newgame BMX 2
      become Player2
      exec Astrodrill
      exec UseAction1<UseActionFromCard>
      task A UseAction1<Astrodrill> THEN ActionUsedMarker<Astrodrill>
      task B Plant
    """.trimIndent()
    for (cmd in commands.split("\n")) {
      repl.command(cmd)
    }
    assertThat(repl.session.count(metric("Plant"))).isEqualTo(1)
    assertThat(repl.session.count(metric("ActionUsedMarker"))).isEqualTo(1)
  }

  @Test
  fun test() {
    val repl = ReplSession(Canon, GameSetup(Canon, "MB", 2)) // TODO
    repl.command("become Player2")
    repl.command("mode green")
    repl.command("exec ProjectCard")

    // TODO deprodify these for the screen
    assertThat(strip(repl.command("exec PROD[5, 4 Energy]")))
        .containsExactly(
            "+5 Production<Player2, Class<Megacredit>> FOR Player2 (manual)",
            "+4 Production<Player2, Class<Energy>> FOR Player2 (manual)",
        )
    val byCard = "FOR Player2 BY StripMine<Player2>"
    assertThat(strip(repl.command("exec StripMine")))
        .containsExactly(
            "+StripMine<Player2> FOR Player2 (manual)",
            "-ProjectCard<Player2> FOR Player2 BY StripMine<Player2>",
            "+BuildingTag<Player2, StripMine<Player2>> $byCard",
            "-2 Production<Player2, Class<Energy>> $byCard",
            "+2 Production<Player2, Class<Steel>> $byCard",
            "+Production<Player2, Class<Titanium>> $byCard",
            "+OxygenStep $byCard",
            "+TerraformRating<Player2> FOR Player2 BY OxygenStep",
            "+OxygenStep $byCard",
            "+TerraformRating<Player2> FOR Player2 BY OxygenStep",
        )

    val check1 = "has PROD[=2 Energy, =2 Steel]"
    assertThat(repl.command(check1).first()).startsWith("true")

    repl.command("become Player1")
    val check2 = "has PROD[=0 Energy, =0 Steel]"
    assertThat(repl.command(check2).first()).startsWith("true")
  }

  @Test
  fun testBoard() {
    val repl = ReplSession(Canon, GameSetup(Canon, "MB", 2)) // TODO
    repl.command("become Player1")
    repl.command("exec PROD[9, 8 Steel, 7 Titanium, 6 Plant, 5 Energy, 4 Heat]")
    repl.command("exec 8, 6 Steel, 7 Titanium, 5 Plant, 3 Energy, 9 Heat")

    val game = repl.session.game ?: fail("huh?")
    val board = BoardToText(game.reader, false).board(cn("Player1").expr)
    assertThat(board)
        .containsExactly(
            "  Player1   TR: 20   Tiles: 0",
            "+---------+---------+---------+",
            "|  M:   8 |  S:   6 |  T:   7 |",
            "| prod  9 | prod  8 | prod  7 |",
            "+---------+---------+---------+",
            "|  P:   5 |  E:   3    H:   9 |",
            "| prod  6 | prod  5 | prod  4 |",
            "+---------+---------+---------+",
        )
        .inOrder()
  }

  @Test
  fun testMap() {
    val repl = ReplSession(Canon, GameSetup(Canon, "MB", 3)) // TODO
    repl.command("become P1")
    repl.command("mode red")
    repl.command("exec OT<M26>, OT<M55>, OT<M56>, CT<M46>, GT<M57>, GT<M45, P3>")
    repl.command("exec Tile008<P2, M66>, Tile142<P2, M99>")

    assertThat(repl.command("map"))
        .containsExactly(
            "                              1       2       3       4       5       6       7       8       9",
            "                             /       /       /       /       /       /       /       /       /",
            "",
            " 1 —                     LSS     WSS      L      WC       W",
            "",
            "",
            " 2 —                  L      VS       L       L       L      [O]",
            "",
            "",
            " 3 —             VC       L       L       L       L       L       L",
            "",
            "",
            " 4 —         VPT     LP      LP      LP     [G3]    [C1]     LP      WPP",
            "",
            "",
            " 5 —     VPP     LPP     NPP     WPP     [O]     [O]    [G1]     LPP     LPP",
            "",
            "",
            " 6 —         LP      LPP     LP      LP     [C2]     WP      WP      WP",
            "",
            "",
            " 7 —              L       L       L       L       L      LP       L",
            "",
            "",
            " 8 —                 LSS      L      LC      LC       L      LT",
            "",
            "",
            " 9 —                     LSS     LSS      L       L     [S2]",
        )
        .inOrder()
  }
}

fun strip(strings: Iterable<String>): List<String> {
  return strings.map { endRegex.replace(startRegex.replace(it, ""), "") }
}

private val startRegex = Regex("^[^:]+: ")
private val endRegex = Regex(" BECAUSE.*")
