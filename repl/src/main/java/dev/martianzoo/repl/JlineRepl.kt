package dev.martianzoo.repl

import kotlin.io.path.Path
import kotlin.system.exitProcess
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp.Capability

internal class JlineRepl {
  private val historyFile = Path(System.getProperty("user.home") + "/.rego_history")
  private val terminal: Terminal =
      TerminalBuilder.builder().color(true).build().also {
        // it.enterRawMode()
        it.puts(Capability.enter_ca_mode)
        it.puts(Capability.keypad_xmit)
      }

  val history = DefaultHistory()

  private val reader: LineReader =
      LineReaderBuilder.builder().terminal(terminal).history(history).build().also {
        history.attach(it)
        history.read(historyFile, /* checkDuplicates= */ false)
      }

  fun loop(prompt: () -> String, handler: (String) -> List<String>, welcome: String) {
    var first = true
    while (true) {
      fun end() = history.append(historyFile, true)

      val entireLine =
          try {
            reader.readLine((if (first) "$welcome\n" else "") + prompt())
          } catch (e: EndOfFileException) {
            return end()
          }
      first = false

      for (chunk in entireLine.split(";").map { it.trim() }) {
        when (chunk.trim().lowercase()) {
          "exit" -> return end()
          "rebuild" -> {
            end()
            exitProcess(5) // see /rego
          }
          else ->
              try {
                handler(chunk).forEach(::println)
              } catch (e: Exception) {
                e.printStackTrace()
              }
        }
        println()
      }
    }
  }
}
