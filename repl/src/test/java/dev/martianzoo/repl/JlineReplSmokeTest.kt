package dev.martianzoo.repl

import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.io.path.absolutePathString
import org.junit.jupiter.api.Test

internal class JlineReplSmokeTest {

  @Test
  fun smokeTestRealTerminalSessionWithExpect() {
    val expect = Path.of("/usr/bin/expect")
    assertWithMessage("Expect must be installed for the REPL smoke test")
        .that(Files.isExecutable(expect))
        .isTrue()

    val jar = Path.of(System.getProperty("repl.shadowJar"))
    val java = Path.of(System.getProperty("java.home"), "bin", "java")
    val workDir = Files.createTempDirectory("rego-expect-test")
    val homeDir = Files.createDirectory(workDir.resolve("home"))
    val script = Path.of(System.getProperty("repl.smokeScript"))

    val process =
        ProcessBuilder(
                expect.absolutePathString(),
                script.absolutePathString(),
                java.absolutePathString(),
                jar.absolutePathString(),
                homeDir.absolutePathString(),
            )
            .directory(Path.of("").toAbsolutePath().toFile())
            .redirectErrorStream(true)
            .start()

    val finished = process.waitFor(45, SECONDS)
    if (!finished) {
      process.destroyForcibly()
      process.waitFor(5, SECONDS)
    }
    val transcript = process.inputStream.bufferedReader().readText()

    assertWithMessage(transcript).that(finished).isTrue()
    assertWithMessage(transcript).that(process.exitValue()).isEqualTo(0)
  }
}
