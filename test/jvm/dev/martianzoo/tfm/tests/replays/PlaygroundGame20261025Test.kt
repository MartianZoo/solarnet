package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.GameFileImporter
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

internal class PlaygroundGame20261025Test {
  @Test
  fun playerDecisionsReproduceTheCompletedGame() = runBlocking {
    val source =
        checkNotNull(javaClass.getResourceAsStream("/replays/PlaygroundGame20261025.short.json.gz"))
    val document =
        GameFileJson.parse(GZIPInputStream(source).bufferedReader().use { it.readText() })
    val agents =
        GameFileImporter.replay(
            document,
            Canon,
            newAgents = { TfmEngine.agents(TfmEngine.newGame(it)) },
            automaticAdmin = true,
            startGame = { agents ->
              agents[ADMIN].beginOperation("SetupPhase FROM BootstrapPhase")
              // Neutral setup placements recorded by the Playground's seeded dealer.
              agents[ADMIN].doTask("NormalCityTile<SoloOpponent, Hellas_2_2>")
              agents[ADMIN].doTask("GreeneryTile<SoloOpponent, Hellas_3_3>")
              agents[ADMIN].doTask("NormalCityTile<SoloOpponent, Hellas_7_8>")
              agents[ADMIN].doTask("GreeneryTile<SoloOpponent, Hellas_7_9>")
            },
        )
    val green = agents[agents.world.actors.filterIsInstance<Player>().single()]
    // Expected values are transcribed from the user's 2026-10-25 23:50:27 dashboard screenshot.
    green.assertCounts(
        74 to "TerraformRating",
        0 to "ProjectCard",
        6 to "PlayedEvent",
        96 to "MC",
        7 to "Steel",
        5 to "Titanium",
        5 to "Plant",
        0 to "Energy",
        15 to "Heat",
        0 to "WildTag",
        151 to "VictoryPoint",
        1 to "Victory",
        0 to "GameScope",
        1 to "GreatDamPromo_SpecialTile<Hellas_3_7>",
        1 to "DeimosDownPromo_SpecialTile<Hellas_9_7>",
    )
    green.assertTags(
        but = 8,
        spt = 10,
        sct = 7,
        pot = 5,
        eat = 2,
        jot = 3,
        vet = 4,
        plt = 9,
        cit = 1,
    )
    green.assertProds(
        22 to "MC",
        1 to "Steel",
        4 to "Titanium",
        16 to "Plant",
        0 to "Energy",
        8 to "Heat",
    )
    assertTrue(agents.world.tasks.isEmpty(), "The game must finish its final scoring")

    // 23:51:46 map screenshot: suffix 1 denotes Green; neutral tiles have no suffix.
    // Both special tiles render as [S1]; their identities and locations are checked above.
    val expectedMap =
        """
        |                      1     2     3     4     5     6     7     8     9
        |                     /     /     /     /     /     /     /     /     /
        |
        | 1 -             [O]   [G1]  [C1]  [G1]  [G1]
        |
        | 2 -          [G1]  [C]   [G1]  [G1]  [C1]  [G1]
        |
        | 3 -        WP   [G1]  [G]   [C1]  [G1]  [G1]  [S1]
        |
        | 4 -     WP   [G1]  [C1]  [G1]  [G1]  [O]   [O]    LP
        |
        | 5 -  LC    L    [G1]  [G1]  [C1]  [O]   [O]   [O]    LP
        |
        | 6 -     LT    L    [C1]  [G1]  [O]   [O]   [O]    L
        |
        | 7 -       WTT   [G1]  [G1]  [C1]  [G1]  [C]   [G]
        |
        | 8 -           LS   [C1]  [G1]  LHH    LT    LT
        |
        | 9 -              L    LHH   [S1]  LHH    L
        """
            .trimMargin()
    assertEquals(
        expectedMap,
        TfmMapRenderer(
                agents.world.reader,
                agents.world.actors.filterIsInstance<Player>(),
                useAnsiColors = false,
            )
            .render()
            .joinToString("\n"),
    )
  }
}
