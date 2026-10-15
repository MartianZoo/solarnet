package dev.martianzoo.state

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.testCatalog
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent.ChangeEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

internal class GameFileJsonTest {
  @Test
  internal fun premiseAndCatalogFingerprintsMatchAcrossTargets() = runTest {
    val catalog = testCatalog("CLASS Token : System")
    val premise = catalog.gamePremise(GameConfig("Token"))
    GameFingerprint.catalog(catalog) shouldBe
        "e02c8bbeb97d0d2ed55f6b6b556d64ee1cd11991854bbc2e83b82512c751e642"
    GameFingerprint.premise(premise) shouldBe
        "cc5e77f88ca5b097573a30bc0273b7d95bfa4a39c16211f8f5efaf7c0cdc5a53"
  }

  @Test
  internal fun fullFileOpensPassivelyAndRetainsNotes() = runTest {
    val catalog = testCatalog("CLASS Token : System")
    val premise = catalog.gamePremise(GameConfig("Token"))
    val token = premise.classTable.resolve(parse<Expression>("Token")).toComponent()
    val event = ChangeEvent(0, ADMIN, ComponentChange.Gain(2, token), cause = null)
    event.notes = "observed correction"
    val recording = GameRecording(premise, listOf(event), listOf(Checkpoint(0), Checkpoint(1)))
    val inputs =
        listOf(
            RecordedInput.StartGame,
            RecordedInput.Command(ADMIN, RecordedInput.Kind.EX_MACHINA, "2 Token"),
        )

    val file = GameFileJson.parse(GameFileJson.encode(recording, inputs))
    val importedPremise = file.premise(catalog)
    file.hasEvents shouldBe true
    file.inputs(importedPremise) shouldBe inputs
    file.decode(importedPremise).events.single().notes shouldBe "observed correction"
    file.open(catalog).world.reader.count(token.type) shouldBe 2
  }

  @Test
  internal fun shortFileRetainsInputsButCannotBeOpenedByState() = runTest {
    val catalog = testCatalog("CLASS Token : System")
    val premise = catalog.gamePremise(GameConfig("Token"))
    val recording = GameRecording(premise, emptyList(), listOf(Checkpoint(0)))
    val inputs = listOf(RecordedInput.StartGame)

    val file = GameFileJson.parse(GameFileJson.encode(recording, inputs, short = true))
    file.hasEvents shouldBe false
    file.inputs(file.premise(catalog)) shouldBe inputs
    shouldThrow<IllegalArgumentException> { file.open(catalog) }
  }

  @Test
  internal fun rejectsDifferentCatalogEvenWhenPremiseSelectsSameClasses() = runTest {
    val catalog = testCatalog("CLASS Token : System")
    val premise = catalog.gamePremise(GameConfig("Token"))
    val recording = GameRecording(premise, emptyList(), listOf(Checkpoint(0)))
    val file = GameFileJson.parse(GameFileJson.encode(recording, listOf(RecordedInput.StartGame)))
    val enlargedCatalog = testCatalog("CLASS Token : System\nCLASS Spare : System")

    shouldThrow<IllegalArgumentException> { file.premise(enlargedCatalog) }
  }

  @Test
  internal fun rejectsDifferentResolvedPremise() = runTest {
    val catalog = testCatalog("CLASS Token : System\nCLASS Spare : System")
    val premise = catalog.gamePremise(GameConfig("Token"))
    val file =
        GameFileJson.parse(
            GameFileJson.encode(
                GameRecording(premise, emptyList(), listOf(Checkpoint(0))),
                listOf(RecordedInput.StartGame),
            )
        )

    shouldThrow<IllegalArgumentException> { file.decode(catalog.gamePremise(GameConfig("Spare"))) }
  }

  @Test
  internal fun shortFileRetainsStructuredCardInputs() = runTest {
    val premise = testGamePremise("CLASS Token")
    val player = premise.actors.filterIsInstance<Player>().single()
    val input =
        RecordedInput.Command(
            player,
            RecordedInput.Kind.DO_TASK,
            "Token",
            cardInputs =
                listOf(
                    RecordedInput.CardInput(
                        player,
                        "ProjectCard",
                        RecordedInput.CardMovement.HAND_ARRIVAL,
                        listOf("AcquiredCompany", "AdaptedLichen"),
                    ),
                ),
            taskSelector = RecordedInput.TaskSelector("Token", emptyList(), 0),
        )
    val file =
        GameFileJson.parse(
            GameFileJson.encode(
                GameRecording(premise, emptyList(), listOf(Checkpoint(0))),
                listOf(RecordedInput.StartGame, input),
                short = true,
            )
        )

    file.inputs(file.premise(premise.catalog)) shouldBe listOf(RecordedInput.StartGame, input)
  }
}
