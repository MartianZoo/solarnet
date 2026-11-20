package dev.martianzoo.tfm.tests.replays

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

/** Verifies the checked-in archive and optionally exports a fresh uncommented copy. */
internal class ReplayExportExtension : AfterTestExecutionCallback {
  override fun afterTestExecution(context: ExtensionContext) {
    if (context.executionException.isPresent) return
    val replay = context.requiredTestInstance as? FullGameTest ?: return
    val fixture = "/games/${context.requiredTestClass.simpleName}.game"
    val source =
        checkNotNull(javaClass.getResourceAsStream(fixture)) { "Missing replay archive: $fixture" }
            .bufferedReader()
            .use { it.readText() }
    checkArchive(replay, source, fixture)
    if (!java.lang.Boolean.getBoolean("solarnet.replayExports")) return
    val directory =
        Path.of(
            System.getProperty(
                "solarnet.replayEventLogDirectory",
                "build/replay-event-logs",
            )
        )
    Files.createDirectories(directory)
    val output = directory.resolve("${context.requiredTestClass.simpleName}.game")
    val text = replay.completedGameArchive() ?: return
    Files.writeString(output, text)
    checkArchive(replay, text, output.toString())
  }

  private fun checkArchive(replay: FullGameTest, text: String, label: String) {
    val imported = replay.importGameArchive(text).world
    val original = replay.game
    check(
        imported.reader
            .getComponents(imported.classTable.componentClass.baseType)
            .entries
            .associate { it.key.toString() to it.value } ==
            original.reader
                .getComponents(original.classTable.componentClass.baseType)
                .entries
                .associate { it.key.toString() to it.value }
    ) {
      "Archive final inventory differs: $label"
    }
    fun pending(tasks: dev.martianzoo.state.TaskQueue) =
        tasks
            .extract {
              "${it.assignee}|${it.selected}|${it.instruction}|${it.then}|${it.deferral}"
            }
            .sorted()
    check(pending(imported.tasks) == pending(original.tasks)) {
      "Archive pending tasks differ: $label"
    }
  }
}
