package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MassiveDiscountsTest : CardTest() {

  @Test
  internal fun `printed tags and resource value grants remain required while their card is in play`() {
    newGame(CorporateEraExpansion, PromoCardPack)
    p1.runOperation("$AdvancedAlloys, $MercurianAlloys")

    shouldThrow<LimitsException> {
      p1.runOperation("-GrantedResourceValue<Class<Titanium>, $AdvancedAlloys>!")
    }
    p1.count("ResourceValue<Class<Titanium>>") shouldBe 5
    shouldThrow<LimitsException> { p1.runOperation("-ScienceTag<$AdvancedAlloys>!") }
    shouldThrow<LimitsException> { p1.runOperation("ScienceTag<$AdvancedAlloys>!") }
    p1.count("ScienceTag<$AdvancedAlloys>") shouldBe 1

    p1.runOperation("-$AdvancedAlloys")
    p1.count("ResourceValue<Class<Titanium>>") shouldBe 4
    p1.count("ResourceValue<Class<Steel>>") shouldBe 2
    p1.count("ScienceTag<$AdvancedAlloys>") shouldBe 0
  }

  @Test
  internal fun `Stacks with other card discounts`() {
    newGame(VenusNextExpansion, PreludeExpansion, PromoCardPack)

    admin.phase("Action")
    p1.runOperation(
        "5 MC, 2 ProjectCard, Steel, Titanium, $AntiGravityTechnology, $EarthCatapult, " +
            "$ResearchOutpost, $MassConverter, $QuantumExtractor, $Shuttles, $SpaceStation, " +
            "$AdvancedAlloys, $PhoboLog, $MercurianAlloys, $RegoPlastics"
    ) {
      placeTile(4, 2)
    }

    p1.playProject(SpaceElevator, 4, steel = 1, titanium = 1).expect("-4 MC, -Steel, -Titanium")
  }

  @Test
  internal fun `A discount applies once for each matching printed tag`() {
    newGame(CorporateEraExpansion, ColoniesExpansion)
    admin.phase("Action")
    p1.runOperation("$EarthOffice, $AcquiredCompany, $MediaGroup, ProjectCard, 4 MC")

    p1.playProject(LunaGovernor, 0).expect("PROD[2 MC]")
  }
}
