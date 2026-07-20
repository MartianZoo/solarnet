@file:Suppress("UnsafeCastFromDynamic")

package dev.martianzoo.web

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletionEngine
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException
import dev.martianzoo.script.welcome
import kotlin.js.JSON
import kotlin.js.json
import kotlinx.browser.window

@JsModule("jquery") @JsNonModule private external fun jquery(selector: String): dynamic

@JsModule("jquery.terminal") @JsNonModule private external val jqueryTerminal: dynamic

@JsModule("jquery.terminal/js/unix_formatting")
@JsNonModule
private external val unixFormatting: dynamic

@JsModule("jquery.terminal/css/jquery.terminal.min.css")
@JsNonModule
private external val terminalStyles: dynamic

private const val HISTORY_KEY = "rego-plastics-command-history"
private const val HISTORY_LIMIT = 1_000

internal class BrowserHistory {
  private val commands: MutableList<String> = load()

  fun record(command: String) {
    if (command.isBlank()) return
    commands += command
    if (commands.size > HISTORY_LIMIT) {
      commands.subList(0, commands.size - HISTORY_LIMIT).clear()
    }
    window.localStorage.setItem(HISTORY_KEY, JSON.stringify(commands.toTypedArray()))
  }

  fun lines(max: Int? = null): List<String> {
    val first = max?.let { (commands.size - it).coerceAtLeast(0) } ?: 0
    return commands.drop(first).mapIndexed { offset, command -> "${first + offset + 1}: $command" }
  }

  private fun load(): MutableList<String> {
    val stored = window.localStorage.getItem(HISTORY_KEY) ?: return mutableListOf()
    return try {
      JSON.parse<Array<String>>(stored).toMutableList()
    } catch (_: Throwable) {
      mutableListOf()
    }
  }
}

private class BrowserHistoryCommand(private val history: BrowserHistory) :
    ScriptCommand("history") {
  override val usage = "history <count>"
  override val help =
      """
        This shows the history of the commands you've typed into the browser REPL. It contains
        history from previous browser sessions too. `history 20` shows only the last 20 commands.
      """
  override val isReadOnly = true

  override fun noArgs(): List<String> = history.lines()

  override fun withArgs(args: String): List<String> {
    val max = args.toIntOrNull()?.takeIf { it >= 0 } ?: throw UsageException()
    return history.lines(max)
  }
}

private class BrowserInformationCommand(name: String, override val help: String) :
    ScriptCommand(name) {
  override val usage: String = name
  override val isReadOnly = true

  override fun noArgs(): List<String> = listOf(help)
}

public fun main() {
  // These imports extend jQuery and install the terminal's ANSI formatter and CSS.
  jqueryTerminal
  unixFormatting
  terminalStyles

  val history = BrowserHistory()
  val session = ScriptSession {
    listOf(
        BrowserHistoryCommand(history),
        BrowserInformationCommand("exit", "Close this browser tab to exit."),
        BrowserInformationCommand(
            "rebuild",
            "Reload this page to restart. Your current game will be lost.",
        ),
    )
  }
  val completionEngine = ScriptCompletionEngine(session)

  val interpreter = { command: String, terminal: dynamic ->
    history.record(command)
    session.executeAll(command).forEach { terminal.echo(it) }
  }
  val options =
      json(
          "name" to "rego-plastics",
          "greetings" to welcome,
          "history" to true,
          "historySize" to HISTORY_LIMIT,
          "prompt" to { setPrompt: (String) -> Unit -> setPrompt(session.prompt()) },
          "completion" to
              { line: String, offer: (Array<String>) -> Unit ->
                offer(completionEngine.completeLine(line).map { it.value }.toTypedArray())
              },
          "checkArity" to false,
          "convertLinks" to false,
          "wrap" to true,
      )

  jquery("#terminal").terminal(interpreter, options)
}
