package dev.martianzoo.tfm.web.playground

import kotlin.test.Test
import kotlin.test.assertEquals

internal class SavedGameSetupTest {
  @Test
  fun storedSetupRestoresTheExactOptionsPlayerAndShuffleSeed() {
    val setup =
        SavedGameSetup(
            listOf("TerraformingMars", "CorporateEraExpansion", "promos:cards", "TharsisMap"),
            "Green",
            0x1a2b3c4d,
        )
    assertEquals("1P - TM,CEE,PC,TM - 0x1a2b3c4d", setup.label)

    rememberGameSetup(setup)
    assertEquals(setup, savedGameSetups().first())

    val draft = NewGameDraft()
    draft.recall(savedGameSetups().first())
    assertEquals(setup.entries, draft.entries.toList())
    assertEquals(setup.playerName, draft.playerName)
    assertEquals(formatPlaySeed(setup.seed), draft.seedText)
  }

  @Test
  fun choosingAnotherSoloObjectiveReplacesTheFirst() {
    val draft = NewGameDraft()
    draft.chooseSoloObjective("StandardSoloObjective")
    draft.chooseSoloObjective("Tr63SoloObjective")

    assertEquals(1, draft.entries.count { it.endsWith("SoloObjective") })
    assertEquals("Tr63SoloObjective", draft.entries.single { it.endsWith("SoloObjective") })
  }
}
