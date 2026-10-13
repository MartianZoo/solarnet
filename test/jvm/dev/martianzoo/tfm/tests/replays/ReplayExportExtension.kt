package dev.martianzoo.tfm.tests.replays

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

/** Exports full and short game files, then verifies reconstruction from their recorded inputs. */
internal class ReplayExportExtension : AfterTestExecutionCallback {
  override fun afterTestExecution(context: ExtensionContext) = runBlocking {
    if (context.executionException.isPresent) return@runBlocking
    val replay = context.requiredTestInstance as? AbstractFullGameTest ?: return@runBlocking
    val directory =
        Path.of(
            System.getProperty(
                "solarnet.replayEventLogDirectory",
                "build/replay-event-logs",
            )
        )
    Files.createDirectories(directory)
    val output = directory.resolve("${context.requiredTestClass.simpleName}.json")
    val shortOutput = directory.resolve("${context.requiredTestClass.simpleName}.short.json")
    if (!replay.producesReplayRecording) {
      Files.deleteIfExists(output)
      Files.deleteIfExists(shortOutput)
      return@runBlocking
    }
    val json = replay.completedGameFileJson() ?: return@runBlocking
    Files.writeString(output, json)
    Files.writeString(shortOutput, checkNotNull(replay.completedGameFileJson(short = true)))
    replay.verifyGameFileRoundTrip()
    println("Game-file round-trip passed: ${context.requiredTestClass.simpleName}")
  }
}
