package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Test

private class ReplSessionTest {
  @Test
  fun test() {
    val repl = ReplSession(Canon)
    repl.command("newgame MB 2")
    repl.command("become Player2")
    repl.command("mode green")

    // TODO deprodify these for the screen
    assertThat(strip(repl.command("exec PROD[5, 4 Energy]")))
        .containsExactly(
            "5 Production<Player2, Class<Megacredit>> FOR Player2 (by fiat)",
            "4 Production<Player2, Class<Energy>> FOR Player2 (by fiat)",
        )
    val byCard = "FOR Player2 BY StripMine<Player2>"
    assertThat(strip(repl.command("exec StripMine")))
        .containsExactly(
            "1 StripMine<Player2> FOR Player2 (by fiat)",
            "1 BuildingTag<Player2, StripMine<Player2>> $byCard",
            "-2 Production<Player2, Class<Energy>> $byCard",
            "2 Production<Player2, Class<Steel>> $byCard",
            "1 Production<Player2, Class<Titanium>> $byCard",
            "1 OxygenStep $byCard",
            "1 TerraformRating<Player2> FOR Player2 BY OxygenStep",
            "1 OxygenStep $byCard",
            "1 TerraformRating<Player2> FOR Player2 BY OxygenStep",
        )

    val check1 = "has PROD[=2 Energy, =2 Steel]"
    assertThat(repl.command(check1).first()).startsWith("true")

    repl.command("become Player1")
    val check2 = "has PROD[=0 Energy, =0 Steel]"
    assertThat(repl.command(check2).first()).startsWith("true")
  }

  @Test
  fun testBoard() {
    val repl = ReplSession(Canon)
    repl.command("newgame MB 2")
    repl.command("become Player1")
    repl.command("exec PROD[9, 8 Steel, 7 Titanium, 6 Plant, 5 Energy, 4 Heat]")
    repl.command("exec 8, 6 Steel, 7 Titanium, 5 Plant, 3 Energy, 9 Heat")

    val board = BoardToText(repl.session.game!!.reader, false).board(cn("Player1").expr)
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
    val repl = ReplSession(Canon)
    repl.command("newgame MB 3")
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
