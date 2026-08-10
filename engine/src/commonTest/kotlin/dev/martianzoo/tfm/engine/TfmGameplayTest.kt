package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.tfm.engine.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class TfmGameplayTest : CardTest() {
  @Test
  fun `declining a second action rejects an unrelated optional task`() {
    newGame()

    p1.manual("UseAction<StandardAction>?") {
      shouldThrow<TaskException> { p1.declineSecondAction() }
      abort()
    }
  }
}
