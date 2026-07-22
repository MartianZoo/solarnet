package dev.martianzoo.tfm.script

import dev.martianzoo.script.ScriptSession
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.script.commands.TfmBoardCommand.PlayerBoardToText
import dev.martianzoo.tfm.script.commands.TfmMapCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ScriptSessionTest {
  private val eventOrdinalRegex = Regex("^\\d+(?=:)")
  private val causeOrdinalRegex = Regex("(?<=BECAUSE )\\d+")
  private val letterRegex = Regex("^[A-Z]\\b")

  private fun normalizeEventOrdinals(line: String) =
      line.replace(eventOrdinalRegex, "0000").replace(causeOrdinalRegex, "0000")

  @Test
  fun newGameDefersColonySelection() {
    val repl = ScriptSession()

    assertEquals(
        listOf("New 2-player game created with options: BRMCX"),
        repl.command("newgame BRMCX 2"),
    )
    assertTrue(repl.setup.options.deferredColonySelection)
    assertTrue(repl.setup.options.colonyTiles.isEmpty())
    assertEquals(listOf("0 Ceres"), repl.command("count Ceres"))
    assertEquals(listOf("0 Io"), repl.command("count Io"))
    assertEquals(listOf("0 Titan"), repl.command("count Titan"))
    val colonyTaskIds =
        repl.game.tasks
            .extract {
              if (it.instruction.toString().startsWith("AddColonyTile")) it.id else null
            }
            .filterNotNull()
    colonyTaskIds.zip(listOf("Ceres", "Io", "Titan", "Luna", "Pluto")).forEach { (id, tile) ->
      repl.command("task $id AddColonyTile<Class<$tile>>")
    }
    repl.command("phase Corporation")
  }

  @Test
  fun purpleModeSelectsColoniesAsSetupTasks() {
    val repl = ScriptSession()

    assertEquals(
        listOf(
            "New 1-player game created with options: BRMCS",
            "Purple mode: workflow active",
            "NOTE: Solo world-government terraforming and victory checking remain manual.",
        ),
        repl.command("newgame BRMC 1 purple"),
    )

    val colonyTaskIds =
        repl.game.tasks
            .extract {
              if (it.instruction.toString().startsWith("AddColonyTile")) it.id else null
            }
            .filterNotNull()
    assertEquals(3, colonyTaskIds.size)
    repl.command("task ${colonyTaskIds[0]} AddColonyTile<Class<Ceres>>")
    repl.command("task ${colonyTaskIds[1]} AddColonyTile<Class<Io>>")
    repl.command("task ${colonyTaskIds[2]} AddColonyTile<Class<Titan>>")
    repl.command("task N CityTile<Tharsis_2_4, Opponent>")
    repl.command("task GreeneryTile<Tharsis_2_3, Opponent>")
    repl.command("task CityTile<Tharsis_8_7, Opponent>")
    repl.command("task GreeneryTile<Tharsis_8_6, Opponent>")

    assertEquals(listOf("1 CorporationPhase"), repl.command("count CorporationPhase"))
    assertEquals(listOf("1 Ceres"), repl.command("count Ceres"))
    assertEquals(listOf("1 Io"), repl.command("count Io"))
    assertEquals(
        listOf("1 DelayedColonyTile<Class<Titan>>"),
        repl.command("count DelayedColonyTile<Class<Titan>>"),
    )
  }

  @Test
  fun testBasicRunthrough() {
    val repl = ScriptSession()

    fun command(c: String, expected: String) {
      val results = repl.command(c).map { normalizeEventOrdinals(it).replace(letterRegex, "Z") }
      assertEquals(expected.split("\n"), results)
    }

    command("newgame BRMPX 3", "New 3-player game created with options: BRMPX")
    command("tfm_sample A 3", "Okay, did that.")
    command(
        "tfm_board P1",
        """
          Player1   TR: 23   Tiles: 1
        +---------+---------+---------+
        |  M:  17 |  S:   0 |  T:   0 |
        | prod  7 | prod  3 | prod  0 |
        +---------+---------+---------+
        |  P:   1 |  E:   1    H:   8 |
        | prod  0 | prod  1 | prod  1 |
        +---------+---------+---------+
        """
            .trimIndent(),
    )
    command("count CityTile", "1 CityTile<Owner>")
    command("become P2", "Hi, Player2")
    command("count CityTile", "0 CityTile<Player2>")
    command("count Resource", "24 Resource<Player2>")
    command("mode blue", "Mode BLUE: Turn integrity: must perform a valid game turn for this phase")
    command(
        "turn",
        """
        New tasks pending:
        Z* [queue: Player2, assignee: Player2] UseAction<Player2, StandardAction>! OR Pass<Player2>! (abstract)
        """
            .trimIndent(),
    )
    command("task UseAction1<ConvertHeatSA>", "Can't remove 8 Heat<Player2>: max possible is 6")
    command("mode red", "Mode RED: Change integrity: make changes without triggered effects")
    command("exec 2 Heat", "0000: +2 Heat<Player2> BY Player2 (manual)")
    command(
        "task UseAction1<ConvertHeatSA>",
        """
        0000: -8 Heat<Player2> BY Player2 VIA ConvertHeatSA BECAUSE 0000
        0000: +TemperatureStep BY Player2 VIA ConvertHeatSA BECAUSE 0000
        0000: +TerraformRating<Player2> BY Player2 VIA TemperatureStep BECAUSE 0000
        """
            .trimIndent(),
    )
    command(
        "list GlobalParameter",
        """
        3 GlobalParameter:
          2 TemperatureStep
          1 VenusStep
        """
            .trimIndent(),
    )
  }

  @Test
  fun game20230521() {
    val repl = ScriptSession()
    val commands =
        """
        newgame BRMVPX 2; mode blue; auto safe; phase Corporation

        become P1; turn; tfm_play Manutech; task 5 BuyCard
        become P2; turn; tfm_play Factorum; task 4 BuyCard

        phase Prelude

        become P1
        turn; tfm_play NewPartner; tfm_play UnmiContractor
        turn; tfm_play AlliedBank

        become P2
        turn; tfm_play AcquiredSpaceAgency
        turn; tfm_play IoResearchOutpost

        phase Action

        become P1
        turn; task UseAction1<PlayCardSA>; tfm_play InventorsGuild; tfm_pay 9
        """
            .trimIndent()
            .split(Regex(" *[\n;] *"))
            .filter { it.isNotEmpty() }

    val expectedOutput =
        """
        New 2-player game created with options: BRMVPX
        Mode BLUE: Turn integrity: must perform a valid game turn for this phase
        Autoexec mode is: SAFE
        0000: +CorporationPhase FROM SetupPhase BY Engine (manual)
        0000: +CorporationCard<Player1> BY Engine VIA Player1 BECAUSE 0000
        0000: +CorporationCard<Player2> BY Engine VIA Player2 BECAUSE 0000
        0000: +Photosynthesis BY Engine VIA TerraformingMars BECAUSE 0000
        Hi, Player1
        New tasks pending:
        Z  [queue: Player1, assignee: Player1] PlayCard<Player1, Class<CorporationCard>>! (abstract)
        Z  [queue: Player1, assignee: Player1] 10 BuyCard<Player1>? (abstract)
        0000: +Manutech<Player1> FROM CorporationCard<Player1> BY Player1 VIA PlayCard<Player1, Class<CorporationCard>, Class<Manutech>> BECAUSE 0000
        0000: +BuildingTag<Player1, Manutech<Player1>> BY Player1 VIA Manutech<Player1> BECAUSE 0000
        0000: +Production<Player1, Class<Steel>> BY Player1 VIA Manutech<Player1> BECAUSE 0000
        0000: +35 Megacredit<Player1> BY Player1 VIA Manutech<Player1> BECAUSE 0000
        0000: +Steel<Player1> BY Player1 VIA Manutech<Player1> BECAUSE 0000
        0000: -15 Megacredit<Player1> BY Player1 VIA BuyCard<Player1> BECAUSE 0000
        0000: +5 ProjectCard<Player1> BY Player1 VIA BuyCard<Player1> BECAUSE 0000
        Hi, Player2
        New tasks pending:
        Z  [queue: Player2, assignee: Player2] PlayCard<Player2, Class<CorporationCard>>! (abstract)
        Z  [queue: Player2, assignee: Player2] 10 BuyCard<Player2>? (abstract)
        0000: +Factorum<Player2> FROM CorporationCard<Player2> BY Player2 VIA PlayCard<Player2, Class<CorporationCard>, Class<Factorum>> BECAUSE 0000
        0000: +PowerTag<Player2, Factorum<Player2>> BY Player2 VIA Factorum<Player2> BECAUSE 0000
        0000: +BuildingTag<Player2, Factorum<Player2>> BY Player2 VIA Factorum<Player2> BECAUSE 0000
        0000: +37 Megacredit<Player2> BY Player2 VIA Factorum<Player2> BECAUSE 0000
        0000: +Production<Player2, Class<Steel>> BY Player2 VIA Factorum<Player2> BECAUSE 0000
        0000: -12 Megacredit<Player2> BY Player2 VIA BuyCard<Player2> BECAUSE 0000
        0000: +4 ProjectCard<Player2> BY Player2 VIA BuyCard<Player2> BECAUSE 0000
        0000: +PreludePhase FROM CorporationPhase BY Engine (manual)
        0000: +2 PreludeCard<Player1> BY Engine VIA PreludeSetup<Player1> BECAUSE 0000
        0000: +2 PreludeCard<Player2> BY Engine VIA PreludeSetup<Player2> BECAUSE 0000
        Hi, Player1
        New tasks pending:
        Z* [queue: Player1, assignee: Player1] PlayCard<Player1, Class<PreludeCard>>! OR (-PreludeCard<Player1>! THEN 15 Megacredit<Player1>!) (abstract)
        0000: +NewPartner<Player1> FROM PreludeCard<Player1> BY Player1 VIA PlayCard<Player1, Class<PreludeCard>, Class<NewPartner>> BECAUSE 0000
        0000: +Production<Player1, Class<Megacredit>> BY Player1 VIA NewPartner<Player1> BECAUSE 0000
        0000: +PreludeCard<Player1> BY Player1 VIA NewPartner<Player1> BECAUSE 0000
        0000: +Megacredit<Player1> BY Player1 VIA Manutech<Player1> BECAUSE 0000

        New tasks pending:
        Z* [queue: Player1, assignee: Player1] PlayCard<Player1, Class<PreludeCard>>! (abstract)
        0000: +UnmiContractor<Player1> FROM PreludeCard<Player1> BY Player1 VIA PlayCard<Player1, Class<PreludeCard>, Class<UnmiContractor>> BECAUSE 0000
        0000: +EarthTag<Player1, UnmiContractor<Player1>> BY Player1 VIA UnmiContractor<Player1> BECAUSE 0000
        0000: +3 TerraformRating<Player1> BY Player1 VIA UnmiContractor<Player1> BECAUSE 0000
        0000: +ProjectCard<Player1> BY Player1 VIA UnmiContractor<Player1> BECAUSE 0000
        New tasks pending:
        Z* [queue: Player1, assignee: Player1] PlayCard<Player1, Class<PreludeCard>>! OR (-PreludeCard<Player1>! THEN 15 Megacredit<Player1>!) (abstract)
        0000: +AlliedBank<Player1> FROM PreludeCard<Player1> BY Player1 VIA PlayCard<Player1, Class<PreludeCard>, Class<AlliedBank>> BECAUSE 0000
        0000: +EarthTag<Player1, AlliedBank<Player1>> BY Player1 VIA AlliedBank<Player1> BECAUSE 0000
        0000: +4 Production<Player1, Class<Megacredit>> BY Player1 VIA AlliedBank<Player1> BECAUSE 0000
        0000: +3 Megacredit<Player1> BY Player1 VIA AlliedBank<Player1> BECAUSE 0000
        0000: +4 Megacredit<Player1> BY Player1 VIA Manutech<Player1> BECAUSE 0000
        Hi, Player2
        New tasks pending:
        Z* [queue: Player2, assignee: Player2] PlayCard<Player2, Class<PreludeCard>>! OR (-PreludeCard<Player2>! THEN 15 Megacredit<Player2>!) (abstract)
        0000: +AcquiredSpaceAgency<Player2> FROM PreludeCard<Player2> BY Player2 VIA PlayCard<Player2, Class<PreludeCard>, Class<AcquiredSpaceAgency>> BECAUSE 0000
        0000: +6 Titanium<Player2> BY Player2 VIA AcquiredSpaceAgency<Player2> BECAUSE 0000
        0000: +2 ProjectCard<Player2> BY Player2 VIA AcquiredSpaceAgency<Player2> BECAUSE 0000
        New tasks pending:
        Z* [queue: Player2, assignee: Player2] PlayCard<Player2, Class<PreludeCard>>! OR (-PreludeCard<Player2>! THEN 15 Megacredit<Player2>!) (abstract)
        0000: +IoResearchOutpost<Player2> FROM PreludeCard<Player2> BY Player2 VIA PlayCard<Player2, Class<PreludeCard>, Class<IoResearchOutpost>> BECAUSE 0000
        0000: +ScienceTag<Player2, IoResearchOutpost<Player2>> BY Player2 VIA IoResearchOutpost<Player2> BECAUSE 0000
        0000: +JovianTag<Player2, IoResearchOutpost<Player2>> BY Player2 VIA IoResearchOutpost<Player2> BECAUSE 0000
        0000: +Production<Player2, Class<Titanium>> BY Player2 VIA IoResearchOutpost<Player2> BECAUSE 0000
        0000: +ProjectCard<Player2> BY Player2 VIA IoResearchOutpost<Player2> BECAUSE 0000
        0000: +ActionPhase FROM PreludePhase BY Engine (manual)
        Hi, Player1
        New tasks pending:
        Z* [queue: Player1, assignee: Player1] UseAction<Player1, StandardAction>! OR Pass<Player1>! (abstract)
        New tasks pending:
        Z* [queue: Player1, assignee: Player1] PlayCard<Player1, Class<ProjectCard>>! (abstract)
        New tasks pending:
        Z* [queue: Player1, assignee: Player1] X Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1>? (abstract)
        Z  [queue: Player1, assignee: Player1] MAX 0 Barrier: InventorsGuild<Player1> FROM ProjectCard<Player1>!
        0000: +9 Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1> BY Player1 VIA Accept<Player1, Class<Megacredit>> BECAUSE 0000
        0000: +InventorsGuild<Player1> FROM ProjectCard<Player1> BY Player1 VIA PlayCard<Player1, Class<ProjectCard>, Class<InventorsGuild>> BECAUSE 0000
        0000: +ScienceTag<Player1, InventorsGuild<Player1>> BY Player1 VIA InventorsGuild<Player1> BECAUSE 0000
        """
            .trimIndent()
            .split("\n")

    // TODO The "MAX 0 Barrier" one should have said "(currently impossible)"
    // also why is there a random blank line up there??

    val output =
        commands.flatMap(repl::command).map {
          normalizeEventOrdinals(it).replace(letterRegex, "Z")
        }
    assertEquals(expectedOutput, output)
  }

  @Test
  fun test() {
    val repl = ScriptSession()
    repl.command("become Player2")
    repl.command("exec ProjectCard")

    assertEquals(
        listOf(
                "+5 Production<Player2, Class<Megacredit>> BY Player2 (manual)",
                "+4 Production<Player2, Class<Energy>> BY Player2 (manual)",
            )
            .sorted(),
        strip(repl.command("exec PROD[5, 4 Energy]")).sorted(),
    )
    val byCard = "BY Player2 VIA StripMine<Player2>"
    assertEquals(
        listOf(
                "+StripMine<Player2> BY Player2 (manual)",
                "+BuildingTag<Player2, StripMine<Player2>> $byCard",
                "-2 Production<Player2, Class<Energy>> $byCard",
                "+2 Production<Player2, Class<Steel>> $byCard",
                "+Production<Player2, Class<Titanium>> $byCard",
                "+OxygenStep $byCard",
                "+TerraformRating<Player2> BY Player2 VIA OxygenStep",
                "+OxygenStep $byCard",
                "+TerraformRating<Player2> BY Player2 VIA OxygenStep",
            )
            .sorted(),
        strip(repl.command("exec StripMine")).sorted(),
    )

    val check1 = "has PROD[=2 Energy, =2 Steel]"
    assertTrue(repl.command(check1).first().startsWith("true"))

    repl.command("become Player1")
    val check2 = "has PROD[=0 Energy, =0 Steel]"
    assertTrue(repl.command(check2).first().startsWith("true"))
  }

  @Test
  fun testBoard() {
    val repl = ScriptSession()
    repl.command("become Player1")
    repl.command("exec PROD[9, 8 Steel, 7 Titanium, 6 Plant, 5 Energy, 4 Heat]")
    repl.command("exec 8, 6 Steel, 7 Titanium, 5 Plant, 3 Energy, 9 Heat")

    val board =
        PlayerBoardToText(TfmGameplay(repl.game, repl.gameplay.actor, repl.gameplay), false).board()
    assertEquals(
        listOf(
            "  Player1   TR: 20   Tiles: 0",
            "+---------+---------+---------+",
            "|  M:   8 |  S:   6 |  T:   7 |",
            "| prod  9 | prod  8 | prod  7 |",
            "+---------+---------+---------+",
            "|  P:   5 |  E:   3    H:   9 |",
            "| prod  6 | prod  5 | prod  4 |",
            "+---------+---------+---------+",
        ),
        board,
    )
  }

  @Test
  fun testMap() {
    val repl = ScriptSession()
    repl.command("become Player1")
    repl.command("exec OT<M26>, OT<M55>, OT<M56>, CT<M46>, GT<M57>")
    repl.command("as Player2 exec GT<M45>, CT<M66>, MaTile<M99>")
    assertTrue(repl.command("tasks").isEmpty())
    assertEquals(8, repl.gameplay.count("Tile"))

    assertEquals(
        """
                                   1    2    3    4    5    6    7    8    9
                                  /    /    /    /    /    /    /    /    /

               1 -            LSS  WSS   L    WC   W

               2 -           L   VS    L    L    L   [O]

               3 -        VC   L    L    L    L    L    LS

               4 -     VPT  LP   LP   LP  [G2] [C1]  LP   WPP

               5 -  VPP  LPP  NPP  WPP  [O]  [O]  [G1] LPP  LPP

               6 -     LP   LPP  LP   LP  [C2]  WP   WP   WP

               7 -        L    L    L    L    L    LP   L

               8 -          LSS   L   LC   LC    L   LT

               9 -             LS  LSS   L    L   [S2]
            """
            .replaceIndent(" ")
            .split("\n")
            .map { it.trimEnd() },
        repl.command(TfmMapCommand(repl)),
    )
  }
}

fun strip(strings: Iterable<String>): List<String> {
  return strings.map { endRegex.replace(startRegex.replace(it, ""), "") }
}

private val startRegex = Regex("^[^:]+: ")
private val endRegex = Regex(" BECAUSE.*")
