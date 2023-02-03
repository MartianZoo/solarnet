package dev.martianzoo.tfm.repl

import kotlin.io.path.Path
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp.Capability

internal class JlineRepl {
  val terminal: Terminal =
      TerminalBuilder.builder().color(true).build().also {
        it.enterRawMode()
        it.puts(Capability.enter_ca_mode)
        it.puts(Capability.keypad_xmit)
      }

  val history = DefaultHistory().also { it.read(historyFile, /* checkDuplicates= */ false) }

  val reader: LineReader =
      LineReaderBuilder.builder().terminal(terminal).history(history).build().also {
        history.attach(it)
      }

  var prompt = "> "

  private fun end() {
    history.append(historyFile, true)
  }

  fun loop(handler: (String) -> List<String>) {
    while (true) {
      val inputLine =
          try {
            reader.readLine(prompt)
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
    }
  }

  private val historyFile = Path(System.getProperty("user.home") + "/.rego_history")
}
