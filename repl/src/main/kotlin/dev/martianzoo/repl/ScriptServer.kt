package dev.martianzoo.repl

import dev.martianzoo.script.ScriptCompletionEngine
import java.net.ServerSocket

internal const val SERVER_PORT = 2315

public class ScriptServer(port: Int = SERVER_PORT) {
  private val session = newScriptSession()
  private val completer = ScriptCompletionEngine(session)
  // Bind eagerly so callers can read actualPort before run() is called (useful in tests).
  private val serverSocket = ServerSocket(port)
  public val actualPort: Int = serverSocket.localPort

  public fun run() {
    println("Rego server listening on port $actualPort")
    var running = true
    while (running) {
      val socket = serverSocket.accept()
      try {
        val input = socket.getInputStream().bufferedReader().readLine() ?: continue
        val writer = socket.getOutputStream().bufferedWriter()

        fun send(line: String) {
          writer.write(line)
          writer.newLine()
        }

        val completionLine =
            when {
              input.endsWith('\t') -> input.dropLast(1)
              input.endsWith("\\t") -> input.dropLast(2)
              else -> null
            }
        val command = input.trim().lowercase()
        when {
          completionLine != null -> {
            completer.completeLine(completionLine).map { it.value }.forEach(::send)
          }
          command == "exit" -> {
            send("Shutting down rego server.")
            running = false
          }
          command == "rebuild" -> send("'rebuild' is not supported in server mode.")
          else -> {
            session.executeAll(input).dropLastWhile { it.isBlank() }.forEach(::send)
          }
        }
        send("---END---")
        writer.flush()
      } finally {
        socket.close()
      }
    }
    serverSocket.close()
  }
}
