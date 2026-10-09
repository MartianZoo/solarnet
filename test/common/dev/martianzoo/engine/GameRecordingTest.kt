package dev.martianzoo.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.CustomMetric
import dev.martianzoo.state.GameReader
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameRecordingTest {
  @Test
  internal fun passivePlaybackNeedsNoCustomImplementationsUntilTheirMetricsAreQueried() {
    val premise = testGamePremise("CLASS Score : CustomMetric\nCLASS Token")
    val score =
        object : CustomMetric("Score") {
          override fun count(game: GameReader, type: Type): Int = 7
        }
    val game = Engine.newGame(premise, setOf(score))
    game.testAgent(PLAYER1).runOperation("Token")

    val playback = game.recording().open()

    playback.world.reader.count(parse<Metric>("Token")) shouldBe 1
    playback.seek(0)
    playback.world.reader.count(parse<Metric>("Token")) shouldBe 0
    val error =
        shouldThrow<ExpressionException> {
          playback.world.reader.count(parse<Metric>("Score"))
        }
    error.detail shouldBe "custom metric `Score` has no implementation"
  }

  @Test
  internal fun recordingSeeksAcrossCompletedOperationsAndNotifiesComponentListeners() {
    val game = TfmEngine.newGame(canonicalPremise())
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
    val game = TfmEngine.newGame(canonicalPremise())
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
