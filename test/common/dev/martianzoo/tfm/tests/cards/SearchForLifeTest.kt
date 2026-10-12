package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.SystemClasses.AUDIT
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SearchForLifeTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Successful search adds science and records the revealed card`() {
    kim.exMachina("$SearchForLife")

    val result = kim.inTurn {
      doTasks("UseCardAction<SearchForLife, Action1>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
      selectOnlyTask()
      doTask("ClaimCardReward<TagFilter<Class<MicrobeTag>>, SearchForLife>")
      selectOnlyTask()
    }
    result.expect("-MC, Science<$SearchForLife>, 0 ProjectCard<Revealed>")
    result.changes.count { it.change.gaining?.className == AUDIT } shouldBe 1
  }

  @Test
  internal fun `Failed search discards the revealed card without science`() {
    kim.exMachina("$SearchForLife")

    val result = kim.inTurn {
      doTasks("UseCardAction<SearchForLife, Action1>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
      selectOnlyTask()
      doTask("Ok")
      selectOnlyTask()
    }
    result.expect("-MC, 0 Science<$SearchForLife>, 0 ProjectCard<Revealed>")
    result.changes.count { it.change.gaining?.className == AUDIT } shouldBe 0
  }
}
