package dev.martianzoo.tfm.tests

import dev.martianzoo.pets.api.Exceptions.TaskException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

/** Tests the handoff helper itself, using supplied tasks rather than card rules. */
internal class GiveTaskTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  fun `The recipient identifies the handoff and chooses its outcome`() {
    kim.addTasks("NOW Stan { MC OR Steel }, NOW Rob { Plant }")
    kim.giveTask(stan, "Steel").expect("Steel<Stan>")
    kim.giveTask(rob, "Plant").expect("Plant<Rob>")
    kim.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `An ambiguous handoff changes nothing until identified explicitly`() {
    kim.addTasks("NOW Stan { MC }, NOW Stan { Steel }")
    val pending = kim.tasks.extract { it }
    shouldThrow<TaskException> { kim.giveTask(stan, "Steel") }
    kim.tasks.extract { it } shouldBe pending
    stan.tasks.isEmpty() shouldBe true
    kim.giveTask(stan, "Steel", "Steel").expect("Steel<Stan>")
    kim.giveTask(stan, "MC").expect("MC<Stan>")
  }

  @Test
  fun `Operation scopes reject a missing handoff and accept a unique choice`() {
    kim.addTasks("NOW Stan { MC OR Steel }")
    kim.continueOperation {
          shouldThrow<TaskException> { giveTask(rob, "MC") }
          giveTask(stan, "MC")
        }
        .expect("MC<Stan>")
  }

  @Test
  fun `Giving a task leaves its later stage for a separate submission`() {
    kim.addTasks("NOW Stan { MC THEN Steel }")
    kim.giveTask(stan, "MC").expect("MC<Stan>, 0 Steel<Stan>")
    stan.doTask("Steel").expect("Steel<Stan>")
  }

  @Test
  fun `Other dispatch types and deferred handoffs do not compete with NOW`() {
    kim.addTasks("SOON Stan { MC }, NOW Stan { Steel }, LATER Kim { NOW Stan { Plant } }")
    kim.giveTask(stan, "Steel").expect("Steel<Stan>, 0 MC<Stan>, 0 Plant<Stan>")
    kim.tasks.ids().size shouldBe 2
  }
}
