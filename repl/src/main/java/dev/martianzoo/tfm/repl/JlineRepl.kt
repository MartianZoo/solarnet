package dev.martianzoo.tfm.repl

import kotlin.io.path.Path
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle.DEFAULT
import org.jline.utils.InfoCmp.Capability

internal class JlineRepl {
  private val historyFile = Path(System.getProperty("user.home") + "/.rego_history")
  val terminal: Terminal =
      TerminalBuilder.builder().color(true).build().also {
        it.enterRawMode()
        it.puts(Capability.enter_ca_mode)
        it.puts(Capability.keypad_xmit)
      }

  val history = DefaultHistory()

  val reader: LineReader =
      LineReaderBuilder.builder().terminal(terminal).history(history).build().also {
        history.attach(it)
        history.read(historyFile, /* checkDuplicates= */ false)
      }

  fun loop(prompt: () -> Pair<String, Int>, handler: (String) -> List<String>) {
    while (true) {
      val (text, color) = prompt()
      val pr =
          AttributedStringBuilder()
              .style(DEFAULT.foreground(color))
              .append(text)
              .append("> ")
              .style(DEFAULT)
              .toAnsi()

      fun end() = history.append(historyFile, true)

      val inputLine =
          try {
            reader.readLine(pr)
          } catch (e: EndOfFileException) {
            return end()
          }

      when (inputLine.trim()) {
        "exit" -> return end()
        "history" -> history.forEach { println("${it.index() + 1}: ${it.line()}") }
        else ->
            try {
              handler(inputLine).forEach(::println)
            } catch (e: Exception) {
              e.printStackTrace()
            }
      }
      println()
    }
  }
}
