package dev.martianzoo.tfm.tests.replays

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

/** Writes each successfully completed full-game test's passive recording for the web viewer. */
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
    if (!replay.producesReplayRecording) {
      Files.deleteIfExists(output)
      return
    }
    val json = replay.completedRecordingJson() ?: return
    Files.writeString(output, json)
  }
}
