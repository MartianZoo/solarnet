package dev.martianzoo.tfm.repl

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Attributes
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp.Capability

fun main() {
  val terminal = TerminalBuilder.builder()
      .color(true)
      .build()

  terminal.enterRawMode()
  terminal.puts(Capability.enter_ca_mode)
  terminal.puts(Capability.keypad_xmit)

  val reader = LineReaderBuilder.builder()
      .terminal(terminal)
      .history(DefaultHistory())
      .build()

  println("Welcome to REgo PLastics.")
  val repl = ReplSession()

  val space = Regex("\\s+")
  while (true) {
    try {
      val inputLine = reader.readLine("> ").trim()
      if (inputLine.isEmpty()) continue

      val commandAndArgs = inputLine.split(space, 2)
      val command = commandAndArgs.first()
      repl.replCommand(command, commandAndArgs.getOrNull(1)).forEach(::println)
      println()

    } catch (e: EndOfFileException) {
      return
    } catch (e: Exception) {
      e.printStackTrace()
      println()
    }
  }
}
