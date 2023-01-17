package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.canon.Canon
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp.Capability

val INPUT_REGEX = Regex("""^\s*(\S+)(.*)$""")

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
  reader.readLine("Welcome to REgo PLastics! Press enter.")

  val repl = ReplSession(Canon)
  repl.command("newgame BM 2").forEach(::println)

  while (true) {
    val inputLine = try {
      reader.readLine("> ")
    } catch (e: EndOfFileException) {
      return
    }
    val results = try {
      repl.command(inputLine)
    } catch (e: Exception) {
      listOf("${e::class}: ${e.message}")
    }
    results.forEach(::println)
    println()
  }
}
