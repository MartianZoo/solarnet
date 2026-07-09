package dev.martianzoo.interactive

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.repl.ReplSession
import org.jline.reader.Candidate
import org.junit.jupiter.api.Test

private class JlineReplCompleterTest {
  private val completer = JlineReplCompleter(ReplSession())

  @Test
  fun adaptsNeutralCompletionToJlineCandidate() {
    val candidate = candidates("mode b").single()

    assertThat(candidate.value()).isEqualTo("blue")
    assertThat(candidate.group()).isEqualTo("modes")
    assertThat(candidate.descr()).contains("Turn integrity")
  }

  private fun candidates(line: String): List<Candidate> = completer.completeLine(line)
}
