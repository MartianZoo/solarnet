package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.tfm.repl.commands.TfmBoardCommand.PlayerBoardToText
import dev.martianzoo.tfm.repl.commands.TfmMapCommand
import org.junit.jupiter.api.Test

private class ReplSessionTest {
  @Test
  fun game20230521() {
    val repl = ReplSession()
    val commands =
        """
          newgame BRMVPX 2; mode blue; auto safe; phase Corporation

          become P1; turn; tfm_play Manutech; task 5 BuyCard
          become P2; turn; tfm_play Factorum; task 4 BuyCard

          as Engine phase Prelude

          become P1
          turn; tfm_play NewPartner; tfm_play UnmiContractor
          turn; tfm_play AlliedBank

          become P2
          turn; tfm_play AcquiredSpaceAgency
          turn; tfm_play IoResearchOutpost

          as Engine phase Action

          become P1
          turn; task UseAction1<PlayCardSA>; tfm_play InventorsGuild; tfm_pay 9
        """
            .trimIndent()
            .split(Regex(" *[\n;] *"))
            .filter { it.isNotEmpty() }

    val expectedOutput =
        """
          New 2-player game created with bundles: BRMVPX
          Mode BLUE: Turn integrity: must perform a valid game turn for this phase
          Autoexec mode is: SAFE
          0000: +CorporationPhase FROM SetupPhase FOR Engine (manual)
          0000: +CorporationCard<Player1> FOR Engine BY Player1 BECAUSE 0000
          0000: +CorporationCard<Player2> FOR Engine BY Player2 BECAUSE 0000
          Hi, Player1
          New tasks pending:
          Z  [Player1] PlayCard<Player1, Class<CorporationCard>>! (abstract)
          Z  [Player1] 10 BuyCard<Player1>? (abstract)
          0000: +Manutech<Player1> FROM CorporationCard<Player1> FOR Player1 BY PlayCard<Player1, Class<CorporationCard>, Class<Manutech>> BECAUSE 0000
          0000: +BuildingTag<Player1, Manutech<Player1>> FOR Player1 BY Manutech<Player1> BECAUSE 0000
          0000: +Production<Player1, Class<Steel>> FOR Player1 BY Manutech<Player1> BECAUSE 0000
          0000: +35 Megacredit<Player1> FOR Player1 BY Manutech<Player1> BECAUSE 0000
          0000: +Steel<Player1> FOR Player1 BY Manutech<Player1> BECAUSE 0000
          0000: -15 Megacredit<Player1> FOR Player1 BY BuyCard<Player1> BECAUSE 0000
          0000: +5 ProjectCard<Player1> FOR Player1 BY BuyCard<Player1> BECAUSE 0000
          Hi, Player2
          New tasks pending:
          Z  [Player2] PlayCard<Player2, Class<CorporationCard>>! (abstract)
          Z  [Player2] 10 BuyCard<Player2>? (abstract)
          0000: +Factorum<Player2> FROM CorporationCard<Player2> FOR Player2 BY PlayCard<Player2, Class<CorporationCard>, Class<Factorum>> BECAUSE 0000
          0000: +PowerTag<Player2, Factorum<Player2>> FOR Player2 BY Factorum<Player2> BECAUSE 0000
          0000: +BuildingTag<Player2, Factorum<Player2>> FOR Player2 BY Factorum<Player2> BECAUSE 0000
          0000: +37 Megacredit<Player2> FOR Player2 BY Factorum<Player2> BECAUSE 0000
          0000: +Production<Player2, Class<Steel>> FOR Player2 BY Factorum<Player2> BECAUSE 0000
          0000: -12 Megacredit<Player2> FOR Player2 BY BuyCard<Player2> BECAUSE 0000
          0000: +4 ProjectCard<Player2> FOR Player2 BY BuyCard<Player2> BECAUSE 0000
          0000: +PreludePhase FROM CorporationPhase FOR Engine (manual)
          0000: +2 PreludeCard<Player1> FOR Engine BY Player1 BECAUSE 0000
          0000: +2 PreludeCard<Player2> FOR Engine BY Player2 BECAUSE 0000
          Hi, Player1
          New tasks pending:
          Z* [Player1] PlayCard<Player1, Class<PreludeCard>>! OR (-PreludeCard<Player1>! THEN 15 Megacredit<Player1>!) (abstract)
          0000: +NewPartner<Player1> FROM PreludeCard<Player1> FOR Player1 BY PlayCard<Player1, Class<PreludeCard>, Class<NewPartner>> BECAUSE 0000
          0000: +Production<Player1, Class<Megacredit>> FOR Player1 BY NewPartner<Player1> BECAUSE 0000
          0000: +PreludeCard<Player1> FOR Player1 BY NewPartner<Player1> BECAUSE 0000
          0000: +Megacredit<Player1> FOR Player1 BY Manutech<Player1> BECAUSE 0000

          New tasks pending:
          Z* [Player1] PlayCard<Player1, Class<PreludeCard>>! (abstract)
          0000: +UnmiContractor<Player1> FROM PreludeCard<Player1> FOR Player1 BY PlayCard<Player1, Class<PreludeCard>, Class<UnmiContractor>> BECAUSE 0000
          0000: +EarthTag<Player1, UnmiContractor<Player1>> FOR Player1 BY UnmiContractor<Player1> BECAUSE 0000
          0000: +3 TerraformRating<Player1> FOR Player1 BY UnmiContractor<Player1> BECAUSE 0000
          0000: +ProjectCard<Player1> FOR Player1 BY UnmiContractor<Player1> BECAUSE 0000
          New tasks pending:
          Z* [Player1] PlayCard<Player1, Class<PreludeCard>>! OR (-PreludeCard<Player1>! THEN 15 Megacredit<Player1>!) (abstract)
          0000: +AlliedBank<Player1> FROM PreludeCard<Player1> FOR Player1 BY PlayCard<Player1, Class<PreludeCard>, Class<AlliedBank>> BECAUSE 0000
          0000: +EarthTag<Player1, AlliedBank<Player1>> FOR Player1 BY AlliedBank<Player1> BECAUSE 0000
          0000: +4 Production<Player1, Class<Megacredit>> FOR Player1 BY AlliedBank<Player1> BECAUSE 0000
          0000: +3 Megacredit<Player1> FOR Player1 BY AlliedBank<Player1> BECAUSE 0000
          0000: +4 Megacredit<Player1> FOR Player1 BY Manutech<Player1> BECAUSE 0000
          Hi, Player2
          New tasks pending:
          Z* [Player2] PlayCard<Player2, Class<PreludeCard>>! OR (-PreludeCard<Player2>! THEN 15 Megacredit<Player2>!) (abstract)
          0000: +AcquiredSpaceAgency<Player2> FROM PreludeCard<Player2> FOR Player2 BY PlayCard<Player2, Class<PreludeCard>, Class<AcquiredSpaceAgency>> BECAUSE 0000
          0000: +6 Titanium<Player2> FOR Player2 BY AcquiredSpaceAgency<Player2> BECAUSE 0000
          0000: +2 ProjectCard<Player2> FOR Player2 BY AcquiredSpaceAgency<Player2> BECAUSE 0000
          New tasks pending:
          Z* [Player2] PlayCard<Player2, Class<PreludeCard>>! OR (-PreludeCard<Player2>! THEN 15 Megacredit<Player2>!) (abstract)
          0000: +IoResearchOutpost<Player2> FROM PreludeCard<Player2> FOR Player2 BY PlayCard<Player2, Class<PreludeCard>, Class<IoResearchOutpost>> BECAUSE 0000
          0000: +ScienceTag<Player2, IoResearchOutpost<Player2>> FOR Player2 BY IoResearchOutpost<Player2> BECAUSE 0000
          0000: +JovianTag<Player2, IoResearchOutpost<Player2>> FOR Player2 BY IoResearchOutpost<Player2> BECAUSE 0000
          0000: +Production<Player2, Class<Titanium>> FOR Player2 BY IoResearchOutpost<Player2> BECAUSE 0000
          0000: +ProjectCard<Player2> FOR Player2 BY IoResearchOutpost<Player2> BECAUSE 0000
          0000: +ActionPhase FROM PreludePhase FOR Engine (manual)
          Hi, Player1
          New tasks pending:
          Z* [Player1] UseAction<Player1, StandardAction>! OR Pass<Player1>! (abstract)
          New tasks pending:
          Z* [Player1] PlayCard<Player1, Class<ProjectCard>>! (abstract)
          New tasks pending:
          Z* [Player1] X Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1>? (abstract)
          Z  [Player1] MAX 0 Barrier: InventorsGuild<Player1> FROM ProjectCard<Player1>!
          0000: +9 Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1> FOR Player1 BY Accept<Player1, Class<Megacredit>> BECAUSE 0000
          0000: +InventorsGuild<Player1> FROM ProjectCard<Player1> FOR Player1 BY PlayCard<Player1, Class<ProjectCard>, Class<InventorsGuild>> BECAUSE 0000
          0000: +ScienceTag<Player1, InventorsGuild<Player1>> FOR Player1 BY InventorsGuild<Player1> BECAUSE 0000
        """
            .trimIndent()
            .split("\n")

    // TODO The "MAX 0 Barrier" one should have said "(currently impossible)"
    // also why is there a random blank line up there??

    val output =
        commands.flatMap(repl::command).map {
          it.replace(digitsRegex, "0000").replace(letterRegex, "Z")
        }
    assertThat(output).containsExactlyElementsIn(expectedOutput).inOrder()
  }

  val digitsRegex = Regex("\\b\\d{4}\\b")
  val letterRegex = Regex("^[A-Z]\\b")

  @Test
  fun test() {
    val repl = ReplSession()
    repl.command("become Player2")
    repl.command("exec ProjectCard")

    assertThat(strip(repl.command("exec PROD[5, 4 Energy]")))
        .containsExactly(
            "+5 Production<Player2, Class<Megacredit>> FOR Player2 (manual)",
            "+4 Production<Player2, Class<Energy>> FOR Player2 (manual)",
        )
    val byCard = "FOR Player2 BY StripMine<Player2>"
    assertThat(strip(repl.command("exec StripMine")))
        .containsExactly(
            "+StripMine<Player2> FOR Player2 (manual)",
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
    val repl = ReplSession()
    repl.command("become Player1")
    repl.command("exec PROD[9, 8 Steel, 7 Titanium, 6 Plant, 5 Energy, 4 Heat]")
    repl.command("exec 8, 6 Steel, 7 Titanium, 5 Plant, 3 Energy, 9 Heat")

    val board = PlayerBoardToText(repl.tfm, false).board()
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
    val repl = ReplSession()
    repl.command("become Player1")
    repl.command("exec OT<M26>, OT<M55>, OT<M56>, CT<M46>, GT<M57>")
    repl.command("as Player2 exec GT<M45>, CT<M66>, MaTile<M99>")
    assertThat(repl.command("tasks")).isEmpty()
    assertThat(repl.tfm.count("Tile")).isEqualTo(8)

    assertThat(repl.command(TfmMapCommand(repl)))
        .containsExactlyElementsIn(
            """
                                   1    2    3    4    5    6    7    8    9
                                  /    /    /    /    /    /    /    /    /

               1 -            LSS  WSS   L    WC   W

               2 -           L   VS    L    L    L   [O]

               3 -        VC   L    L    L    L    L    L

               4 -     VPT  LP   LP   LP  [G2] [C1]  LP   WPP

               5 -  VPP  LPP  NPP  WPP  [O]  [O]  [G1] LPP  LPP

               6 -     LP   LPP  LP   LP  [C2]  WP   WP   WP

               7 -        L    L    L    L    L    LP   L

               8 -          LSS   L   LC   LC    L   LT

               9 -            LSS  LSS   L    L   [S2]
            """
                .replaceIndent(" ")
                .split("\n")
                .map { it.trimEnd() })
        .inOrder()
  }
}

fun strip(strings: Iterable<String>): List<String> {
  return strings.map { endRegex.replace(startRegex.replace(it, ""), "") }
}

private val startRegex = Regex("^[^:]+: ")
private val endRegex = Regex(" BECAUSE.*")
