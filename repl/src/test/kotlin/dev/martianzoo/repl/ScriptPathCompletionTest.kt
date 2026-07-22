package dev.martianzoo.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.script.ScriptCompletionEngine
import java.io.File
import java.nio.file.Files
import org.junit.jupiter.api.Test

internal class ScriptPathCompletionTest {
  @Test
  fun runsScriptFile() {
    val script = Files.createTempFile("solarnet-script", ".rego")
    try {
      script.toFile().writeText("count Steel\ncount Plant\nstop\ncount Heat\n")

      assertThat(newScriptSession().command("script $script"))
          .containsExactly(
              ">>> count Steel",
              "0 Steel<Owner>",
              "",
              ">>> count Plant",
              "0 Plant<Owner>",
              "",
          )
          .inOrder()
    } finally {
      script.toFile().delete()
    }
  }

  @Test
  fun rejectsExitCommandInScriptFile() {
    val script = Files.createTempFile("solarnet-script", ".rego")
    try {
      script
          .toFile()
          .writeText("// exit is harmless in a comment\ncount Steel\n  ExIt // stop here\n")

      assertThat(newScriptSession().command("script $script"))
          .containsExactly("$script:3: `exit` is not allowed in script files")
    } finally {
      script.toFile().delete()
    }
  }

  @Test
  fun completesScriptPaths() {
    val tempDir = Files.createTempDirectory("solarnet-script-completion")
    try {
      val file = tempDir.resolve("completion-test.rego").toFile()
      val dir = tempDir.resolve("completion-dir").toFile()
      file.writeText("status\n")
      assertThat(dir.mkdir()).isTrue()

      val completer = ScriptCompletionEngine(newScriptSession())
      val fileValues =
          completer.completeLine("script ${tempDir.resolve("completion-test")}").map { it.value }
      assertThat(fileValues).contains(file.toString())

      val dirCandidate =
          completer.completeLine("script ${tempDir.resolve("completion-d")}").single()
      assertThat(dirCandidate.value).isEqualTo("${dir}${File.separator}")
      assertThat(dirCandidate.complete).isFalse()
    } finally {
      tempDir.toFile().deleteRecursively()
    }
  }
}
