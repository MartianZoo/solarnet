package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class DoubleDownTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.playCorp(PharmacyUnion, 5)
    admin.phase("Prelude")
    p1.playPrelude(BiosphereSupport)
  }

  @Test
  internal fun `Can copy Biosphere Support`() {
    p1.playPrelude(DoubleDown) { doTask("CopyPrelude<$BiosphereSupport>") }
        .expect("PROD[-1 MC, 0 Steel, 0 Titanium, 2 Plant, 0 Energy, 0 Heat]")
  }

  @Test
  internal fun `Can copy Board of Directors without holding its directors`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack)
    admin.phase("Prelude")
    p1.playPrelude(BoardOfDirectors)

    p1.playPrelude(DoubleDown) { doTask("CopyPrelude<$BoardOfDirectors>") }.expect("0 Director")
    p1.assertCounts(4 to "Director<$BoardOfDirectors>", 1 to "$DoubleDown")
  }

  @Test
  internal fun `Can copy Early Colonization's per-colony benefit`() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        PromoCardPack,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2, "Luna"),
    )
    admin.phase("Prelude")
    p1.playPrelude(EarlyColonization) { doTask("Colony<Luna>") }

    p1.playPrelude(DoubleDown) {
          doTask("CopyPrelude<$EarlyColonization>")
          doTask("Colony<Ceres>")
        }
        .expect("3 Energy, 2 ColonyProduction<Luna>")
  }

  @Test
  internal fun `Cannot copy an absent Prelude`() {
    p1.playPrelude(DoubleDown) {
      shouldThrow<DependencyException> { doTask("CopyPrelude<$MartianIndustries>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot copy another player's Prelude`() {
    requireP2().playPrelude(UnmiContractor)
    p1.playPrelude(DoubleDown) {
      shouldThrow<DependencyException> { doTask("CopyPrelude<$UnmiContractor>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot copy a corporation`() {
    p1.playPrelude(DoubleDown) {
      shouldThrow<NarrowingException> { doTask("CopyPrelude<$PharmacyUnion>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot copy itself`() {
    p1.playPrelude(DoubleDown) {
      shouldThrow<NarrowingException> { doTask("CopyPrelude<$DoubleDown>") }
      abort()
    }
  }

  @Test
  internal fun `Double Down itself fizzles when a fizzled Merger leaves no Prelude to copy`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack)
    p1.playCorp(ValleyTrust, 10)
    admin.phase("Prelude")
    // Seven MC plus Nirgal's thirty cannot pay Merger's forty-two MC cost.
    shouldThrow<LimitsException> { p1.playPrelude(Merger) { p1.playCorp(NirgalEnterprises) } }
    // The declared fizzle leaves no Prelude face for Double Down.
    p1.startTurn()
    p1.doTask("-PreludeCard").expect("15 MC")
    p1.assertCounts(0 to "$Merger", 0 to "CardFront<Class<PreludeCard>>")
    p1.playPrelude(DoubleDown) {
      shouldThrow<DependencyException> { doTask("CopyPrelude<$Merger>") }
      abort()
    }

    val checkpoint = game.timeline.checkpoint()
    p1.startTurn()
    p1.doTask("-PreludeCard").expect("15 MC")
    p1.assertCounts(37 to "MC", 0 to "PreludeCard", 0 to "$DoubleDown")
    p1.auditGainsSince(checkpoint) shouldBe 1
  }

  @Test
  internal fun `Double Down repeats immediate benefits without adding tags or triggering Point Luna`() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.playCorp(PointLuna, 0)
    admin.phase("Prelude")
    p1.playPrelude(AlliedBank).expect("3 MC, PROD[4 MC], ProjectCard, EarthTag")

    p1.playPrelude(DoubleDown) { doTask("CopyPrelude<$AlliedBank>") }
        .expect("3 MC, PROD[4 MC], 0 ProjectCard, 0 EarthTag")
  }

  @Test
  internal fun `Double Down repeats Preservation Program TR without adding another skip`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, Unsafe)
    p1.playCorp(CrediCor, 1)
    admin.phase("Prelude")
    p1.playPrelude(PreservationProgram)
    p1.playPrelude(DoubleDown) { doTask("CopyPrelude<$PreservationProgram>") }
        .expect("5 TerraformRating")
    admin.phase("Action")

    // Double Down repeats the benefit without adding a second skip.
    p1.playProject(Comet, 21) { placeTile(1, 2) }.expect("TerraformRating")
  }
}
