package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.fakeWildTags
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class InterplanetaryTradeTest : CardTest() {
  @Test
  internal fun `Counts three existing tag types and adds four production`() {
    newGame(PromoCardPack)
    // These have to be played: tags depend on their cards.
    p1.runOperation("$Ecoline, $Mine, $SearchForLife, 8 Plant, 6 Steel, 4 Heat, 3 ProjectCard")
    p1.runOperation("$InterplanetaryTrade").expect("PROD[4 MC]")
  }

  @Test
  internal fun `Does not count a tag from a played event`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("100 MC, 2 ProjectCard, $Ecoline, $Mine, $SearchForLife")
    p1.playProject(ImportedHydrogen, 16) {
      doTask("3 Plant")
      placeTile(1, 2)
    }

    p1.playProject(InterplanetaryTrade, 27).expect("PROD[4 MC]")
  }

  @Test
  internal fun `An explicitly assigned wild Event tag raises Interplanetary Trade without Venus`() {
    interplanetaryTradeWithWildEvent(false)
  }

  @Test
  internal fun `An explicitly assigned wild Event tag raises Interplanetary Trade with Venus`() {
    interplanetaryTradeWithWildEvent(true)
  }

  private fun interplanetaryTradeWithWildEvent(venus: Boolean) {
    newGame(
        GameConfig(
            "PreludeExpansion, PromoCardPack, CorporateEraExpansion, FakeStuffBundle, " +
                "EcologyExperts, Unsafe" +
                if (venus) ", VenusNextExpansion" else "",
            "Player1",
            "Player2",
        )
    )
    p1.playCorp(SaturnSystems, 0)
    p1.runOperation("200 MC, 10 ProjectCard")
    admin.phase("Prelude")
    with(p1) { playPrelude(EcologyExperts) { playProject(Research, 11) } }
    p1.playPrelude(FakeResearchNetwork)
    admin.phase("Action")
    p1.playProject(Pets, 10)
    p1.playProject(PowerPlant, 4)
    p1.playProject(ImmigrantCity, 13) { placeTile(1, 1) }
    if (venus) p1.playProject(FloatingHabs, 5)

    // Six supporting card faces cover the nine ordinary types; Trade supplies Space itself.
    with(p1) {
      runOperation("${fakeWildTags("EventTag")}, NewTurn") {
            useStdAction("PlayCardFromHandAction", payment = {}) {
              playProject(InterplanetaryTrade, 27)
            }
          }
          .expect("PROD[${if (venus) 12 else 11} MC]")
    }
    p1.count("FakeWildTagUse") shouldBe 0
  }
}
