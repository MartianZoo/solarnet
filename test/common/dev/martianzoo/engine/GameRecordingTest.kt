package dev.martianzoo.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameRecordingTest {
  @Test
  internal fun recordingSeeksAcrossCompletedOperationsAndNotifiesComponentListeners() {
    val game = Engine.newGame(canonicalPremise())
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val tasks = agent as Agent
    val heat = game.reader.resolve(parse<Expression>("Heat<Player1>"))
    val observedCounts = mutableListOf<Int>()
    val subscription = game.components.listenToCount(heat, game.reader, observedCounts::add)

    tasks.beginOperation("Heat?")
    agent.doTask("Heat!")
    val recording = game.recording()
    val playback = recording.open()
    val playbackHeat = playback.world.reader.resolve(parse<Expression>("Heat<Player1>"))
    val playbackCounts = mutableListOf<Int>()
    val playbackSubscription =
        playback.world.components.listenToCount(
            playbackHeat,
            playback.world.reader,
            playbackCounts::add,
        )

    playback.positions.size shouldBe 3
    playback.positionIndex shouldBe 2
    observedCounts.shouldContainExactly(0, 1)

    shouldThrow<IllegalArgumentException> { playback.seek(-1) }

    playback.seek(0)
    playback.world.reader.count(playbackHeat) shouldBe 0
    playback.seek(1)
    playback.world.reader.count(playbackHeat) shouldBe 0
    playback.seek(2)
    playback.world.reader.count(playbackHeat) shouldBe 1
    playbackCounts.shouldContainExactly(1, 0, 1)
    agent.count("Heat<Player1>") shouldBe 1
    observedCounts.shouldContainExactly(0, 1)

    subscription.cancel()
    playbackSubscription.cancel()
  }

  @Test
  internal fun automaticFollowUpWorkIsOneSeparateRecordedStep() {
    val game = Engine.newGame(canonicalPremise())
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    var addAutomaticResources = true
    game.onTransactionComplete = {
      if (addAutomaticResources) {
        addAutomaticResources = false
        agent.runOperation("Plant")
        agent.runOperation("Steel")
      }
    }

    agent.runOperation("Heat")
    val playback = game.recording().open()

    playback.positions.size shouldBe 3
    playback.seek(1)
    playback.world.reader.count(playback.world.reader.resolve(parse("Heat<Player1>"))) shouldBe 1
    playback.world.reader.count(playback.world.reader.resolve(parse("Plant<Player1>"))) shouldBe 0
    playback.world.reader.count(playback.world.reader.resolve(parse("Steel<Player1>"))) shouldBe 0
    playback.seek(2)
    playback.world.reader.count(playback.world.reader.resolve(parse("Plant<Player1>"))) shouldBe 1
    playback.world.reader.count(playback.world.reader.resolve(parse("Steel<Player1>"))) shouldBe 1
  }
}
