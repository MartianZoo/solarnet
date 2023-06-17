package dev.martianzoo.repl.commands

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession

internal class HelpCommand(private val repl: ReplSession) : ReplCommand("help") {
  override val usage = "help [command]"
  override val help =
      """
        Help gives you help if you want help, but this help on help doesn't help, if that helps.
      """
  override val isReadOnly = true
  override fun noArgs() = listOf(helpText)
  override fun withArgs(args: String): List<String> {
    val arg = args.trim()
    return when (arg.lowercase()) {
      "exit" -> listOf("I mean it exits.")
      "rebuild" -> listOf("Exits, recompiles the code, and restarts. Your game is lost.")
      else -> {
        val helpCommand: ReplCommand? = repl.commands[arg.lowercase()]
        if (helpCommand != null) {
          helpCommand.help.trimIndent().split("\n")
        } else {
          return try {
            val docstring = repl.game.classes.getClass(cn(arg)).docstring
            listOf("Class `$arg`: \"$docstring\"", "Type `desc $arg` for super gory details.")
          } catch (e: Exception) {
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
          rebuild             -> erases current game and rebuilds the REPL (after code changes)
          become <player>     -> changes the default player for queries & executions
          as <player> <cmd>   -> temporarily changes default player to run a single command
          script <filename>   -> reads a file and performs REPL commands as if typed
          exit                -> go waste time differently
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
          history             -> shows your *command* history (as you typed it)
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
