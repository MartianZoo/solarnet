package dev.martianzoo.tfm.repl

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder

fun main() {
  val term = TerminalBuilder.builder()
      .color(true)
      .build()

  val reader = LineReaderBuilder.builder()
      .terminal(term)
      .build()

  println("Welcome to REgo PLastics.")
  val repl = ReplSession(::println)

  while (true) {
    try {
      val inputLine = reader.readLine("> ").trim()
      if (inputLine.isEmpty()) continue

      val commandAndArgs = inputLine.split(Regex("\\s+"), 2)
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
