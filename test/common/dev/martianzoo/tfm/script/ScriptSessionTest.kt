package dev.martianzoo.tfm.script

import dev.martianzoo.script.ScriptSession
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.script.commands.TfmBoardCommand.PlayerBoardToText
import dev.martianzoo.tfm.script.commands.TfmMapCommand
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
  internal fun playerSnapshotTracksResourcesProductionAndTags() {
    val repl = ScriptSession()

    repl.command("become Player1")
    repl.command("mode red")
    val initialSteelProduction =
        repl.playerSnapshot().resources.single { it.name == "Steel" }.production
    repl.command("exec 5 MC, 7 Steel, PROD[2 Steel]")

    val snapshot = repl.playerSnapshot()
    assertEquals(20, snapshot.terraformRating)
    assertEquals(5, snapshot.resources.single { it.name == "Megacredit" }.stock)
    assertEquals(7, snapshot.resources.single { it.name == "Steel" }.stock)
    assertEquals(
        initialSteelProduction + 2,
        snapshot.resources.single { it.name == "Steel" }.production,
    )
    assertTrue(snapshot.tags.none { it.name == "venus" })

    repl.command("tfm_sample A 0")
    val sampleSnapshot = repl.playerSnapshot()
    assertEquals(1, sampleSnapshot.tags.single { it.name == "building" }.count)
    assertEquals(2, sampleSnapshot.tags.single { it.name == "earth" }.count)

    repl.command("newgame BMV 2")
    assertEquals(0, repl.playerSnapshot().tags.single { it.name == "venus" }.count)
  }

  @Test
  internal fun mapSnapshotTracksAreaTypesBonusesAndTiles() {
    val repl = ScriptSession()
    val emptyMap = repl.mapSnapshot()

    assertEquals("Tharsis", emptyMap.name)
    assertEquals(61, emptyMap.areas.size)
    assertEquals(12, emptyMap.areas.count { it.kind == "water" })
    assertEquals(listOf("P", "P"), emptyMap.areas.single { it.row == 5 && it.column == 3 }.bonuses)

    repl.command("become Player1")
    repl.command("mode red")
    repl.command("exec CityTile<Tharsis_5_3>")

    val noctis = repl.mapSnapshot().areas.single { it.row == 5 && it.column == 3 }
    assertEquals("noctis", noctis.kind)
    assertEquals("city", noctis.tile)
    assertEquals("Player1", noctis.owner)

    repl.command("newgame BRH 2")
    val hellas = repl.mapSnapshot()
    assertEquals(
        listOf("H", "H", "H"),
        hellas.areas.single { it.row == 5 && it.column == 7 }.bonuses,
    )
    assertEquals(listOf("O", "6"), hellas.areas.single { it.row == 9 && it.column == 7 }.bonuses)
  }

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
        ScriptSession().command("exec RequiredAction { -> 3 ProjectCard }"),
    )
  }

  @Test
  internal fun `as Engine temporarily selects the Engine actor`() {
    val repl = ScriptSession()
    repl.command("newgame B 2")
    repl.command("become Player1")

    assertEquals(listOf("1 Phase"), repl.command("as Engine count Phase"))
    assertEquals("Player1", repl.agent.actor.toString())
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
                "-CorporateEraExpansion, -WorldGovernmentRule; players: Player1, Player2",
            "Purple mode: workflow active",
        ),
        repl.command(
            "newgame \"MultiplayerMode, TerraformingMars, TharsisMap, VenusNextExpansion, " +
                "-CorporateEraExpansion, -WorldGovernmentRule\" Player1 Player2 purple"
        ),
    )
    assertEquals(listOf("0 WorldGovernmentRule"), repl.command("count WorldGovernmentRule"))
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
    assertEquals("Player1", repl.agent.actor.toString())
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
  internal fun logRendersLinkedComponentTypesMinimally() {
    val repl = ScriptSession()
    repl.command("mode red")
    repl.command("as Player1 exec NitriteReducingBacteria<Player1>")
    repl.command(
        "as Player1 exec " +
            "Microbe<Player1, " +
            "NitriteReducingBacteria<Player1, Class<ProjectCard>, Class<Microbe>>>"
    )

    listOf(repl.command("log"), repl.command("log full")).forEach { output ->
      assertTrue(
          output.any {
            "+Microbe<NitriteReducingBacteria<Player1>> BY Player1 (manual)" in it
          }
      )
      assertTrue(output.none { "+Microbe<Player1, NitriteReducingBacteria" in it })
    }
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
        "task UseAction<ConvertHeat, Action1>",
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
        0000: +TemperatureStep BY Player2 VIA ConvertHeat BECAUSE 0000
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

        become Player1; turn; tfm_play Manutech; task -5 ProjectCard<Selecting>; task 15 Pay<Class<MC>> FROM MC
        become Player2; turn; tfm_play Factorum; task -6 ProjectCard<Selecting>; task 12 Pay<Class<MC>> FROM MC

        phase Prelude

        become Player1
        turn; tfm_play NewPartner; tfm_play UnmiContractor
        turn; tfm_play AlliedBank

        become Player2
        turn; tfm_play AcquiredSpaceAgency
        turn; tfm_play IoResearchOutpost

        phase Action

        become Player1
        turn; task UseAction<PlayCardFromHand, Action1>; tfm_play InventorsGuild; tfm_pay 9 MC
        """
            .trimIndent()
            .split(Regex(" *[\n;] *"))
            .filter { it.isNotEmpty() }

    val expectedPreamble =
        listOf(
            "New 2-player game created with options: BRVPX",
            "Mode BLUE: Turn integrity: must perform a valid game turn for this phase",
            "Autoexec mode is: SAFE",
            "0000: +CorporationPhase FROM SetupPhase BY Engine (manual)",
        )

    val output = commands.flatMap(repl::command).map(::normalizeEventOrdinals)
    assertEquals(expectedPreamble, output.take(4))
    assertContains(
        output,
        "0000: +5 ProjectCard<Hand<Player1>> FROM ProjectCard<Selecting<Player1>> BY Player1 VIA BuySelectedCards<Player1> BECAUSE 0000",
    )
    assertContains(
        output,
        "0000: +4 ProjectCard<Hand<Player2>> FROM ProjectCard<Selecting<Player2>> BY Player2 VIA BuySelectedCards<Player2> BECAUSE 0000",
    )
    assertTrue(
        output.none {
          "can't narrow" in it || "select-lock" in it || it.startsWith("pending tasks:")
        }
    )
    assertEquals(0, repl.agent.count("ProjectCard<Player1, Selecting<Player1>>"))
    assertEquals(0, repl.agent.count("ProjectCard<Player2, Selecting<Player2>>"))
    assertEquals(1, repl.agent.count("InventorsGuild<Player1>"))
  }

  @Test
  internal fun test() {
    val repl = ScriptSession()
    repl.command("become Player2")
    repl.command("exec ProjectCard")

    assertEquals(
        listOf(
                "+5 Production<Player2, Class<MC>> BY Player2 (manual)",
                "+4 Production<Player2, Class<Energy>> BY Player2 (manual)",
            )
            .sorted(),
        strip(repl.command("exec PROD[5 MC, 4 Energy]")).sorted(),
    )
    val byCard = "BY Player2 VIA StripMine<Player2>"
    assertEquals(
        listOf(
                "+StripMine<Player2> BY Player2 (manual)",
                "+BuildingTag<StripMine<Player2>> $byCard",
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
    repl.command("exec PROD[9 MC, 8 Steel, 7 Titanium, 6 Plant, 5 Energy, 4 Heat]")
    repl.command("exec 8 MC, 6 Steel, 7 Titanium, 5 Plant, 3 Energy, 9 Heat")

    val board =
        PlayerBoardToText(
                TfmGameplay(repl.game, repl.agent.actor, repl.agent),
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
    assertEquals(8, repl.agent.count("Tile"))

    assertEquals(
        """
        |                       1     2     3     4     5     6     7     8     9
        |                      /     /     /     /     /     /     /     /     /
        |
        | 1 -              LSS   WSS    L     WC    W
        |
        | 2 -            L     VS    L     L     L    [O]
        |
        | 3 -         VC    L     L     L     L     L     LS
        |
        | 4 -     VPT    LP    LP    LP   [G2]  [C1]   LP   WPP
        |
        | 5 -  VPP   LPP   NPP   WPP   [O]   [O]   [G1]  LPP   LPP
        |
        | 6 -      LP   LPP    LP    LP   [C2]   WP    WP    WP
        |
        | 7 -         L     L     L     L     L     LP    L
        |
        | 8 -           LSS    L     LC    LC    L     LT
        |
        | 9 -               LS   LSS    L     L    [S2]
        """
            .trimMargin()
            .split("\n"),
        repl.command(TfmMapCommand(repl)),
    )
  }
}

private fun strip(strings: Iterable<String>): List<String> {
  return strings.map { endRegex.replace(startRegex.replace(it, ""), "") }
}

private val startRegex = Regex("^[^:]+: ")
private val endRegex = Regex(" BECAUSE.*")
