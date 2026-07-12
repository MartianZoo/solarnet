package dev.martianzoo.script.commands

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession

internal class HelpCommand(private val repl: ScriptSession) : ScriptCommand("help") {
  override val usage = "help [command]"
  override val help =
      """
        Help gives you help if you want help, but this help on help doesn't help, if that helps.
      """
  override val isReadOnly = true

  override fun noArgs() = listOf(helpText)

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.commandNames() + context.classNames()

  @Suppress("TooGenericExceptionCaught") // TODO think about
  override fun withArgs(args: String): List<String> {
    val arg = args.trim()
    return when (arg.lowercase()) {
      else -> {
        val helpCommand: ScriptCommand? = repl.commands[arg.lowercase()]
        if (helpCommand != null) {
          helpCommand.help.trimIndent().split("\n")
        } else {
          return try {
            val docstring = repl.game.classes.getClass(cn(arg)).docstring
            listOf("Class `$arg`: \"$docstring\"", "Type `desc $arg` for super gory details.")
          } catch (_: Exception) {
            listOf("¯\\_(ツ)_/¯ Type `help` for help")
          }
        }
      }
    }
  }

  private val helpText: String =
      """
      Commands can be separated with semicolons, or saved in a file and run with `script`.
      Type `help <command name>` to learn more.,

      CONTROL
        newgame             -> erases current game and starts a new one with given setup
        become <player>     -> changes the default player for queries & executions
        as <player> <cmd>   -> temporarily changes default player to run a single command
        script <filename>   -> reads a file and performs REPL commands as if typed
      QUERYING
        has <requirement>   -> evaluates a requirement (true/false) in the current game state
        count <metric>      -> counts something in the game state, like `count Tag<Player2>`
        list <expression>   -> lists all instances of some type in the current game state
      EXECUTION
        exec <instruction>  -> initiates an arbitrary instruction if current mode allows it
        tasks               -> shows your current to-do list
        task <taskid>       -> performs a task on your to-do list
        turn                -> begins a new turn for current player (necessary only in blue mode)
        phase <name>        -> begins a new game phase (e.g. `as Engine phase Action`)
        auto <mode>         -> changes the auto-execute mode
        mode <mode>         -> changes repl modes (more power vs. more game integrity)
      HISTORY
        log [full]          -> shows events that have happened in the current game
        rollback <id>       -> returns the game to an earlier state, forgetting everything since
      METADATA
        desc <expression>   -> describes a type like `Microbe<Ants>` in great detail
      TERRAFORMING MARS
        tfm_board           -> displays an extremely bad looking player board
        tfm_map             -> displays an extremely bad looking Mars board
        tfm_play <card>     -> plays a Terraforming Mars card (shortcut)
        tfm_pay <amt> <res> -> pays some amount of MC/Steel/etc for something (shortcut)
        tfm_sample          -> executes one of the hardcoded sample games
      """
          .trimIndent()
}
