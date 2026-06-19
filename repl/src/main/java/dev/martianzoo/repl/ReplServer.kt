package dev.martianzoo.repl

import java.net.ServerSocket

internal const val SERVER_PORT = 2315

internal class ReplServer(port: Int = SERVER_PORT) {
  private val session = ReplSession()
  // Bind eagerly so callers can read actualPort before run() is called (useful in tests).
  private val serverSocket = ServerSocket(port)
  internal val actualPort: Int = serverSocket.localPort

  internal fun run() {
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

        when (input.trim().lowercase()) {
          "exit" -> {
            send("Shutting down rego server.")
            running = false
          }
          "rebuild" -> send("'rebuild' is not supported in server mode.")
          else -> session.executeAll(input).dropLastWhile { it.isBlank() }.forEach(::send)
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
