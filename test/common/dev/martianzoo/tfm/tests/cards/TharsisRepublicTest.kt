package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TharsisRepublicTest : TfmSandboxTest() {
  @Test
  internal fun `Original solo corporation gains production for the two opponent cities`() {
    newTestGameBeforeCorporationPlay(playerCount = 1)

    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<TharsisRepublic>>",
          "TharsisRepublic FROM CorporationCard",
          "40 MC",
      )
      doTasks("PROD[2 MC]", "15 Pay<> FROM MC", "5 ProjectCard<Hand FROM Selecting>")
    }
  }

  // Unresolved adjudication (question 25, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3745041
  @Test
  internal fun `Does not gain the solo starting bonus when acquired through Merger`() {
    newTestGame(addOptions = "PreludeExpansion", playerCount = 1, kimCorporation = CrediCor)

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
          doTask("4 CorporationCard<Kim, Selecting>")
          doTask("PlayCard<Class<CorporationCard>, Class<TharsisRepublic>>")
          doTask("TharsisRepublic<Kim> FROM CorporationCard<Kim, Selecting>!")
          doTasks("40 MC", "-42 MC")
        }
        .expect("PROD[0 MC]")
  }
}
