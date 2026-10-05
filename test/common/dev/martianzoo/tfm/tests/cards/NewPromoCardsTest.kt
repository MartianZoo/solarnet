package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class NewPromoCardsTest : CardTest() {
  @Test
  internal fun `Solar Logistics draws for space events played by its owner and either opponent`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.testTfm(PLAYER3)
    p1.runOperation("$SolarLogistics")

    p1.runOperation("$ImportedGhg")
    p1.count("ProjectCard") shouldBe 1
    p2.runOperation("$TechnologyDemonstration")
    p1.count("ProjectCard") shouldBe 2
    p3.runOperation("$InterstellarColonyShip")

    p1.count("ProjectCard") shouldBe 3
  }

  @Test
  internal fun `Icy Impactors lets the first player choose an ocean placed by the card owner`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    val oceanArea = "Tharsis_2_6"
    p2.runOperation("$IcyImpactors, Asteroid<$IcyImpactors>")
    admin.phase("Action")
    p2.cardAction2(IcyImpactors) {
      p1.doTask("OceanTile<$oceanArea> BY Player2")
    }

    p2.count("TerraformRating") shouldBe 21
    p1.count("TerraformRating") shouldBe 20
    p2.count("ProjectCard") shouldBe 2
    p1.count("ProjectCard") shouldBe 0
  }

  @Test
  internal fun `Icy Impactors owner chooses their own ocean when they are first player`() {
    newGame(PromoCardPack)
    p1.runOperation("$IcyImpactors, Asteroid<$IcyImpactors>")
    admin.phase("Action")
    p1.cardAction2(IcyImpactors) {
      doTask("OceanTile<Tharsis_1_2> BY Player1")
    }

    p1.count("TerraformRating") shouldBe 21
  }

  @Test
  internal fun `Icy Impactors owner controls when a third-player first player chooses`() {
    newGame(PromoCardPack, players = 3)
    val p3 = game.testTfm(PLAYER3)
    admin.runOperation("StartToken<Player3> FROM StartToken<Player1>")
    p1.runOperation("$IcyImpactors, Asteroid<$IcyImpactors>")
    admin.phase("Action")
    val previousAutoExecPolicy = p1.autoExecPolicy
    p1.autoExecPolicy = NONE

    try {
      p1.cardAction2(IcyImpactors) {
        val signal = tasks.ids().single()
        p1.selectTask(signal)
        val placement = tasks.ids().single()
        p1.selectTask(placement)
        val ocean = tasks.ids().single()
        shouldThrow<TaskException> { p3.doTask("OceanTile<Tharsis_1_2> BY Player1") }
        p1.selectTask(ocean)
        p3.doTask("OceanTile<Tharsis_1_2> BY Player1")
        shouldThrow<TaskException> { p3.doTask("TerraformRating<Player1>") }
        doTask("2 Steel")
        doTask("TerraformRating")
        p1.autoExecPolicy = previousAutoExecPolicy
      }
    } finally {
      p1.autoExecPolicy = previousAutoExecPolicy
    }

    p1.count("TerraformRating") shouldBe 21
    p3.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `Carbon Nanosystems can spend multiple graphene on one space card`() {
    newGame(PromoCardPack)

    admin.phase("Action")
    p1.runOperation("25 MC, 2 ProjectCard")

    p1.playProject(CarbonNanosystems, 14).expect("Graphene<$CarbonNanosystems>")
    p1.runOperation("Graphene<$CarbonNanosystems>")

    p1.playProject(IcyImpactors, 7) {
          doTask("-2 Graphene<$CarbonNanosystems>")
        }
        .expect("-2 Graphene<$CarbonNanosystems>")
  }

  @Test
  internal fun `Martian Lumber Corporation plants can pay for a building card`() {
    newGame(PromoCardPack)

    admin.phase("Action")
    p1.runOperation("ProjectCard, $MartianLumberCorp, 2 Plant, 20 MC")
    p1.playProject(Mine, 1) {
          doTask("-Plant")
        }
        .expect("-Plant")
  }

  @Test
  internal fun `Neptunian option remains required until its card leaves play`() {
    newGame(PromoCardPack)
    p1.runOperation("$NeptunianPowerConsultants")

    shouldThrow<LimitsException> {
      p1.runOperation("-NeptunianOption<$NeptunianPowerConsultants>!")
    }
    p1.count("NeptunianOption<$NeptunianPowerConsultants>") shouldBe 1

    p1.runOperation("-$NeptunianPowerConsultants")
    p1.count("NeptunianOption") shouldBe 0
  }

  @Test
  internal fun `Neptunian Power Consultants may pay for its ocean bonus with steel`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("50 MC, 2 ProjectCard")
    p1.playProject(NeptunianPowerConsultants, 14)

    p1.stdProject("AquiferProject") {
      doTask("OceanTile<Tharsis_1_2>")
      doTask("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>")
      p1.pay(mc = 1, steel = 2)
    }

    p1.assertCounts(17 to "MC", 0 to "Steel", 1 to "Hydroelectric")
    p1.assertProds(1 to "Energy")
  }

  @Test
  internal fun `Neptunian owner chooses and pays when an opponent places the ocean`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.autoExecPolicy = NONE
    admin.phase("Action")
    p1.runOperation("50 MC, 2 ProjectCard")
    p2.runOperation("20 MC")
    p1.playProject(NeptunianPowerConsultants, 14)
    p1.autoExecPolicy = NONE
    val ownerMcBeforeOcean = p1.count("MC")

    p2.stdProject("AquiferProject") {
      doTask("OceanTile<Tharsis_1_2>")
      p2.selectTask("UseAction<Player1, NeptunianOption<NeptunianPowerConsultants<Player1>>>?")
      p1.doTask("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>")
      p1.pay(5)
      p2.autoExecPolicy = EAGER
    }

    p1.assertCounts(ownerMcBeforeOcean - 5 to "MC", 1 to "Hydroelectric")
    p1.assertProds(1 to "Energy")
  }

  @Test
  internal fun `Homeostasis Bureau lets each actor raise temperature`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.runOperation("$HomeostasisBureau")
    p1.count("MC") shouldBe 0

    p2.runOperation("TemperatureStep")
    admin.runOperation("TemperatureStep")
    p1.count("MC") shouldBe 0

    p1.runOperation("TemperatureStep").expect("3 MC")
  }

  @Test
  internal fun `Kaguya Tech can replace a greenery with its city`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("10 MC, ProjectCard, GreeneryTile<Tharsis_4_2>")
    p1.playProject(KaguyaTech, 10) {
          shouldThrow<NarrowingException> {
            doTask("CityTile<Tharsis_4_3> FROM GreeneryTile<Tharsis_4_2>")
          }
          doTask("CityTile<Tharsis_4_2> FROM GreeneryTile<Tharsis_4_2>")
        }
        .expect("-GreeneryTile<Tharsis_4_2>, CityTile<Tharsis_4_2>")
  }

  @Test
  internal fun `Kaguya Tech can replace a Protected Valley greenery with its city`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("33 MC, 2 ProjectCard")
    p1.playProject(ProtectedValley, 23) { placeTile(1, 2) }

    p1.playProject(KaguyaTech, 10) {
          doTask("CityTile<Tharsis_1_2> FROM GreeneryTile<Tharsis_1_2>")
        }
        .expect("-GreeneryTile<Tharsis_1_2>, CityTile<Tharsis_1_2>")
  }

  @Test
  internal fun `Mission can target an opponent's normal city by area with Capital in play`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.runOperation("14 MC, ProjectCard")
    p2.runOperation("2 MC, PROD[2 Energy]")
    p2.runOperation("CityTile<Player2, Tharsis_4_2>")
    p2.runOperation("$Capital") { placeTile(2, 5) }
    admin.phase("Action")

    p1.playProject(StJosephOfCupertinoMission, 7)
    p1.cardAction1(StJosephOfCupertinoMission) {
      p1.pay(5)
      val wrongHolder = shouldThrow<Exception> { doTask("Cathedral<CityTile<Tharsis_4_2>>") }
      wrongHolder.message.orEmpty() shouldContain "missing dependencies"
      wrongHolder.message.orEmpty() shouldContain "CityTile<Player1, Tharsis_4_2>"
      val emptyArea = shouldThrow<Exception> { doTask("Cathedral<CityTile<Anyone, Tharsis_4_3>>") }
      emptyArea.message.orEmpty() shouldContain "missing dependencies"
      emptyArea.message.orEmpty() shouldContain "Tharsis_4_3"
      p1.count("Cathedral") shouldBe 0
      p1.count("MC") shouldBe 2
      doTask("Cathedral<CityTile<Anyone, Tharsis_4_2>>")
      p2.doTask("UseAction<CathedralOption, Action1>")
      p2.pay(2)
    }

    p1.assertCounts(2 to "MC", 0 to "ProjectCard")
    p1.assertCounts(1 to "Cathedral<Player1, NormalCityTile<Player2, Tharsis_4_2>>")
    p2.assertCounts(1 to "ProjectCard")
    admin.runOperation("End FROM Phase")
    p1.assertCounts(21 to "VictoryPoint")
  }

  @Test
  internal fun `Mission can target Capital's city by area`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.runOperation("12 MC, ProjectCard")
    p2.runOperation("2 MC, PROD[2 Energy]")
    p2.runOperation("$Capital") { placeTile(2, 5) }
    admin.phase("Action")

    p1.playProject(StJosephOfCupertinoMission, 7)
    p1.cardAction1(StJosephOfCupertinoMission) {
      p1.pay(5)
      doTask("Cathedral<CityTile<Anyone, Tharsis_2_5>>")
      p2.doTask("UseAction<CathedralOption, Action1>")
      p2.pay(2)
    }

    p1.assertCounts(1 to "Cathedral<Player1, CapitalTile<Player2, Tharsis_2_5>>")
    p2.assertCounts(1 to "ProjectCard")
  }

  @Test
  internal fun `St Joseph of Cupertino Mission can place a Cathedral on a neutral solo city`() {
    newGame(PromoCardPack, players = 1)
    p1.runOperation("12 MC, ProjectCard")
    admin.phase("Action")

    p1.playProject(StJosephOfCupertinoMission, 7)
    p1.cardAction1(StJosephOfCupertinoMission) {
      p1.pay(5)
      doTask("Cathedral<CityTile<Anyone, Tharsis_4_1>>")
    }

    p1.assertCounts(1 to "Cathedral<Player1, CityTile<SoloOpponent, Tharsis_4_1>>")
    game.isIdle() shouldBe true
  }

  @Test
  internal fun `Red Ships counts each city or special tile beside an ocean`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("$RedShips, CityTile<Tharsis_1_3>, OceanTile<Tharsis_1_2>")
    p1.runOperation("MiningRights_SpecialTile<Tharsis_2_2>")

    p1.cardAction1(RedShips).expect("2 MC")
  }

  @Test
  internal fun `Kaguya creates a new Philares adjacency without renewing an Arcadian reservation`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.playCorp(ArcadianCommunities, 1)
    p2.playCorp(Philares, 0)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") { doTask("Community<Tharsis_4_2>") }
    p2.stdAction("DoRequiredActionsAction") { placeTile(4, 1) }
    p1.stdProject("GreeneryProject") {
      placeTile(4, 2)
      p2.doTask("Steel")
    }
    p1.count("Community<Tharsis_4_2>") shouldBe 0
    p1.playProject(KaguyaTech, 10) {
          doTask("CityTile<Tharsis_4_2> FROM GreeneryTile<Tharsis_4_2>")
          p2.doTask("Titanium")
        }
        .expect("-10 MC, Titanium<Player2>")
  }

  @Test
  internal fun `Neptunian Power Consultants may decline an affordable ocean bonus`() {
    declineNeptunianBonus(5)
  }

  @Test
  internal fun `An unaffordable Neptunian bonus does not block an opponent's ocean`() {
    declineNeptunianBonus(0)
  }

  private fun declineNeptunianBonus(spareMc: Int) {
    newGame(PromoCardPack)
    val p2 = requireP2()
    admin.phase("Action")
    p1.runOperation("${14 + spareMc} MC, ProjectCard")
    p2.runOperation("18 MC")
    p1.playProject(NeptunianPowerConsultants, 14)

    p2.stdProject("AquiferProject") {
          placeTile(1, 2)
          p1.declineTask()
        }
        .expect(
            "OceanTile, TerraformRating<Player2>, PROD[0 Energy<Player1>], " +
                "0 Hydroelectric<Player1>, 0 MC<Player1>"
        )
  }

  @Test
  internal fun `Cathedrals allow steel payment and an optional draw but do not score as resources`() {
    newGame(Hellas, PromoCardPack)
    val p2 = requireP2()
    p1.playCorp(CrediCor, 1)
    p2.playCorp(MiningGuild, 1)
    admin.phase("Action")
    p1.runOperation("2 Steel")
    p1.stdProject("CityProject") { placeTile(4, 2) }
    p2.playProject(Pets, 10)
    p1.playProject(StJosephOfCupertinoMission, 7)

    p1.cardAction1(StJosephOfCupertinoMission) {
          p1.pay(mc = 1, steel = 2)
          doTask("Cathedral<CityTile<Anyone, Hellas_4_2>>")
          // Decline paying 2 MC for the city owner's card draw.
          declineTask()
        }
        .expect("Cathedral, -2 Steel, -1 MC, 0 ProjectCard")

    p1.fundAward(cn("Excentric"), 8)
    admin.runOperation("End FROM Phase")
    p1.count("FirstPlace<Excentric>") shouldBe 0
    p2.count("FirstPlace<Excentric>") shouldBe 1
  }

  @Test
  internal fun `CEO's Favorite Project cannot add a resource to a Cathedral marker`() {
    newGame(PromoCardPack, VenusNextExpansion)
    p1.runOperation(
        "$StJosephOfCupertinoMission, $SearchForLife, Science<$SearchForLife>, " +
            "CityTile<Tharsis_4_2>, 5 MC"
    )
    admin.phase("Action")
    p1.cardAction1(StJosephOfCupertinoMission) {
      p1.pay(5)
      doTask("Cathedral<CityTile<Anyone, Tharsis_4_2>>")
      declineTask()
    }

    p1.runOperation("$CeosFavoriteProject") {
      shouldThrow<ExpressionException> {
        doTask("CardResource<$StJosephOfCupertinoMission>")
      }
      doTask("Science<$SearchForLife>")
    }

    p1.count("Cathedral") shouldBe 1
    p1.count("Science<$SearchForLife>") shouldBe 2
  }
}
