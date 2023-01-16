package dev.martianzoo.tfm.repl

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder

fun main() {
  val term = TerminalBuilder.builder()
      .color(true)
      .build()

  val reader = LineReaderBuilder.builder()
      .terminal(term)
      .history(DefaultHistory())
      .build()

  println("Welcome to REgo PLastics.")
  val repl = ReplSession(::println)

  val space = Regex("\\s+")
  while (true) {
    try {
      val inputLine = reader.readLine("> ").trim()
      if (inputLine.isEmpty()) continue

      val commandAndArgs = inputLine.split(space, 2)
      val command = commandAndArgs.first()
      repl.replCommand(command, commandAndArgs.getOrNull(1))
      println()

    } catch (e: EndOfFileException) {
      return
    } catch (e: Exception) {
      println(e.message)
      println()
    }
  }
}
