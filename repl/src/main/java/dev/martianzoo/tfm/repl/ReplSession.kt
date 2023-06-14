package dev.martianzoo.tfm.repl

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.SystemClasses.SIGNAL
import dev.martianzoo.api.SystemClasses.SYSTEM
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.Player
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Game
import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.HasExpression.Companion.expressions
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Companion.split
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon.SIMPLE_GAME
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
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
import dev.martianzoo.tfm.repl.commands.AsCommand
import dev.martianzoo.tfm.repl.commands.HelpCommand
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

internal class ReplSession(private val jline: JlineRepl? = null) {
  lateinit var setup: GameSetup
  lateinit var game: Game
  lateinit var tfm: TfmGameplay

  var mode: ReplMode = GREEN

  fun newGame(setup: GameSetup) {
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

  internal val commands =
      listOf(
              AsCommand(this),
              AutoCommand(this),
              BecomeCommand(this),
              BoardCommand(this),
              CountCommand(this),
              DescCommand(this),
              ExecCommand(this),
              HasCommand(this),
              HelpCommand(this),
              HistoryCommand(this),
              ListCommand(this),
              LogCommand(this),
              MapCommand(this),
              ModeCommand(this),
              NewGameCommand(this),
              PhaseCommand(this),
              RollbackCommand(this),
              ScriptCommand(this),
              TaskCommand(this),
              TasksCommand(this),
              TurnCommand(this),
              TfmPayCommand(this),
              TfmPlayCommand(this),
          )
          .associateBy { it.name }

  internal class NewGameCommand(val repl: ReplSession) : ReplCommand("newgame") {
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

        repl.setup = GameSetup(repl.setup.authority, bundleString, players.toInt())
        repl.newGame(repl.setup)

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

  internal class BecomeCommand(val repl: ReplSession) : ReplCommand("become") {
    override val usage = "become [PlayerN]"
    override val help =
        """
          Type `become Player2` or whatever and your prompt will change accordingly; everything you
          do now will be done as if it's player 2 doing it. You can also `become Engine` to do
          engine things.
        """

    override fun noArgs(): List<String> {
      repl.tfm = repl.game.tfm(ENGINE)
      return listOf("Okay, you are the game engine now")
    }

    override fun withArgs(args: String): List<String> {
      repl.tfm = repl.game.tfm(repl.player(args))
      return listOf("Hi, ${repl.tfm.player}")
    }
  }

  internal class TurnCommand(val repl: ReplSession) : ReplCommand("turn") {
    override val usage = "turn"
    override val help =
        """
          Asks the engine to start a new turn for the current player.
        """
    override fun noArgs() = repl.describeExecutionResults(repl.access().newTurn())
  }

  internal class PhaseCommand(val repl: ReplSession) : ReplCommand("phase") {
    override val usage = "phase <phase name>"
    override val help =
        """
          Asks the engine to begin a new phase, e.g. `phase Corporation`
        """
    override fun withArgs(args: String) =
        repl.describeExecutionResults(repl.access().phase(args.trim()))
  }

  internal class HasCommand(val repl: ReplSession) : ReplCommand("has") {
    override val usage = "has <Requirement>"
    override val help =
        """
          Evaluates the requirement and tells you true or false. Go see syntax.md on the github page
          for syntax.
        """
    override val isReadOnly = true

    override fun withArgs(args: String): List<String> {
      val result = repl.tfm.has(args)
      return listOf("$result: ${repl.tfm.parse<Requirement>(args)}")
    }
  }

  internal class CountCommand(val repl: ReplSession) : ReplCommand("count") {
    override val usage = "count <Metric>"
    override val help =
        """
          Evaluates the metric and tells you the count. Usually just a type, but can include `MAX`,
          `+`, etc.
        """
    override val isReadOnly = true

    override fun withArgs(args: String): List<String> {
      val count = repl.tfm.count(args)
      return listOf("$count ${repl.tfm.parse<Metric>(args)}")
    }
  }

  internal class ListCommand(val repl: ReplSession) : ReplCommand("list") {
    override val usage = "list <Expression>"
    override val help = """
          This command is super broken right now.
        """
    override val isReadOnly = true
    override fun noArgs() = withArgs(COMPONENT.toString())

    override fun withArgs(args: String): List<String> {
      val expr: Expression = repl.tfm.parse(args)
      val counts: Multiset<Expression> = repl.tfm.list(args)
      return listOf("${counts.size} $expr") +
          counts.entries.sortedByDescending { (_, ct) -> ct }.map { (e, ct) -> "  $ct $e" }
    }
  }

  internal class BoardCommand(val repl: ReplSession) : ReplCommand("board") {
    override val usage = "board [PlayerN]"
    override val help =
        """
          Shows a crappy player board for the named player, or the current player by default.
        """
    override val isReadOnly = true

    override fun noArgs(): List<String> = PlayerBoardToText(repl.tfm, repl.jline != null).board()

    override fun withArgs(args: String) =
        PlayerBoardToText(repl.tfm.asPlayer(repl.player(args)), repl.jline != null).board()
  }

  internal class MapCommand(val repl: ReplSession) : ReplCommand("map") {
    override val usage = "map"
    override val help = """
          I mean it shows a map.
        """
    override val isReadOnly = true
    override fun noArgs() = MapToText(repl.tfm.reader, repl.jline != null).map()
  }

  internal class ModeCommand(val repl: ReplSession) : ReplCommand("mode") {
    override val usage = "mode <mode name>"
    override val help =
        """
          Changes modes. Names are red, yellow, green, blue, purple. Just enter a mode and it will
          tell you what it means.
        """

    override fun noArgs() = listOf("Mode ${repl.mode}: ${repl.mode.message}")

    override fun withArgs(args: String): List<String> {
      try {
        repl.mode = ReplMode.valueOf(args.uppercase())
      } catch (e: Exception) {
        throw UsageException(
            "Valid modes are: ${ReplMode.values().joinToString { it.toString().lowercase() }}")
      }
      return noArgs()
    }
  }

  internal class AutoCommand(val repl: ReplSession) : ReplCommand("auto") {
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

    override fun noArgs() = listOf("Autoexec mode is: ${repl.tfm.autoExecMode}")

    override fun withArgs(args: String): List<String> {
      repl.tfm.autoExecMode =
          when (args) {
            "none" -> NONE
            "safe" -> SAFE
            "first" -> FIRST
            else -> throw UsageException()
          }
      return noArgs()
    }
  }

  internal class ExecCommand(val repl: ReplSession) : ReplCommand("exec") {
    override val usage = "exec <Instruction>"
    override val help =
        """
          Initiates the specified instruction; see syntax.md on github for details on syntax. If
          `auto` mode is on, it will also try to execute any tasks that result from this. Otherwise
           use `tasks` to see which tasks are waiting for you.
        """

    override fun withArgs(args: String) = repl.describeExecutionResults(repl.access().exec(args))
  }

  internal fun access(): Access = // TODO maybe don't do this "just-in-time"...
  when (mode) {
        RED -> RedMode(tfm.godMode())
        YELLOW -> YellowMode(tfm.godMode())
        GREEN -> GreenMode(tfm.godMode())
        BLUE -> BlueMode(tfm.godMode())
        PURPLE -> PurpleMode(tfm.godMode())
      }

  internal class TasksCommand(val repl: ReplSession) : ReplCommand("tasks") {
    override val usage = "tasks"
    override val help =
        """
          List all currently pending tasks. You can then execute or drop them using `task`. The
          tasks of all players plus the engine are currently mixed together (but labeled).
        """
    override val isReadOnly = true
    override fun noArgs() = repl.tfm.game.tasks.extract { it.toStringWithoutCause() }
  }

  internal class TaskCommand(val repl: ReplSession) : ReplCommand("task") {
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
        return repl.describeExecutionResults(repl.tfm.tryTask(args))
      }

      val id = TaskId(first.uppercase())
      if (id !in repl.game.tasks) throw UsageException("valid ids are ${repl.game.tasks.ids()}")
      val rest: String? =
          if (split.size > 1 && split[1].isNotEmpty()) {
            split[1]
          } else {
            null
          }

      val result: TaskResult =
          when (rest) {
            "drop" -> {
              repl.access().dropTask(id)
              return listOf("Task $id deleted")
            }
            "prepare" -> {
              repl.tfm.prepareTask(id)
              return repl.tfm.game.tasks.extract { "$it" }
            }
            null -> repl.tfm.tryTask(id)
            else ->
              repl.tfm.game.timeline.atomic {
                repl.tfm.reviseTask(id, rest)
                  if (id in repl.game.tasks) repl.tfm.tryTask(id)
                }
          }
      return repl.describeExecutionResults(result)
    }
  }

  fun describeExecutionResults(result: TaskResult): List<String> {
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

  internal class LogCommand(val repl: ReplSession) : ReplCommand("log") {
    override val usage = "log [full]"
    override val help =
        """
          Shows everything that has happened in the current game (`log full`) or just the more
          interesting bits (i.e., filtering out Task changes, and filtering out changes to System
          components -- just like the default output after `exec` or `task` does).
        """
    override val isReadOnly = true

    override fun noArgs() =
        repl.tfm.game.events.changesSinceSetup().filterNot { repl.isSystem(it, repl.tfm.reader) }.toStrings()

    override fun withArgs(args: String): List<String> {
      if (args == "full") {
        return repl.tfm.game.events.entriesSince(Checkpoint(0)).toStrings()
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

  internal class RollbackCommand(val repl: ReplSession) : ReplCommand("rollback") {
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
      repl.tfm.game.timeline.rollBack(Checkpoint(args.toInt()))
      return listOf("Rollback done")
    }
  }

  internal class HistoryCommand(val repl: ReplSession) : ReplCommand("history") {
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
    val history: DefaultHistory? = repl.jline?.history

    override fun noArgs() = fmt(history!!)

    override fun withArgs(args: String): List<String> {
      val max = args.toIntOrNull() ?: throw UsageException()
      val drop = (history!!.size() - max).coerceIn(0, null)
      return fmt(history.drop(drop))
    }

    private fun fmt(entries: Iterable<History.Entry>) =
        entries.map { "${it.index() + 1}: ${it.line()}" }
  }

  internal class DescCommand(val repl: ReplSession) : ReplCommand("desc") {
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
                repl.tfm.reader
                    .resolve(CLASS.expression)
                    .let(repl.tfm.reader::getComponents)
                    .expressions()
                    .map { it.arguments.single() }
                    .random()
                    .let { repl.tfm.reader.resolve(it) as MType }
                    .concreteSubtypesSameClass()
                    .random()
            type.expressionFull to type
          } else {
            val expression: Expression = repl.tfm.parse(args)
            expression to repl.game.reader.resolve(expression) as MType
          }
      return listOf(MTypeToText.describe(expression, type))
    }
  }

  internal class ScriptCommand(val repl: ReplSession) : ReplCommand("script") {
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
            .flatMap { listOf(">>> $it") + repl.command(it) + "" }
  }

  internal class TfmPayCommand(val repl: ReplSession) : ReplCommand("tfm_pay") {
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
      return TaskCommand(repl).withArgs(ins.toString())
    }
  }

  internal class TfmPlayCommand(val repl: ReplSession) : ReplCommand("tfm_play") {
    override val usage: String = "tfm_play <CardName>"
    override val help: String = ""
    override fun withArgs(args: String): List<String> {
      val cardName = cn(args)
      val kind = repl.setup.authority.card(cardName).deck!!.className
      return TaskCommand(repl).withArgs("PlayCard<Class<$kind>, Class<$args>>")
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

  fun player(name: String): Player {
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
