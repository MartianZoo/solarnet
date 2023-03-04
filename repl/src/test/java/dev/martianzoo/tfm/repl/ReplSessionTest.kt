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
    var n = repl.session.game!!.changeLogFull().size - 1

    assertThat(repl.command("exec PROD[5, 4 Energy]"))
        .containsExactly(
            "${++n}: 5 Production<Player2, Class<Megacredit>>",
            "${++n}: 4 Production<Player2, Class<Energy>>",
        )
    assertThat(repl.command("exec StripMine, BuildingTag<StripMine>"))
        .containsExactly(
            "${++n}: 1 StripMine<Player2>",
            "${++n}: 1 BuildingTag<Player2, StripMine<Player2>>",
        )
    assertThat(repl.command("exec PROD[-2 Energy, 2 Steel, Titanium]"))
        .containsExactly(
            "${++n}: -2 Production<Player2, Class<Energy>>",
            "${++n}: 2 Production<Player2, Class<Steel>>",
            "${++n}: 1 Production<Player2, Class<Titanium>>",
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
    repl.command("exec PROD[14, 8 Steel, 7 Titanium, 6 Plant, 5 Energy, 4 Heat]")
    repl.command("exec 8, 6 Steel, 7 Titanium, 5 Plant, 3 Energy, 9 Heat")

    val board = BoardToText(repl.session.game!!).board(cn("Player1").expr, false)
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
    repl.command("newgame MB 3")
    repl.command("become Player1")
    repl.command("exec OceanTile<Tharsis_2_6>, OceanTile<Tharsis_5_5>, OceanTile<Tharsis_5_6>")
    repl.command("exec CityTile<Tharsis_4_6>, GreeneryTile<Tharsis_5_7>")
    repl.command("exec GreeneryTile<Tharsis_4_5, Player3>")

    repl.command("become Player2")
    repl.command("exec Tile008<Tharsis_6_6>")
    repl.command("exec Tile142<Tharsis_9_9>")

    assertThat(repl.command("map"))
        .containsExactly(
            "                       1     2     3     4     5     6     7     8     9",
            "                      /     /     /     /     /     /     /     /     /",
            "",
            "1 —                LSS   WSS    L    WC     W",
            "",
            "2 —              L    VS     L     L     L    [O]",
            "",
            "3 —          VC     L     L     L     L     L     L",
            "",
            "4 —       VPT   LP    LP    LP   [G3]  [C1]   LP    WPP",
            "",
            "5 —    VPP   LPP   NPP   WPP   [O]   [O]  [G1]   LPP   LPP",
            "",
            "6 —       LP    LPP   LP    LP   [C2]   WP    WP    WP",
            "",
            "7 —           L     L     L     L     L    LP     L",
            "",
            "8 —             LSS    L    LC    LC     L    LT",
            "",
            "9 —                LSS   LSS    L     L   [S2]",
        )
        .inOrder()
  }
}
