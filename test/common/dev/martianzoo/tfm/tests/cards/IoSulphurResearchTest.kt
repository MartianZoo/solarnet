package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class IoSulphurResearchTest : ProjectCardTest() {
  @Test
  internal fun `Can choose the lesser card draw despite three Venus tags`() {
    kim.exMachina("$AerialMappers, $DeuteriumExport, $Dirigibles")

    kim.playProject(IoSulphurResearch, 17) { doTask("ProjectCard") }.expect("0 ProjectCard")
  }
}
