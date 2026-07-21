@file:Suppress("UnsafeCastFromDynamic")

package dev.martianzoo.web

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletionEngine
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException
import dev.martianzoo.script.welcome
import kotlin.js.JSON
import kotlin.js.json
import kotlinx.browser.document
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

  fun renderDashboard() {
    val snapshot = session.playerSnapshot("Player1")

    fun setValue(name: String, value: Any?) {
      document.querySelector("[data-stat='$name']")?.textContent = value?.toString() ?: "—"
    }

    setValue("player-name", snapshot.playerName.replace("Player", "Player "))
    setValue("phase", snapshot.phase ?: "No phase")
    setValue("victory-points", snapshot.victoryPoints)
    setValue("terraform-rating", snapshot.terraformRating)
    setValue("cards", snapshot.cards)
    snapshot.resources.forEach { resource ->
      val key = resource.name.lowercase()
      setValue("$key-stock", resource.stock)
      setValue(
          "$key-production",
          if (resource.production > 0) "+${resource.production}" else resource.production,
      )
    }
    snapshot.tags.forEach { tag -> setValue("${tag.name}-tag", tag.count) }
  }

  fun renderMap() {
    val snapshot = session.mapSnapshot()
    val svg = buildString {
      append("<svg viewBox='0 0 1000 1000' role='img' aria-labelledby='map-title'>")
      append("<title id='map-title'>${snapshot.name} map</title>")

      snapshot.areas.forEach { area ->
        val centerX = 500.0 + 107.0 * (area.column - area.row / 2.0 - 2.5)
        val centerY = 125.0 + 93.5 * (area.row - 1)
        val points =
            listOf(
                    centerX to centerY - 62.0,
                    centerX + 53.7 to centerY - 31.0,
                    centerX + 53.7 to centerY + 31.0,
                    centerX to centerY + 62.0,
                    centerX - 53.7 to centerY + 31.0,
                    centerX - 53.7 to centerY - 31.0,
                )
                .joinToString(" ") { (x, y) -> "$x,$y" }
        append("<polygon class='map-space ${area.kind}' points='$points'/>")

        if (area.kind == "volcanic") {
          append(
              "<path class='volcano-marker' d='M ${centerX - 20},${centerY + 22} " +
                  "L ${centerX - 7},${centerY - 4} L $centerX,${centerY + 5} " +
                  "L ${centerX + 10},${centerY - 8} L ${centerX + 23},${centerY + 22} Z'/>",
          )
          append(
              "<path class='volcano-smoke' d='M ${centerX - 3},${centerY - 12} " +
                  "C ${centerX - 10},${centerY - 23} ${centerX + 7},${centerY - 25} " +
                  "${centerX + 1},${centerY - 37}'/>",
          )
        }

        if (area.kind == "noctis" && area.tile == null) {
          append("<text class='noctis-label' x='$centerX' y='${centerY + 21}'>Noctis</text>")
          append("<text class='noctis-label' x='$centerX' y='${centerY + 38}'>City</text>")
        }

        if (area.tile == null) {
          val iconSize = 25.0
          val bonusAssets =
              area.bonuses.mapNotNull { bonus ->
                when (bonus) {
                  "P" -> "plant"
                  "S" -> "steel"
                  "T" -> "titanium"
                  "C" -> "card"
                  "H" -> "heat"
                  else -> null
                }
              }
          val totalWidth = bonusAssets.size * iconSize
          bonusAssets.forEachIndexed { index, asset ->
            val x = centerX - totalWidth / 2 + index * iconSize
            val y = centerY - 25.0
            append(
                "<image class='bonus-icon' href='assets/resources/$asset.png' " +
                    "x='$x' y='$y' width='$iconSize' height='$iconSize'/>",
            )
          }
          if ("O" in area.bonuses && "-" in area.bonuses) {
            append(
                "<image class='bonus-icon' href='assets/tiles/ocean.png' " +
                    "x='${centerX - 17.5}' y='${centerY - 12}' width='21' height='24'/>",
            )
            append(
                "<image class='bonus-icon' href='assets/map/hellas-ocean-cost.png' " +
                    "x='${centerX + 5.5}' y='${centerY - 12}' width='12' height='12'/>",
            )
          }
        } else {
          append(
              "<image class='map-tile' href='assets/tiles/${area.tile}.png' " +
                  "x='${centerX - 55}' y='${centerY - 63}' width='110' height='126'/>",
          )
          area.owner?.let { owner ->
            append(
                "<rect class='owner-cube ${owner.lowercase()}' x='${centerX + 24}' " +
                    "y='${centerY + 19}' width='16' height='16' rx='2'/>",
            )
          }
        }
      }
      append("</svg>")
    }
    document.getElementById("mars-map")?.innerHTML = svg
  }

  fun renderDisplays() {
    renderDashboard()
    renderMap()
  }

  val interpreter = { command: String, terminal: dynamic ->
    history.record(command)
    session.executeAll(command).forEach { terminal.echo(it) }
    renderDisplays()
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
  renderDisplays()
}
