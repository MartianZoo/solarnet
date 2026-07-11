package dev.martianzoo.repl

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.welcome
import kotlin.io.path.Path
import kotlin.system.exitProcess
import org.jline.reader.Completer
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp.Capability

public fun main() {
  val session = ScriptSession()
  val repl = JlineRepl(session)
  repl.loop()
  println("Bye")
}

internal class JlineRepl(private val session: ScriptSession) : ReplTerminal {
  private val replCommands: Map<String, ScriptCommand> =
      listOf(HistoryCommand(this)).associateBy { it.name }
  private val completer: Completer = JlineReplCompleter(session, replCommands.values.toList())
  private val historyFile = Path(System.getProperty("user.home") + "/.rego_history")
  private val terminal: Terminal =
      TerminalBuilder.builder().color(true).build().also {
        // it.enterRawMode()
        it.puts(Capability.enter_ca_mode)
        it.puts(Capability.keypad_xmit)
      }

  private val history = DefaultHistory()

  private val reader: LineReader =
      LineReaderBuilder.builder().terminal(terminal).history(history).completer(completer).build().also {
        history.attach(it)
        history.read(historyFile, /* checkDuplicates= */ false)
      }

  fun loop() = loop(session::prompt, ::executeAll, welcome)

  private val inputRegex = Regex("""^\s*(\S+)(.*)$""")

  private fun executeAll(input: String): List<String> {
    val allOutput = mutableListOf<String>()
    for (chunk in input.split(";").map { it.trim() }.filter { it.isNotEmpty() }) {
      val lines =
          try {
            execute(chunk)
          } catch (e: Exception) {
            listOf("Error: ${e.message ?: e.toString()}")
          }
      allOutput += lines
      allOutput += ""
    }
    return allOutput
  }

  private fun execute(input: String): List<String> {
    val groups = inputRegex.matchEntire(input)?.groupValues ?: return emptyList()
    val (_, commandName, arguments) = groups
    return when (commandName.lowercase()) {
      "help" -> replHelp(arguments.trim().ifEmpty { null })
      else -> {
        val command = replCommands[commandName.lowercase()]
        if (command == null) {
          session.command(input)
        } else {
          session.command(command, arguments.trim().ifEmpty { null })
        }
      }
    }
  }

  private fun replHelp(args: String?): List<String> =
      when (args?.lowercase()) {
        "exit" -> listOf("I mean it exits.")
        "rebuild" -> listOf("Exits, recompiles the code, and restarts. Your game is lost.")
        "history" -> replCommands.getValue("history").help.trimIndent().split("\n")
        else -> session.command("help${args?.let { " $it" } ?: ""}")
      }

  override fun loop(prompt: () -> String, handler: (String) -> List<String>, welcome: String) {
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

      when (entireLine.trim().lowercase()) {
        "exit" -> return end()
        "rebuild" -> {
          end()
          exitProcess(5) // see /rego
        }
        else -> handler(entireLine).forEach(::println)
      }
    }
  }

  override fun historyLines(max: Int?): List<String> {
    val drop = max?.let { (history.size() - it).coerceIn(0, null) } ?: 0
    return history.drop(drop).map { "${it.index() + 1}: ${it.line()}" }
  }
}
