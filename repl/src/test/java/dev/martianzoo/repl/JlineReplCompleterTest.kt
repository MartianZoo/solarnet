package dev.martianzoo.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.script.ScriptSession
import org.jline.reader.Candidate
import org.junit.jupiter.api.Test

internal class JlineReplCompleterTest {
  private val completer = JlineReplCompleter(ScriptSession())

  @Test
  fun adaptsNeutralCompletionToJlineCandidate() {
    val candidate = candidates("mode b").single()

    assertThat(candidate.value()).isEqualTo("blue")
    assertThat(candidate.group()).isEqualTo("modes")
    assertThat(candidate.descr()).contains("Turn integrity")
  }

  private fun candidates(line: String): List<Candidate> = completer.completeLine(line)
}
