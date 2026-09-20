package dev.martianzoo.state

import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.state.GameEvent.ChangeEvent
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameRecordingJsonTest {
  @Test
  internal fun recordingRoundTripsAndOpensIndependentViews() {
    val premise =
        testGamePremise("CLASS Token\nCLASS Marker")
            .copy(
                classSelections =
                    setOf(
                        ClassSelection(cn("Token")),
                        ClassSelection(cn("Marker"), included = false),
                    )
            )
    val token = premise.classTable.resolve(parse<Expression>("Token")).toComponent()
    val events =
        listOf(
            ChangeEvent(0, ADMIN, ComponentChange.Gain(1, token), cause = null),
            ChangeEvent(1, ADMIN, ComponentChange.Gain(2, token), cause = null),
        )
    val recording =
        GameRecording(premise, events, listOf(Checkpoint(0), Checkpoint(1), Checkpoint(2)))
    events.first().notes = "changed after capture"
    val text = GameRecordingJson.encode(recording)
    val document = GameRecordingJson.parse(text)
    val decoded = document.decode(premise)

    document.config shouldBe GameConfig("Token, -Marker", "Player1")
    decoded.positions shouldBe recording.positions
    decoded.events shouldBe events
    GameRecordingJson.encode(decoded) shouldBe text

    val first = decoded.open()
    val second = decoded.open()
    first.world.events.changeAt(0)!!.notes = "changed in first playback"
    decoded.events.first().notes shouldBe null
    second.world.events.changeAt(0)!!.notes shouldBe null
    first.seek(1)
    first.world.reader.count(token.type) shouldBe 1
    second.world.reader.count(token.type) shouldBe 3
  }
}
