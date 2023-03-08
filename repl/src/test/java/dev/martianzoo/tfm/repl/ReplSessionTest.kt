package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Test

private class ReplSessionTest {
  @Test
  fun test() {
    val repl = ReplSession(Canon)
    repl.command("new MB 2")
    repl.command("become Player2")
    var n = repl.session.game!!.changeLogFull().size - 1

    // TODO deprodify these for the screen
    val suffix = "BY Player2 (by fiat)"
    assertThat(repl.command("exec PROD[5, 4 Energy]"))
        .containsExactly(
            "${++n}: 5 Production<Player2, Class<Megacredit>> $suffix",
            "${++n}: 4 Production<Player2, Class<Energy>> $suffix",
        )
    assertThat(repl.command("exec StripMine, BuildingTag<StripMine>"))
        .containsExactly(
            "${++n}: 1 StripMine<Player2> $suffix",
            "${++n}: 1 BuildingTag<Player2, StripMine<Player2>> $suffix",
        )
    assertThat(repl.command("exec PROD[-2 Energy, 2 Steel, Titanium]"))
        .containsExactly(
            "${++n}: -2 Production<Player2, Class<Energy>> $suffix",
            "${++n}: 2 Production<Player2, Class<Steel>> $suffix",
            "${++n}: 1 Production<Player2, Class<Titanium>> $suffix",
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
    repl.command("new MB 2")
    repl.command("become Player1")
    repl.command("exec PROD[9, 8 Steel, 7 Titanium, 6 Plant, 5 Energy, 4 Heat]")
    repl.command("exec 8, 6 Steel, 7 Titanium, 5 Plant, 3 Energy, 9 Heat")

    val board = BoardToText(repl.session.game!!.reader).board(cn("Player1").expr, false)
    assertThat(board)
        .containsExactly(
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
    repl.command("new MB 3")
    repl.command("become P1")
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
