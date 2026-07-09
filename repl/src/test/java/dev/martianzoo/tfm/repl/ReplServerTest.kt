package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.repl.ReplServer
import java.net.Socket
import org.junit.jupiter.api.Test

private class ReplServerTest {

  // Starts the server on a random port, runs the given block, then shuts down via "exit".
  private fun withServer(block: (send: (String) -> List<String>) -> Unit) {
    val server = ReplServer(port = 0)
    val thread = Thread { server.run() }.also { it.isDaemon = true; it.start() }

    fun send(command: String): List<String> =
        Socket("localhost", server.actualPort).use { socket ->
          socket.getOutputStream().bufferedWriter().run { write("$command\n"); flush() }
          socket.getInputStream().bufferedReader().lineSequence()
              .takeWhile { it != "---END---" }
              .toList()
        }

    try {
      block(::send)
    } finally {
      try { send("exit") } catch (_: Exception) {} // no-op if test already sent exit
      thread.join(3000)
      assertThat(thread.isAlive).isFalse()
    }
  }

  @Test
  fun basicCommand() = withServer { send ->
    assertThat(send("count Steel")).containsExactly("0 Steel<Owner>")
  }

  @Test
  fun statusReturnsPrompt() = withServer { send ->
    val result = send("status")
    assertThat(result).hasSize(1)
    assertThat(result.single()).endsWith("> ")
  }

  @Test
  fun semicolons() = withServer { send ->
    assertThat(send("count Steel; count Plant"))
        .containsExactly("0 Steel<Owner>", "", "0 Plant<Owner>")
        .inOrder()
  }

  @Test
  fun trailingTabReturnsCompletions() = withServer { send ->
    assertThat(send("mode b\t")).containsExactly("blue")
  }

  @Test
  fun trailingBackslashTAlsoReturnsCompletions() = withServer { send ->
    assertThat(send("mode b\\t")).containsExactly("blue")
  }

  @Test
  fun trailingTabDoesNotExecuteServerControlCommands() = withServer { send ->
    assertThat(send("exit\t")).isEmpty()
    assertThat(send("status")).hasSize(1)
  }

  @Test
  fun rebuildReturnsError() = withServer { send ->
    assertThat(send("rebuild").single()).contains("not supported")
  }

  @Test
  fun exitShutsDownServer() = withServer { send ->
    // withServer itself calls exit and asserts the thread dies — just verify the response message
    assertThat(send("exit").single()).contains("Shutting down")
  }
}
