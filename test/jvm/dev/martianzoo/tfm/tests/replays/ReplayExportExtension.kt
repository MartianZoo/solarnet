package dev.martianzoo.tfm.tests.replays

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/** Opt-in input recording, file export, and verification of reconstruction from recorded inputs. */
internal class ReplayExportExtension : BeforeEachCallback, AfterTestExecutionCallback {
  override fun beforeEach(context: ExtensionContext) {
    val replay = context.requiredTestInstance as? AbstractFullGameTest ?: return
    replay.exportReplay = java.lang.Boolean.getBoolean("solarnet.replayExports")
  }

  override fun afterTestExecution(context: ExtensionContext) = runBlocking {
    if (context.executionException.isPresent) return@runBlocking
    val replay = context.requiredTestInstance as? AbstractFullGameTest ?: return@runBlocking
    if (!replay.exportReplay) return@runBlocking
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
    val json = replay.completedGameFileJson() ?: return@runBlocking
    Files.writeString(output, json)
    Files.writeString(shortOutput, checkNotNull(replay.completedGameFileJson(short = true)))
    replay.verifyGameFileRoundTrip(json)
    println("Game-file round-trip passed: ${context.requiredTestClass.simpleName}")
  }
}
