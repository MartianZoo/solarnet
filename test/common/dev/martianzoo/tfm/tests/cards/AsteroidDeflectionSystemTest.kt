package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.SystemClasses.AUDIT
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AsteroidDeflectionSystemTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Asteroids added by another card do not claim a revealed card`() {
    kim.exMachina("$AsteroidDeflectionSystem, $AsteroidRights")

    val result = kim.inTurn {
      doTasks("UseCardAction<AsteroidRights, Action1>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
      doTask("Asteroid<AsteroidDeflectionSystem>")
    }
    result.expect("Asteroid<$AsteroidDeflectionSystem>")
    result.changes.count { it.change.gaining?.className == AUDIT } shouldBe 0
  }

  @Test
  internal fun `Protects plants from another player's project card`() {
    kim.exMachina("$AsteroidDeflectionSystem")
    kim.setToExMachina(1, "Plant")

    shouldThrow<DeadEndException> {
      stan.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<Virus>>")
        selectTask("-X MC?")
        doTasks(
            "-1 MC",
            "Virus FROM ProjectCard",
            "LATER { PlayedEvent<Class<Virus>> FROM Virus. }",
        )
        selectOnlyTask()
        doTask("-Plant<Kim>")
      }
    }
    kim.assertCounts(1 to "Plant")
  }

  @Test
  internal fun `Its action claims a revealed space card`() {
    kim.exMachina("$AsteroidDeflectionSystem")

    val result = kim.inTurn {
      doTask("UseCardAction<AsteroidDeflectionSystem, Action1>")
      selectOnlyTask()
      selectOnlyTask()
      doTask("ClaimCardReward<TagFilter<Class<SpaceTag>>, AsteroidDeflectionSystem>")
      selectOnlyTask()
    }
    result.expect("Asteroid<$AsteroidDeflectionSystem>, 0 ProjectCard<Revealed>")
    result.changes.count { it.change.gaining?.className == AUDIT } shouldBe 1
  }
}
