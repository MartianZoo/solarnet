package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.Gameplay.TaskLayer
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameRecordingTest {
  @Test
  internal fun recordingSeeksAcrossCompletedOperationsAndNotifiesComponentListeners() {
    val game = Engine.newGame(canonicalPremise())
    val gameplay = game.gameplay(PLAYER1).also { it.autoExecMode = NONE }
    val tasks = gameplay.godMode() as TaskLayer
    val heat = game.reader.resolve(parse<Expression>("Heat<Player1>"))
    val observedCounts = mutableListOf<Int>()
    val subscription = game.components.listenToCount(heat, game.reader, observedCounts::add)

    tasks.beginManual("Heat?")
    gameplay.doTask("Heat!")
    val recording = game.recording()

    recording.positions.size shouldBe 3
    recording.positionIndex shouldBe 2
    observedCounts.shouldContainExactly(0, 1)

    val invalidPosition =
        (0 until recording.positions.last().ordinal).map(Timeline::Checkpoint).first {
          it !in recording.positions
        }
    shouldThrow<IllegalArgumentException> { game.timeline.rollBack(invalidPosition) }
    game.timeline.rollBack(recording.positions.first())
    gameplay.count("Heat<Player1>") shouldBe 0
    recording.seek(recording.positions.lastIndex)

    recording.seek(0)
    gameplay.count("Heat<Player1>") shouldBe 0
    recording.seek(1)
    gameplay.count("Heat<Player1>") shouldBe 0
    recording.seek(2)
    gameplay.count("Heat<Player1>") shouldBe 1
    observedCounts.shouldContainExactly(0, 1, 0, 1, 0, 1)

    subscription.cancel()
    recording.seek(0)
    observedCounts.shouldContainExactly(0, 1, 0, 1, 0, 1)
  }

  @Test
  internal fun automaticFollowUpWorkIsOneSeparateRecordedStep() {
    val game = Engine.newGame(canonicalPremise())
    val gameplay = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    var addAutomaticResources = true
    game.onAtomicComplete = {
      if (addAutomaticResources) {
        addAutomaticResources = false
        gameplay.manual("Plant")
        gameplay.manual("Steel")
      }
    }

    gameplay.manual("Heat")
    val recording = game.recording()

    recording.positions.size shouldBe 3
    recording.seek(1)
    gameplay.count("Heat<Player1>") shouldBe 1
    gameplay.count("Plant<Player1>") shouldBe 0
    gameplay.count("Steel<Player1>") shouldBe 0
    recording.seek(2)
    gameplay.count("Plant<Player1>") shouldBe 1
    gameplay.count("Steel<Player1>") shouldBe 1
  }
}
