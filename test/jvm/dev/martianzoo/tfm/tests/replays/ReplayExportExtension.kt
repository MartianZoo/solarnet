package dev.martianzoo.tfm.tests.replays

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

/** Exports passive recordings and verifies games by reimporting their task text. */
internal class ReplayExportExtension : AfterTestExecutionCallback {
  override fun afterTestExecution(context: ExtensionContext) {
    if (context.executionException.isPresent) return
    val replay = context.requiredTestInstance as? AbstractFullGameTest ?: return
    val directory =
        Path.of(
            System.getProperty(
                "solarnet.replayEventLogDirectory",
                "build/replay-event-logs",
            )
        )
    Files.createDirectories(directory)
    val output = directory.resolve("${context.requiredTestClass.simpleName}.json")
    val taskOutput = directory.resolve("${context.requiredTestClass.simpleName}.txt")
    if (!replay.producesReplayRecording) {
      Files.deleteIfExists(output)
      Files.deleteIfExists(taskOutput)
      return
    }
    val json = replay.completedRecordingJson() ?: return
    Files.writeString(output, json)
    Files.writeString(taskOutput, replay.completedTaskLogText())
    replay.verifyTaskRoundTrip()
    println("Task round-trip passed: ${context.requiredTestClass.simpleName}")
  }
}
