package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.Elysium
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.CrediCor
import dev.martianzoo.tfm.tests.cards.cardnames.GiantSpaceMirror
import dev.martianzoo.tfm.tests.cards.cardnames.PowerGeneration
import dev.martianzoo.tfm.tests.cards.cardnames.SocietySupport
import dev.martianzoo.tfm.tests.cards.cardnames.Supercapacitors
import dev.martianzoo.tfm.tests.cards.cardnames.ThorGate
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SupercapacitorsTest : CardTest() {
  @Test
  internal fun `Can preserve no energy`() {
    newGame(PromoCardPack)
    p1.runOperation("PROD[3 Energy, 5 Heat], 3 Energy, 9 Heat, Supercapacitors")

    admin.phase("Production") {
      // Decline converting energy into heat.
      p1.declineTask()
    }

    p1.assertCounts(3 to "Energy", 17 to "Heat")
  }

  @Test
  internal fun `Can preserve all existing energy but not newly produced energy`() {
    newGame(PromoCardPack)
    p1.runOperation("PROD[3 Energy, 5 Heat], 3 Energy, 9 Heat, Supercapacitors")

    admin.phase("Production") { p1.doTask("3 Energy FROM Heat!") }

    p1.assertCounts(6 to "Energy", 14 to "Heat")
  }

  @Test
  internal fun `Industrialist scores energy retained through final production`() {
    newGame(
        Elysium,
        PreludeExpansion,
        PromoCardPack,
        startingProjects = listOf(1, 1),
    )
    val p2 = requireP2()
    p1.playCorp(CrediCor)
    p2.playCorp(ThorGate)
    admin.phase("Prelude")
    p1.playPrelude(PowerGeneration)
    p2.playPrelude(SocietySupport)
    admin.phase("Action")
    p1.playProject(Supercapacitors, 4)
    p2.playProject(GiantSpaceMirror, 14)
    p1.fundAward(cn("Industrialist"), 8)
    admin.nextGeneration(0, 0)
    admin.phase("Production") { p1.doTask("3 Energy FROM Heat!") }

    admin.runOperation("End FROM Phase")
    p1.count("FirstPlace<Industrialist>") shouldBe 1
    p2.count("FirstPlace<Industrialist>") shouldBe 0
    p1.count("Energy") shouldBe 6
  }
}
