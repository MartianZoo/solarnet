package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class IoSulphurResearchTest : CardTest() {
  @Test
  internal fun `May choose the lesser card draw with three Venus tags`() {
    newGame(VenusNextExpansion)
    admin.phase("Action")
    p1.runOperation("$AerialMappers, $DeuteriumExport, $Dirigibles")
    p1.runOperation("17 MC, ProjectCard")

    p1.playProject(IoSulphurResearch, 17) { doTask("ProjectCard") }.expect("0 ProjectCard")
  }
}
