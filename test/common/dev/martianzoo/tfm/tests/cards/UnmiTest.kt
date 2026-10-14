package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class UnmiTest : TfmSandboxTest() {
  @Test
  internal fun `Can use its action after raising TR`() {
    newTestGame(kimCorporation = UnitedNationsMarsInitiative)
    kim.setToExMachina(17, "MC")
    kim.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
      selectTask("TemperatureStep*")
      doTask("TerraformRating")
    }

    kim.inTurn {
          doTasks("UseCardAction<UnitedNationsMarsInitiative, Action1>", "-MC / Owed<Class<MC>>")
          selectOnlyTask()
        }
        .expect("TerraformRating")
  }

  @Test
  internal fun `Cannot use its action without raising TR`() {
    newTestGame(kimCorporation = UnitedNationsMarsInitiative)

    shouldThrow<RequirementException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseCardAction<UnitedNationsMarsInitiative, Action1>")
        selectTask("-X? MC")
        doTask("-MC / Owed<Class<MC>>")
        selectOnlyTask()
      }
    }
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Can use UNMI acquired after an earlier TR gain in the generation`() {
      newTestGame(addOptions = "PreludeExpansion", playerCount = 2, kimCorporation = CrediCor)
      kim.doTask("PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>")
      kim.autoExecNow(EAGER)
      kim.completeOperation {
        doTask("PlayCard<Class<PreludeCard>, Class<$Merger>>")
        doTask("PlayCard<Class<CorporationCard>, Class<$UnitedNationsMarsInitiative>>")
        autoExecNow(EAGER)
      }
      stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
      stan.autoExecNow(EAGER)

      stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
      stan.autoExecNow(EAGER)

      kim.doTasks("UseCardAction<$UnitedNationsMarsInitiative, Action1>", "-3 MC")
          .expect("TerraformRating")
    }

    @Test
    internal fun `UNMI Contractor TR during Prelude qualifies UNMI in generation one`() {
      newTestGame(
          addOptions = "PreludeExpansion",
          playerCount = 2,
          kimCorporation = UnitedNationsMarsInitiative,
      )
      kim.doTask("PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>")
      kim.autoExecNow(EAGER)
      kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
      stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
      stan.autoExecNow(EAGER)

      stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
      stan.autoExecNow(EAGER)

      kim.doTasks("UseCardAction<$UnitedNationsMarsInitiative, Action1>", "-3 MC")
          .expect("TerraformRating")
    }
  }
}
