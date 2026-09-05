package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Player.Companion.PLAYER3
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior. */
internal class BugsTest : CardTest(additionalClassDeclarations = attributionProbeDeclarations) {
  // The rule belongs to Vermin, but it is hosted on one `VerminWatcher<Player>` per player, so
  // each victim ends up acting on itself. See docs/agents/EACHPLAYER.md, "Attribution is
  // inherited, and it matters", for the fanout that would put the rule back on the card.
  @Test
  internal fun `Vermin incorrectly credits each victim for removing its own point`() {
    newGame(PromoCardPack, players = 3)
    val p3 = game.tfm(PLAYER3)
    p1.manual("$Vermin, 10 Animal<$Vermin>, CityTile<Tharsis_2_1>, $attributionProbe")
    p3.manual("CityTile<Tharsis_3_3>")

    engine.manual("End FROM Phase")

    // The probe records whoever is credited for a point removal. These self-attributions are wrong:
    // Vermin's owner should be credited for both removals, whoever lost the point.
    engine.count("$attribution<Player1>") shouldBe 1
    engine.count("$attribution<Player3>") shouldBe 1
  }

  @Test
  internal fun `Ecology Experts incorrectly does not trigger Viral Enhancers with its own tags`() {
    newGame(PreludeExpansion, CorporateEraExpansion)
    engine.phase("Prelude")
    p1.manual("9 MC, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) { p1.playProject(ViralEnhancers, 9) }

    p1.assertCounts(1 to "Plant")
  }

  @Test
  internal fun `Ecology Experts incorrectly does not trigger Ecological Zone with its plant tag`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("12 MC, ProjectCard, PreludeCard, GreeneryTile<Tharsis_4_4>")

    p1.playPrelude(EcologyExperts) { p1.playProject(EcologicalZone, 12) { placeTile(4, 5) } }

    p1.assertCounts(2 to "Animal<$EcologicalZone>")
  }

  @Test
  internal fun `Mars University incorrectly allows two discards before either draw`() {
    newGame(CorporateEraExpansion)
    p1.manual(
        "5 ProjectCard, $MarsUniversity"
    ) { /* Decline Mars University's discard-and-draw effect. */
      declineTask()
    }
    val manual = p1.also { it.autoExecMode = NONE }

    manual
        .manual("$Research") {
          doTask("2 ProjectCard")
          doTask("-ProjectCard")
          doTask("-ProjectCard")
          doTask("ProjectCard")
          doTask("ProjectCard")
        }
        .expect("2 ProjectCard")
  }

  // https://boardgamegeek.com/thread/3361875/questions-about-the-head-start
  @Test
  internal fun `Head Start incorrectly allows its two actions to interleave`() {
    newGame(PreludeExpansion, TurmoilCardPack, PromoCardPack)
    p1.phase("Prelude")
    p1.manual("4 MC, 10 ProjectCard, PreludeCard, 10 Heat")

    p1.playPrelude(HeadStart) {
      p1.assertCounts(2 to "Steel", 24 to "MC")
      doTask("UseAction<ConvertHeat, Action1>")
      doTask("8 Pay<Class<Heat>> FROM Heat")
      doTask("UseAction<AquiferSP, Action1>")
      doTask("18 Pay<Class<MC>> FROM MC")
      placeTile(5, 5)
    }
  }

  @Test
  internal fun `Prelude incorrectly allows discarding a playable card`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    val moneyBefore = p1.count("MC")

    p1.startTurn()
    p1.doTask("-PreludeCard")
    p1.startTurn()
    p1.doTask("PlayCard<Class<PreludeCard>, Class<$DomeFarming>>")

    p1.assertCounts(1 to "$DomeFarming", 0 to "PreludeCard")
    p1.count("MC") shouldBe moneyBefore + 15
  }

  @Test
  internal fun `Law Suit incorrectly rejects a funded attacker when its owner has only two mc`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.autoExecMode = NONE
    p1.manual("2 MC, ProjectCard, PROD[Plant]")
    val p2 = requireP2()
    p2.manual("5 MC, PROD[-Plant<Player1>]")

    shouldThrow<TaskException> {
      p1.playProject(LawSuit, 2) {
        doTask("3 MC<Player1> FROM MC<Player2>")
      }
    }

    p1.assertCounts(2 to "MC", 1 to "ProjectCard")
    p2.assertCounts(5 to "MC")
  }

  @Test
  internal fun `Space Elevator incorrectly accepts payment that wastes one steel`() {
    newGame()
    engine.phase("Action")
    p1.manual("10 Steel, 10 Titanium, ProjectCard")

    p1.inTurn {
      doTask("UseAction<PlayCardFromHand, Action1>")
      doTask("PlayCard<Class<ProjectCard>, Class<$SpaceElevator>>")
      doTask("7 Pay<Class<Steel>> FROM Steel")
      doTask("5 Pay<Class<Titanium>> FROM Titanium")
      doTask("Ok")
    }

    p1.assertCounts(
        3 to "Steel",
        5 to "Titanium",
        0 to "ProjectCard",
        1 to "$SpaceElevator",
    )
  }
}
