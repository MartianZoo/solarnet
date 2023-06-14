package dev.martianzoo.repl.commands

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
    val arg = args.trim().lowercase()
    return when (arg) {
      "exit" -> listOf("I mean it exits.")
      "rebuild" -> listOf("Exits, recompiles the code, and restarts. Your game is lost.")
      else -> {
        val helpCommand = repl.commands[arg]
        if (helpCommand == null) {
          listOf("¯\\_(ツ)_/¯ Type `help` for help")
        } else {
          helpCommand.help.trimIndent().split("\n")
        }
      }
    }
  }

  private val helpText: String =
      """
        Commands can be separated with semicolons, or saved in a file and run with `script`.
        Type `help <command name>` to learn more.,
  
        CONTROL
          help                -> shows this message
          newgame BHV 3       -> erases current game and starts 3p game with Base/Hellas/Venus
          exit                -> go waste time differently
          rebuild             -> restart after code changes (game is forgotten)
          become Player1      -> makes Player1 the default player for queries & executions
          as Player1 <cmd>    -> does <cmd> as if you'd typed just that, but as Player1
          script mygame       -> reads file `mygame` and performs REPL commands as if typed
        QUERYING
          has MAX 3 OceanTile -> evaluates a requirement (true/false) in the current game state
          count Plant         -> counts how many Plants the default player has
          list Tile           -> list all Tiles (categorized)
          board               -> displays an extremely bad looking player board
          map                 -> displays an extremely bad looking Mars board
        EXECUTION
          exec PROD[3 Heat]   -> gives the default player 3 heat production
          tasks               -> shows your current to-do list
          task F              -> do task F on your to-do list, as-is
          task F Plant        -> do task F, substituting `Plant` for an abstract instruction
          task F drop         -> bye task F
          turn                -> begin new turn for current player (necessary only in blue mode)
          auto off            -> turns off autoexec (run tasks manually but can't break integrity)
          mode yellow         -> switches to Yellow Mode (also try red, green, blue, purple)
        HISTORY
          log                 -> shows events that have happened in the current game
          rollback 123        -> undoes recent events up to and *including* event 123
          history             -> shows your *command* history (as you typed it)
        METADATA
          desc Microbe<Ants>  -> describes the Microbe<Ants> type in detail
      """.trimIndent()
}
