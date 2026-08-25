package dev.martianzoo.script.tfm

import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.tfm.commands.TfmBoardCommand.PlayerBoardToText
import dev.martianzoo.script.tfm.commands.TfmMapCommand
import dev.martianzoo.tfm.engine.TfmGameplay
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class ScriptSessionTest {
  private val eventOrdinalRegex = Regex("^\\d+(?=:)")
  private val causeOrdinalRegex = Regex("(?<=BECAUSE )\\d+")

  private fun normalizeEventOrdinals(line: String) =
      line.replace(eventOrdinalRegex, "0000").replace(causeOrdinalRegex, "0000")

  @Test
  internal fun ansiColorsCanBeEnabledByTheHost() {
    val repl = ScriptSession(useAnsiColors = true)

    assertTrue(repl.prompt().contains("\u001B["))
    assertTrue(repl.command("tfm_board Player1").any { it.contains("\u001B[") })
    assertTrue(repl.command("tfm_map").any { it.contains("\u001B[") })
  }

  @Test
  internal fun descIncludesCanonicalAndAlternateClassNames() {
    val description = ScriptSession().command("desc Birds").single()

    assertContains(description, "Class `Birds`:")
    assertContains(description, "alt name:    Birds")
  }

  @Test
  internal fun descDescribesAnCatalogKnownInactiveType() {
    val description = ScriptSession().command("desc VenusTag").single()

    assertContains(description, "Class `VenusTag`:")
    assertContains(description, "cmpt types:  0")
  }

  @Test
  internal fun execReportsThatOwnerLocalClassesCannotBeAddedToALiveGame() {
    assertEquals(
        listOf("New Class declarations are not allowed after the Class Table is frozen"),
        ScriptSession().command("exec Mandate { -> 3 ProjectCard }"),
    )
  }

  @Test
  internal fun `as Engine temporarily selects the Engine actor`() {
    val repl = ScriptSession()
    repl.command("newgame B 2")
    repl.command("become Player1")

    assertEquals(listOf("1 Phase"), repl.command("as Engine count Phase"))
    assertEquals("Player1", repl.gameplay.actor.toString())
  }

  @Test
  internal fun optionCodesSelectCanonicalOptionsDirectly() {
    val repl = ScriptSession()

    assertEquals(listOf("0 CorporateEraExpansion"), repl.command("count CorporateEraExpansion"))
    repl.command("newgame BR 2")
    assertEquals(listOf("1 CorporateEraExpansion"), repl.command("count CorporateEraExpansion"))
    assertEquals(listOf("1 TharsisMap"), repl.command("count TharsisMap"))
  }

  @Test
  internal fun optionCodesRequireBaseAndDoNotAcceptSolo() {
    val repl = ScriptSession()

    assertTrue(repl.command("newgame R 2").any { it.contains("include B") })
    assertTrue(repl.command("newgame BSEI 1").any { it.contains("supported option codes") })
  }

  @Test
  internal fun quotedSignedClassNamesConfigureTheGame() {
    val repl = ScriptSession()

    assertEquals(
        listOf(
            "New 2-player game created with config: " +
                "MultiplayerMode, TerraformingMars, TharsisMap, VenusNextExpansion, " +
                "-CorporateEraExpansion, -WorldGovernmentOption; players: Player1, Player2",
            "Purple mode: workflow active",
        ),
        repl.command(
            "newgame \"MultiplayerMode, TerraformingMars, TharsisMap, VenusNextExpansion, " +
                "-CorporateEraExpansion, -WorldGovernmentOption\" Player1 Player2 purple"
        ),
    )
    assertEquals(listOf("0 WorldGovernmentOption"), repl.command("count WorldGovernmentOption"))
    assertEquals(listOf("1 CorporationPhase"), repl.command("count CorporationPhase"))
  }

  @Test
  internal fun shortPlayerNamesAliasCanonicalPlayerClasses() {
    val repl = ScriptSession()

    assertEquals(
        listOf("New 2-player game created with config: TerraformingMars; players: P1, P2"),
        repl.command("newgame \"TerraformingMars\" P1 P2"),
    )
    assertEquals(listOf("Hi, P1"), repl.command("become P1"))
    assertEquals("Player1", repl.gameplay.actor.toString())
    assertEquals(listOf("Hi, P2"), repl.command("become P2"))
  }

  @Test
  internal fun countReadsCountProperties() {
    val repl = ScriptSession()
    repl.command("newgame BH 2")

    assertEquals(
        listOf("8 Hellas_8_4.row"),
        repl.command("count Hellas_8_4.row"),
    )
  }

  @Test
  internal fun failedNewGameLeavesTheCurrentGameUntouched() {
    val repl = ScriptSession()
    val originalGame = repl.game
    val originalOptionCodes = repl.optionCodes
    val originalPlayerCount = repl.playerCount
    val originalPrompt = repl.promptPlain()

    assertTrue(repl.command("newgame BU 6").first().contains("between 1 and 5"))

    assertSame(originalGame, repl.game)
    assertEquals(originalOptionCodes, repl.optionCodes)
    assertEquals(originalPlayerCount, repl.playerCount)
    assertEquals(originalPrompt, repl.promptPlain())
  }

  @Test
  internal fun newGameCollectsColonySelectionBeforeCreatingTheGame() {
    val repl = ScriptSession()

    assertEquals(
        listOf("New 2-player game created with options: BRCX"),
        repl.command("newgame BRCX 2 Ceres Io Titan Luna Pluto"),
    )
    assertEquals(listOf("1 Ceres"), repl.command("count Ceres"))
    assertEquals(listOf("1 Io"), repl.command("count Io"))
    assertEquals(listOf("1 Luna"), repl.command("count Luna"))
    assertEquals(listOf("1 Pluto"), repl.command("count Pluto"))
    assertEquals(
        listOf("1 DelayedTitan"),
        repl.command("count DelayedTitan"),
    )
    repl.command("phase Corporation")
  }

  @Test
  internal fun purpleModeUsesColoniesSelectedBeforeGameplaySetup() {
    val repl = ScriptSession()

    assertEquals(
        listOf(
            "New 1-player game created with options: BRC",
            "Purple mode: workflow active",
        ),
        repl.command("newgame BRC 1 Ceres Io Luna Titan purple"),
    )
    repl.command("as Me task -ColonyTileSelection<Class<Luna>>")
    repl.command("task -6 TerraformRating<Me>")
    assertEquals(
        2,
        repl.game.tasks.extract { it.instruction.toString() }.count { it.startsWith("CityTile") },
    )
    repl.command("task CityTile<Tharsis_2_4, SoloOpponent>")
    repl.command("task GreeneryTile<Tharsis_2_3, SoloOpponent>")
    repl.command("task CityTile<Tharsis_8_7, SoloOpponent>")
    repl.command("task GreeneryTile<Tharsis_8_6, SoloOpponent>")

    assertEquals(listOf("1 CorporationPhase"), repl.command("count CorporationPhase"))
    assertEquals(listOf("1 Ceres"), repl.command("count Ceres"))
    assertEquals(listOf("1 Io"), repl.command("count Io"))
    assertEquals(
        listOf("1 DelayedTitan"),
        repl.command("count DelayedTitan"),
    )
  }

  @Test
  internal fun testBasicRunthrough() {
    val repl = ScriptSession()

    fun command(c: String, expected: String) {
      val results = repl.command(c).map(::normalizeEventOrdinals)
      assertEquals(expected.split("\n"), results)
    }

    command("newgame BRPX 3", "New 3-player game created with options: BRPX")
    command("tfm_sample A 3", "Okay, did that.")
    command(
        "tfm_board Player1",
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
    command("become Player2", "Hi, Player2")
    command("count CityTile", "0 CityTile<Player2>")
    command("count Resource", "24 Resource<Player2>")
    command("mode blue", "Mode BLUE: Turn integrity: must perform a valid game turn for this phase")
    command(
        "turn",
        """
        New tasks pending:
        * [Player2] UseAction<Player2, StandardAction>! OR Pass<Player2>! (abstract)
        """
            .trimIndent(),
    )
    command(
        "task UseAction<ConvertHeatSA, First>",
        """
        New tasks pending:
        * [Player2] X Pay<Player2, Class<Heat>> FROM Heat<Player2>? (abstract)
        """
            .trimIndent(),
    )
    command(
        "tfm_pay 8 Heat",
        "Can't transmute 8 Heat<Player2> into Pay<Player2, Class<Heat>>: max possible is 6",
    )
    command("mode red", "Mode RED: Change integrity: make changes without triggered effects")
    command("exec 2 Heat", "0000: +2 Heat<Player2> BY Player2 (manual)")
    command(
        "tfm_pay 8 Heat",
        """
        0000: +8 Pay<Player2, Class<Heat>> FROM Heat<Player2> BY Player2 VIA Accept<Player2, Class<Heat>> BECAUSE 0000
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
  internal fun game20230521() {
    val repl = ScriptSession()
    val commands =
        """
        newgame BRVPX 2; mode blue; auto safe; phase Corporation

        become Player1; turn; tfm_play Manutech; task 5 BuyCard; task 15 Pay<Class<Megacredit>> FROM Megacredit
        become Player2; turn; tfm_play Factorum; task 4 BuyCard; task 12 Pay<Class<Megacredit>> FROM Megacredit

        phase Prelude

        become Player1
        turn; tfm_play NewPartner; tfm_play UnmiContractor
        turn; tfm_play AlliedBank

        become Player2
        turn; tfm_play AcquiredSpaceAgency
        turn; tfm_play IoResearchOutpost

        phase Action

        become Player1
        turn; task UseAction<PlayCardSA, First>; tfm_play InventorsGuild; tfm_pay 9
        """
            .trimIndent()
            .split(Regex(" *[\n;] *"))
            .filter { it.isNotEmpty() }

    val expectedOutput =
        """
        New 2-player game created with options: BRVPX
        Mode BLUE: Turn integrity: must perform a valid game turn for this phase
        Autoexec mode is: SAFE
        0000: +CorporationPhase FROM SetupPhase BY Engine (manual)
        0000: +CorporationCard<Player1> BY Player1 VIA Player1 BECAUSE 0000
        0000: +CorporationCard<Player2> BY Player2 VIA Player2 BECAUSE 0000
        0000: +Photosynthesis BY Engine VIA TerraformingMars BECAUSE 0000
        Hi, Player1
        New tasks pending:
        [Player1] PlayCard<Player1, Class<CorporationCard>>! (abstract)
        [Player1] 10 BuyCard<Player1>? (abstract)
        0000: +Manutech<Player1, Class<CorporationCard>> FROM CorporationCard<Player1> BY Player1 VIA PlayCard<Player1, Class<CorporationCard>, Class<Manutech>> BECAUSE 0000
        0000: +BuildingTag<Player1, Manutech<Player1, Class<CorporationCard>>> BY Player1 VIA Manutech<Player1> BECAUSE 0000
        0000: +35 Megacredit<Player1> BY Player1 VIA Manutech<Player1> BECAUSE 0000
        0000: +Production<Player1, Class<Steel>> BY Player1 VIA Manutech<Player1> BECAUSE 0000
        0000: +Steel<Player1> BY Player1 VIA Manutech<Player1> BECAUSE 0000
        New tasks pending:
        * [Player1] X Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1>? (abstract)
        [Player1] MAX 0 Invoice<Player1>: 5 ProjectCard<Player1>!
        0000: +15 Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1> BY Player1 VIA Accept<Player1, Class<Megacredit>> BECAUSE 0000
        0000: +5 ProjectCard<Player1> BY Player1 VIA BuyCard<Player1> BECAUSE 0000
        Hi, Player2
        New tasks pending:
        [Player2] PlayCard<Player2, Class<CorporationCard>>! (abstract)
        [Player2] 10 BuyCard<Player2>? (abstract)
        0000: +Factorum<Player2, Class<CorporationCard>> FROM CorporationCard<Player2> BY Player2 VIA PlayCard<Player2, Class<CorporationCard>, Class<Factorum>> BECAUSE 0000
        0000: +PowerTag<Player2, Factorum<Player2, Class<CorporationCard>>> BY Player2 VIA Factorum<Player2> BECAUSE 0000
        0000: +BuildingTag<Player2, Factorum<Player2, Class<CorporationCard>>> BY Player2 VIA Factorum<Player2> BECAUSE 0000
        0000: +37 Megacredit<Player2> BY Player2 VIA Factorum<Player2> BECAUSE 0000
        0000: +Production<Player2, Class<Steel>> BY Player2 VIA Factorum<Player2> BECAUSE 0000
        New tasks pending:
        * [Player2] X Pay<Player2, Class<Megacredit>> FROM Megacredit<Player2>? (abstract)
        [Player2] MAX 0 Invoice<Player2>: 4 ProjectCard<Player2>!
        0000: +12 Pay<Player2, Class<Megacredit>> FROM Megacredit<Player2> BY Player2 VIA Accept<Player2, Class<Megacredit>> BECAUSE 0000
        0000: +4 ProjectCard<Player2> BY Player2 VIA BuyCard<Player2> BECAUSE 0000
        0000: +PreludePhase FROM CorporationPhase BY Engine (manual)
        0000: +2 PreludeCard<Player1> BY Player1 VIA PreludeSetup<Player1> BECAUSE 0000
        0000: +2 PreludeCard<Player2> BY Player2 VIA PreludeSetup<Player2> BECAUSE 0000
        Hi, Player1
        New tasks pending:
        * [Player1] PlayCard<Player1, Class<PreludeCard>>! OR (-PreludeCard<Player1>! THEN 15 Megacredit<Player1>!) (abstract)
        0000: +NewPartner<Player1, Class<PreludeCard>> FROM PreludeCard<Player1> BY Player1 VIA PlayCard<Player1, Class<PreludeCard>, Class<NewPartner>> BECAUSE 0000
        0000: +Production<Player1, Class<Megacredit>> BY Player1 VIA NewPartner<Player1> BECAUSE 0000
        0000: +2 PreludeCard<Player1> BY Player1 VIA NewPartner<Player1> BECAUSE 0000
        0000: +Megacredit<Player1> BY Player1 VIA Manutech<Player1> BECAUSE 0000
        0000: -PreludeCard<Player1> BY Player1 VIA NewPartner<Player1> BECAUSE 0000

        New tasks pending:
        * [Player1] PlayCard<Player1, Class<PreludeCard>>! (abstract)
        0000: +UnmiContractor<Player1, Class<PreludeCard>> FROM PreludeCard<Player1> BY Player1 VIA PlayCard<Player1, Class<PreludeCard>, Class<UnmiContractor>> BECAUSE 0000
        0000: +EarthTag<Player1, UnmiContractor<Player1, Class<PreludeCard>>> BY Player1 VIA UnmiContractor<Player1> BECAUSE 0000
        0000: +3 TerraformRating<Player1> BY Player1 VIA UnmiContractor<Player1> BECAUSE 0000
        0000: +ProjectCard<Player1> BY Player1 VIA UnmiContractor<Player1> BECAUSE 0000
        New tasks pending:
        * [Player1] PlayCard<Player1, Class<PreludeCard>>! OR (-PreludeCard<Player1>! THEN 15 Megacredit<Player1>!) (abstract)
        0000: +AlliedBank<Player1, Class<PreludeCard>> FROM PreludeCard<Player1> BY Player1 VIA PlayCard<Player1, Class<PreludeCard>, Class<AlliedBank>> BECAUSE 0000
        0000: +EarthTag<Player1, AlliedBank<Player1, Class<PreludeCard>>> BY Player1 VIA AlliedBank<Player1> BECAUSE 0000
        0000: +4 Production<Player1, Class<Megacredit>> BY Player1 VIA AlliedBank<Player1> BECAUSE 0000
        0000: +3 Megacredit<Player1> BY Player1 VIA AlliedBank<Player1> BECAUSE 0000
        0000: +4 Megacredit<Player1> BY Player1 VIA Manutech<Player1> BECAUSE 0000
        Hi, Player2
        New tasks pending:
        * [Player2] PlayCard<Player2, Class<PreludeCard>>! OR (-PreludeCard<Player2>! THEN 15 Megacredit<Player2>!) (abstract)
        0000: +AcquiredSpaceAgency<Player2, Class<PreludeCard>> FROM PreludeCard<Player2> BY Player2 VIA PlayCard<Player2, Class<PreludeCard>, Class<AcquiredSpaceAgency>> BECAUSE 0000
        0000: +6 Titanium<Player2> BY Player2 VIA AcquiredSpaceAgency<Player2> BECAUSE 0000
        0000: +2 ProjectCard<Player2> BY Player2 VIA AcquiredSpaceAgency<Player2> BECAUSE 0000
        New tasks pending:
        * [Player2] PlayCard<Player2, Class<PreludeCard>>! OR (-PreludeCard<Player2>! THEN 15 Megacredit<Player2>!) (abstract)
        0000: +IoResearchOutpost<Player2, Class<PreludeCard>> FROM PreludeCard<Player2> BY Player2 VIA PlayCard<Player2, Class<PreludeCard>, Class<IoResearchOutpost>> BECAUSE 0000
        0000: +ScienceTag<Player2, IoResearchOutpost<Player2, Class<PreludeCard>>> BY Player2 VIA IoResearchOutpost<Player2> BECAUSE 0000
        0000: +JovianTag<Player2, IoResearchOutpost<Player2, Class<PreludeCard>>> BY Player2 VIA IoResearchOutpost<Player2> BECAUSE 0000
        0000: +Production<Player2, Class<Titanium>> BY Player2 VIA IoResearchOutpost<Player2> BECAUSE 0000
        0000: +ProjectCard<Player2> BY Player2 VIA IoResearchOutpost<Player2> BECAUSE 0000
        0000: +ActionPhase FROM PreludePhase BY Engine (manual)
        Hi, Player1
        New tasks pending:
        * [Player1] UseAction<Player1, StandardAction>! OR Pass<Player1>! (abstract)
        New tasks pending:
        * [Player1] PlayCard<Player1, Class<ProjectCard>>! (abstract)
        New tasks pending:
        * [Player1] X Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1>? (abstract)
        [Player1] MAX 0 Barrier: InventorsGuild<Player1> FROM ProjectCard<Player1>!
        0000: +9 Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1> BY Player1 VIA Accept<Player1, Class<Megacredit>> BECAUSE 0000
        0000: +InventorsGuild<Player1, Class<ProjectCard>> FROM ProjectCard<Player1> BY Player1 VIA PlayCard<Player1, Class<ProjectCard>, Class<InventorsGuild>> BECAUSE 0000
        0000: +ScienceTag<Player1, InventorsGuild<Player1, Class<ProjectCard>>> BY Player1 VIA InventorsGuild<Player1> BECAUSE 0000
        """
            .trimIndent()
            .split("\n")

    // TODO The "MAX 0 Barrier" one should have said "(currently impossible)"
    // also why is there a random blank line up there??

    val output = commands.flatMap(repl::command).map(::normalizeEventOrdinals)
    assertEquals(expectedOutput, output)
  }

  @Test
  internal fun test() {
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
                "+StripMine<Player2, Class<ProjectCard>> BY Player2 (manual)",
                "+BuildingTag<Player2, StripMine<Player2, Class<ProjectCard>>> $byCard",
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

    val check1 = "has PROD[=3 Energy, =3 Steel]"
    assertTrue(repl.command(check1).first().startsWith("true"))

    repl.command("become Player1")
    val check2 = "has PROD[=1 Energy, =1 Steel]"
    assertTrue(repl.command(check2).first().startsWith("true"))
  }

  @Test
  internal fun testBoard() {
    val repl = ScriptSession()
    repl.command("become Player1")
    repl.command("exec PROD[9, 8 Steel, 7 Titanium, 6 Plant, 5 Energy, 4 Heat]")
    repl.command("exec 8, 6 Steel, 7 Titanium, 5 Plant, 3 Energy, 9 Heat")

    val board =
        PlayerBoardToText(
                TfmGameplay(repl.game, repl.gameplay.actor, repl.gameplay),
                false,
            )
            .board()
    assertEquals(
        listOf(
            "  Player1   TR: 20   Tiles: 0",
            "+---------+---------+---------+",
            "|  M:   8 |  S:   6 |  T:   7 |",
            "| prod 10 | prod  9 | prod  8 |",
            "+---------+---------+---------+",
            "|  P:   5 |  E:   3    H:   9 |",
            "| prod  7 | prod  6 | prod  5 |",
            "+---------+---------+---------+",
        ),
        board,
    )
  }

  @Test
  internal fun testMap() {
    val repl = ScriptSession()
    repl.command("become Player1")
    repl.command(
        "exec OceanTile<Tharsis_2_6>, OceanTile<Tharsis_5_5>, OceanTile<Tharsis_5_6>, " +
            "CityTile<Tharsis_4_6>, GreeneryTile<Tharsis_5_7>"
    )
    repl.command(
        "as Player2 exec GreeneryTile<Tharsis_4_5>, CityTile<Tharsis_6_6>, " +
            "MoholeArea_SpecialTile<Tharsis_9_9>"
    )
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

private fun strip(strings: Iterable<String>): List<String> {
  return strings.map { endRegex.replace(startRegex.replace(it, ""), "") }
}

private val startRegex = Regex("^[^:]+: ")
private val endRegex = Regex(" BECAUSE.*")
