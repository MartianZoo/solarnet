package dev.martianzoo.tfm.repl

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SpecialClassNames.CLASS
import dev.martianzoo.api.SpecialClassNames.COMPONENT
import dev.martianzoo.api.SpecialClassNames.SIGNAL
import dev.martianzoo.api.SpecialClassNames.SYSTEM
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Game
import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.tfm.canon.Canon.SIMPLE_GAME
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.pets.HasExpression.Companion.expressions
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.repl.Access.BlueMode
import dev.martianzoo.tfm.repl.Access.GreenMode
import dev.martianzoo.tfm.repl.Access.PurpleMode
import dev.martianzoo.tfm.repl.Access.RedMode
import dev.martianzoo.tfm.repl.Access.YellowMode
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.BLUE
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.GREEN
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.PURPLE
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.RED
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.YELLOW
import dev.martianzoo.types.MType
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.random
import dev.martianzoo.util.toStrings
import java.io.File
import org.jline.reader.History
import org.jline.reader.impl.history.DefaultHistory

internal fun main() {
  val jline = JlineRepl()
  val repl = ReplSession(jline)
  repl.loop()
  println("Bye")
}

/** A programmatic entry point to a REPL session that is more textual than [ReplSession]. */
internal class ReplSession(private val jline: JlineRepl? = null) {
  lateinit var setup: GameSetup
    private set
  lateinit var game: Game
    private set
  lateinit var tfm: TfmGameplay
    private set

  private var mode: ReplMode = GREEN

  private fun newGame(setup: GameSetup) {
    this.setup = setup
    game = Engine.newGame(setup)
    tfm = game.tfm(ENGINE) // default autoexec mode
  }

  init {
    newGame(SIMPLE_GAME)
  }

  fun loop() = jline!!.loop(::prompt, ::command, welcome)

  private fun prompt(): String {
    return with(tfm) {
      val bundles = setup.bundles.joinToString("")
      val phase = list("Phase").single()
      val checkpoint = game.timeline.checkpoint()
      mode.color.foreground("$bundles $phase $player/${setup.players} @$checkpoint> ")
    }
  }

  private val inputRegex = Regex("""^\s*(\S+)(.*)$""")

  internal class UsageException(message: String? = null) : Exception(message ?: "")

  internal abstract inner class ReplCommand(val name: String) {
    open val isReadOnly: Boolean = false // not currently used
    abstract val usage: String
    abstract val help: String
    open fun noArgs(): List<String> = throw UsageException()
    open fun withArgs(args: String): List<String> = throw UsageException()
  }

  internal val commands =
      listOf(
              AsCommand(),
              AutoCommand(),
              BecomeCommand(),
              BoardCommand(),
              CountCommand(),
              DescCommand(),
              ExecCommand(),
              HasCommand(),
              HelpCommand(),
              HistoryCommand(),
              ListCommand(),
              LogCommand(),
              MapCommand(),
              ModeCommand(),
              NewGameCommand(),
              PhaseCommand(),
              RollbackCommand(),
              ScriptCommand(),
              TaskCommand(),
              TasksCommand(),
              TurnCommand(),
              TfmPayCommand(),
              TfmPlayCommand(),
          )
          .associateBy { it.name }

  internal inner class HelpCommand : ReplCommand("help") {
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
          val helpCommand = commands[arg]
          if (helpCommand == null) {
            listOf("¯\\_(ツ)_/¯ Type `help` for help")
          } else {
            helpCommand.help.trimIndent().split("\n")
          }
        }
      }
    }
  }

  internal inner class AsCommand : ReplCommand("as") {
    override val usage = "as <PlayerN> <full command>"
    override val help =
        """
          For any command you could type normally, put `as Player2` etc. or `as Engine` before it.
          It's handled as if you had first `become` that player, then restored.
        """

    override fun noArgs() = throw UsageException()
    override fun withArgs(args: String): List<String> {
      val (player, rest) = args.trim().split(Regex("\\s+"), 2)

      // This is a sad way to do it TODO
      val saved = tfm
      return try {
        tfm = game.tfm(player(player))
        command(rest)
      } finally {
        tfm = saved
      }
    }
  }

  internal inner class NewGameCommand : ReplCommand("newgame") {
    override val usage = "newgame <bundles> <player count>"
    override val help =
        """
          Erases your current game and starts a new one. You can't undo that (but you can get your
          command history out of ~/.rego_session and replay it.) For <bundles>, jam some letters
          together: B=Base, R=coRpoRate eRa, M=Tharsis, H=Hellas, X=Promos, and the rest are what
          you'd think. The player count can be from 1 to 5, but if you choose 1, you are NOT getting
          any of the actual solo rules!
        """

    override fun withArgs(args: String): List<String> {
      try {
        val (bundleString, players) = args.trim().split(Regex("\\s+"), 2)

        setup = GameSetup(setup.authority, bundleString, players.toInt())
        newGame(setup)

        return listOf("New $players-player game created with bundles: $bundleString") +
            if (players.toInt() == 1) {
              listOf("NOTE: No solo mode rules are implemented.")
            } else {
              listOf()
            }
      } catch (e: RuntimeException) {
        throw UsageException(e.message)
      }
    }
  }

  internal inner class BecomeCommand : ReplCommand("become") {
    override val usage = "become [PlayerN]"
    override val help =
        """
          Type `become Player2` or whatever and your prompt will change accordingly; everything you
          do now will be done as if it's player 2 doing it. You can also `become Engine` to do
          engine things.
        """

    override fun noArgs(): List<String> {
      tfm = game.tfm(ENGINE)
      return listOf("Okay, you are the game engine now")
    }

    override fun withArgs(args: String): List<String> {
      tfm = game.tfm(player(args))
      return listOf("Hi, ${tfm.player}")
    }
  }

  internal inner class TurnCommand : ReplCommand("turn") {
    override val usage = "turn"
    override val help =
        """
          Asks the engine to start a new turn for the current player.
        """
    override fun noArgs() = describeExecutionResults(access().newTurn())
  }

  internal inner class PhaseCommand : ReplCommand("phase") {
    override val usage = "phase <phase name>"
    override val help =
        """
          Asks the engine to begin a new phase, e.g. `phase Corporation`
        """
    override fun withArgs(args: String) = describeExecutionResults(access().phase(args.trim()))
  }

  internal inner class HasCommand : ReplCommand("has") {
    override val usage = "has <Requirement>"
    override val help =
        """
          Evaluates the requirement and tells you true or false. Go see syntax.md on the github page
          for syntax.
        """
    override val isReadOnly = true

    override fun withArgs(args: String): List<String> {
      val result = tfm.has(args)
      return listOf("$result: ${tfm.parse<Requirement>(args)}")
    }
  }

  internal inner class CountCommand : ReplCommand("count") {
    override val usage = "count <Metric>"
    override val help =
        """
          Evaluates the metric and tells you the count. Usually just a type, but can include `MAX`,
          `+`, etc.
        """
    override val isReadOnly = true

    override fun withArgs(args: String): List<String> {
      val count = tfm.count(args)
      return listOf("$count ${tfm.parse<Metric>(args)}")
    }
  }

  internal inner class ListCommand : ReplCommand("list") {
    override val usage = "list <Expression>"
    override val help = """
          This command is super broken right now.
        """
    override val isReadOnly = true
    override fun noArgs() = withArgs(COMPONENT.toString())

    override fun withArgs(args: String): List<String> {
      val expr: Expression = tfm.parse(args)
      val counts: Multiset<Expression> = tfm.list(args)
      return listOf("${counts.size} $expr") +
          counts.entries.sortedByDescending { (_, ct) -> ct }.map { (e, ct) -> "  $ct $e" }
    }
  }

  internal inner class BoardCommand : ReplCommand("board") {
    override val usage = "board [PlayerN]"
    override val help =
        """
          Shows a crappy player board for the named player, or the current player by default.
        """
    override val isReadOnly = true

    override fun noArgs(): List<String> = PlayerBoardToText(tfm, jline != null).board()

    override fun withArgs(args: String) =
        PlayerBoardToText(tfm.asPlayer(player(args)), jline != null).board()
  }

  internal inner class MapCommand : ReplCommand("map") {
    override val usage = "map"
    override val help = """
          I mean it shows a map.
        """
    override val isReadOnly = true
    override fun noArgs() = MapToText(tfm.reader, jline != null).map()
  }

  //  var workflow: Workflow? = null
  //
  //  internal inner class StartCommand : ReplCommand("start") {
  //    override val usage = ""
  //    override val help = ""
  //
  //    override fun noArgs(): List<String> {
  //      require(workflow == null)
  //      workflow = Workflow(game).also { it.start() }
  //      return listOf("I think it started?")
  //    }
  //  }

  internal inner class ModeCommand : ReplCommand("mode") {
    override val usage = "mode <mode name>"
    override val help =
        """
          Changes modes. Names are red, yellow, green, blue, purple. Just enter a mode and it will
          tell you what it means.
        """

    override fun noArgs() = listOf("Mode $mode: ${mode.message}")

    override fun withArgs(args: String): List<String> {
      try {
        mode = ReplMode.valueOf(args.uppercase())
      } catch (e: Exception) {
        throw UsageException(
            "Valid modes are: ${ReplMode.values().joinToString { it.toString().lowercase() }}")
      }
      return noArgs()
    }
  }

  internal inner class AutoCommand : ReplCommand("auto") {
    override val usage = "auto [none|safe|first]"
    override val help =
        """
          TODO fix this
          Turns auto-execute mode on or off, or just `auto` tells you what mode you're in. When you
          initiate an instruction with `exec` or `task`, per the game rules you always get to decide
          what order to do all the resulting tasks in. But that's a pain, so when `auto` is `on` (as
          it is by default) the REPL tries to execute each task (in the order they appear on the
          cards), and leaves it on the queue only if it can't run correctly. This setting is sticky
          until you `exit` or `rebuild`, even across games.
        """

    override fun noArgs() = listOf("Autoexec mode is: ${tfm.autoExecMode}")

    override fun withArgs(args: String): List<String> {
      tfm.autoExecMode =
          when (args) {
            "none" -> NONE
            "safe" -> SAFE
            "first" -> FIRST
            else -> throw UsageException()
          }
      return noArgs()
    }
  }

  internal inner class ExecCommand : ReplCommand("exec") {
    override val usage = "exec <Instruction>"
    override val help =
        """
          Initiates the specified instruction; see syntax.md on github for details on syntax. If
          `auto` mode is on, it will also try to execute any tasks that result from this. Otherwise
           use `tasks` to see which tasks are waiting for you.
        """

    override fun withArgs(args: String) = describeExecutionResults(access().exec(args))
  }

  private fun access(): Access = // TODO maybe don't do this "just-in-time"...
  when (mode) {
        RED -> RedMode(tfm.godMode())
        YELLOW -> YellowMode(tfm.godMode())
        GREEN -> GreenMode(tfm.godMode())
        BLUE -> BlueMode(tfm.godMode())
        PURPLE -> PurpleMode(tfm.godMode())
      }

  internal inner class TasksCommand : ReplCommand("tasks") {
    override val usage = "tasks"
    override val help =
        """
          List all currently pending tasks. You can then execute or drop them using `task`. The
          tasks of all players plus the engine are currently mixed together (but labeled).
        """
    override val isReadOnly = true
    override fun noArgs() = tfm.game.tasks.extract { it.toStringWithoutCause() }
  }

  internal inner class TaskCommand : ReplCommand("task") {
    override val usage = "task <id> [<Instruction> | drop]"
    override val help =
        """
          To carry out a task exactly as it is, just type `task A` where `A` is the id of that task
          in your `tasks` list. But usually a task gets put on that list because its instruction
          was not fully specified. So, after `task A` you can write a revised version of that
          instruction, as long as your revision is a more specific form of the instruction. For
          example, if the queued task is `-3 StandardResource<Anyone>?` you can revise it to
          `-2 Plant<Player1>`. If you leave out the id (like `A`) it will expect your revision to
          match only one existing task.
        """

    override fun withArgs(args: String): List<String> {
      val split = Regex("\\s+").split(args, 2)
      val first = split.firstOrNull() ?: throw UsageException()
      if (!first.matches(Regex("[A-Z]{1,2}"))) {
        return describeExecutionResults(tfm.tryTask(args))
      }

      val id = TaskId(first.uppercase())
      if (id !in game.tasks) throw UsageException("valid ids are ${game.tasks.ids()}")
      val rest: String? =
          if (split.size > 1 && split[1].isNotEmpty()) {
            split[1]
          } else {
            null
          }

      val result: TaskResult =
          when (rest) {
            "drop" -> {
              access().dropTask(id)
              return listOf("Task $id deleted")
            }
            "prepare" -> {
              tfm.prepareTask(id)
              return tfm.game.tasks.extract { "$it" }
            }
            null -> tfm.tryTask(id)
            else ->
                tfm.game.timeline.atomic {
                  tfm.reviseTask(id, rest)
                  if (id in game.tasks) tfm.tryTask(id)
                }
          }
      return describeExecutionResults(result)
    }
  }

  private fun describeExecutionResults(result: TaskResult): List<String> {
    val changes = result.changes.filterNot { isSystem(it, tfm.reader) }.toStrings()

    val newTasks: Set<TaskId> = result.tasksSpawned
    val taskLines =
        if (newTasks.any()) {
          listOf("New tasks pending:") +
              tfm.game.tasks
                  .extract { if (it.id in newTasks) it.toStringWithoutCause() else null }
                  .filterNotNull()
        } else {
          listOf()
        }
    return if (changes.none() && taskLines.none()) {
      listOf("um, nothing happened")
    } else if (changes.any() && taskLines.any()) {
      changes + listOf("") + taskLines
    } else {
      changes + taskLines
    }
  }

  internal inner class LogCommand : ReplCommand("log") {
    override val usage = "log [full]"
    override val help =
        """
          Shows everything that has happened in the current game (`log full`) or just the more
          interesting bits (i.e., filtering out Task changes, and filtering out changes to System
          components -- just like the default output after `exec` or `task` does).
        """
    override val isReadOnly = true

    override fun noArgs() =
        tfm.game.events.changesSinceSetup().filterNot { isSystem(it, tfm.reader) }.toStrings()

    override fun withArgs(args: String): List<String> {
      if (args == "full") {
        return tfm.game.events.entriesSince(Checkpoint(0)).toStrings()
      } else {
        throw UsageException()
      }
    }
  }

  fun isSystem(event: ChangeEvent, game: GameReader): Boolean {
    val g = event.change.gaining
    val r = event.change.removing

    val system = game.resolve(SYSTEM.expression)
    if (listOfNotNull(g, r).all { game.resolve(it).narrows(system) }) return true

    if (r != null) {
      val signal = game.resolve(SIGNAL.expression)
      if (game.resolve(r).narrows(signal)) return true
    }
    return false
  }

  internal inner class RollbackCommand : ReplCommand("rollback") {
    override val usage = "rollback <logid>"
    override val help =
        """
          Undoes the event with the id given and every event after it. If you undo too far,
          you can't go forward again (you can only try to reconstruct the game from your
          ~/.rego_history). If you want to undo your command `exec 5 Plant`, look for the number in
          the command prompt on that line; that's the number to use here. Or check `log`. Be careful
          though, as you it will let you undo to a position when the engine was in the middle of
          doing stuff, which would put you in an invalid game state.
        """

    override fun withArgs(args: String): List<String> {
      tfm.game.timeline.rollBack(Checkpoint(args.toInt()))
      return listOf("Rollback done")
    }
  }

  internal inner class HistoryCommand : ReplCommand("history") {
    override val usage = "history <count>"
    override val help =
        """
          This shows the history of the commands you've typed into the REPL. It should contain
          history from your previous sessions too (hopefully). `history 20` would show you only
          the last 20. These are numbered, and if one command is numbered 123 you can type `!123`
          to repeat it. You can also write `!` plus the first few letters of the command and you'll
          get the most recent match. There's other stuff you can do; look for info on the `jline`
          library if curious.
        """
    override val isReadOnly = true
    private val history: DefaultHistory? = jline?.history

    override fun noArgs() = fmt(history!!)

    override fun withArgs(args: String): List<String> {
      val max = args.toIntOrNull() ?: throw UsageException()
      val drop = (history!!.size() - max).coerceIn(0, null)
      return fmt(history.drop(drop))
    }

    private fun fmt(entries: Iterable<History.Entry>) =
        entries.map { "${it.index() + 1}: ${it.line()}" }
  }

  internal inner class DescCommand : ReplCommand("desc") {
    override val usage = "desc <Expression>"
    override val help =
        """
          Put any type expression after `desc` and it will tell you everything it knows about that
          type. A page on github somewhere will explain what all the output means, but it doesn't
          exist yet.
        """
    override val isReadOnly = true

    override fun withArgs(args: String): List<String> {
      val (expression, type) =
          if (args == "random") {
            val type =
                tfm.reader
                    .resolve(CLASS.expression)
                    .let(tfm.reader::getComponents)
                    .expressions()
                    .map { it.arguments.single() }
                    .random()
                    .let { tfm.reader.resolve(it) as MType }
                    .concreteSubtypesSameClass()
                    .random()
            type.expressionFull to type
          } else {
            val expression: Expression = tfm.parse(args)
            expression to game.reader.resolve(expression) as MType
          }
      return listOf(MTypeToText.describe(expression, type))
    }
  }

  internal inner class ScriptCommand : ReplCommand("script") {
    override val usage = "script <filename>"
    override val help =
        """
          Reads from the given filename (expressed relative to the solarnet/ directory) and executes
          every command in it, as if you had typed it directly at the prompt, until reaching the
          line "stop" or the end of file. You probably don't want to put "exit" in that file.
        """

    override fun withArgs(args: String) =
        File(args)
            .readLines()
            .takeWhile { it.trim() != "stop" }
            .filter { it.isNotEmpty() }
            .flatMap { listOf(">>> $it") + command(it) + "" }
  }

  internal inner class TfmPayCommand : ReplCommand("tfm_pay") {
    override val usage: String = "tfm_pay <amount resource>"
    override val help: String = ""
    override fun withArgs(args: String): List<String> {
      val gains: List<Instruction> = split(Parsing.parse(args)).instructions

      val ins =
          Multi.create(
              gains.map {
                val sex = (it as Gain).scaledEx
                val currency = sex.expression
                val pay = cn("Pay").of(CLASS.of(currency))
                Transmute(SimpleFrom(pay, currency), sex.scalar)
              })
      return TaskCommand().withArgs(ins.toString())
    }
  }

  internal inner class TfmPlayCommand : ReplCommand("tfm_play") {
    override val usage: String = "tfm_play <CardName>"
    override val help: String = ""
    override fun withArgs(args: String): List<String> {
      val cardName = cn(args)
      val kind = setup.authority.card(cardName).deck!!.className
      return TaskCommand().withArgs("PlayCard<Class<$kind>, Class<$args>>")
    }
  }

  public fun command(wholeCommand: String): List<String> {
    val stripped = wholeCommand.replace(Regex("//.*"), "")
    val groups = inputRegex.matchEntire(stripped)?.groupValues
    return if (groups == null) {
      listOf()
    } else {
      val (_, commandName, arguments) = groups
      val args = arguments.trim().ifEmpty { null }
      val command = commands[commandName.lowercase()]
      if (command == null) {
        listOf("¯\\_(ツ)_/¯ Type `help` for help")
      } else {
        command(command, args)
      }
    }
  }

  internal fun command(command: ReplCommand, args: String? = null): List<String> {
    return try {
      if (args == null) command.noArgs() else command.withArgs(args.trim())
    } catch (e: RuntimeException) {
      throw e
    } catch (e: Exception) {
      val usage = if (e is UsageException) "Usage: ${command.usage}" else ""
      listOf(e.message ?: "", usage).filter { it.any() }
    }
  }

  public enum class ReplMode(val message: String, val color: TfmColor) {
    RED("Change integrity: make changes without triggered effects", TfmColor.HEAT),
    YELLOW("Task integrity: changes have consequences", TfmColor.MEGACREDIT),
    GREEN("Operation integrity: clear task queue before starting new operation", TfmColor.PLANT),
    BLUE("Turn integrity: must perform a valid game turn for this phase", TfmColor.OCEAN_TILE),
    PURPLE("Game integrity: the engine fully controls the workflow", TfmColor.ENERGY),
  }

  private fun player(name: String): Player {
    // In case a shortname was used
    val type: MType = tfm.reader.resolve(cn(name).expression) as MType
    return Player(type.className)
  }
}

private val welcome =
    """
      Welcome to REgo PLastics. Type `help` or `help <command>` for help.
      Warning: this is a bare-bones tool that is not trying to be easy to use... at all

    """
        .trimIndent()

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
    """
        .trimIndent()
