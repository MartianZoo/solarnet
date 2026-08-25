package dev.martianzoo.script

import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.engine.World
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.HIDDEN
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.Task.TaskId
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.pets.types.Type
import dev.martianzoo.script.Access.BlueMode
import dev.martianzoo.script.Access.GreenMode
import dev.martianzoo.script.Access.PurpleMode
import dev.martianzoo.script.Access.RedMode
import dev.martianzoo.script.Access.YellowMode
import dev.martianzoo.script.ScriptSession.ScriptMode.BLUE
import dev.martianzoo.script.ScriptSession.ScriptMode.GREEN
import dev.martianzoo.script.ScriptSession.ScriptMode.PURPLE
import dev.martianzoo.script.ScriptSession.ScriptMode.RED
import dev.martianzoo.script.ScriptSession.ScriptMode.YELLOW
import dev.martianzoo.script.commands.AsCommand
import dev.martianzoo.script.commands.AutoCommand
import dev.martianzoo.script.commands.BecomeCommand
import dev.martianzoo.script.commands.CountCommand
import dev.martianzoo.script.commands.DescCommand
import dev.martianzoo.script.commands.ExecCommand
import dev.martianzoo.script.commands.HasCommand
import dev.martianzoo.script.commands.HelpCommand
import dev.martianzoo.script.commands.ListCommand
import dev.martianzoo.script.commands.LogCommand
import dev.martianzoo.script.commands.ModeCommand
import dev.martianzoo.script.commands.NewGameCommand
import dev.martianzoo.script.commands.PhaseCommand
import dev.martianzoo.script.commands.RollbackCommand
import dev.martianzoo.script.commands.StatusCommand
import dev.martianzoo.script.commands.TaskCommand
import dev.martianzoo.script.commands.TasksCommand
import dev.martianzoo.script.commands.TurnCommand
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.script.TFM_SCRIPT_CLASS_SYNONYMS
import dev.martianzoo.tfm.script.TfmColor
import dev.martianzoo.tfm.script.TfmColor.ENERGY
import dev.martianzoo.tfm.script.TfmColor.HEAT
import dev.martianzoo.tfm.script.TfmColor.MEGACREDIT
import dev.martianzoo.tfm.script.TfmColor.OCEAN_TILE
import dev.martianzoo.tfm.script.TfmColor.PLANT
import dev.martianzoo.tfm.script.commands.TfmActionCommand
import dev.martianzoo.tfm.script.commands.TfmBoardCommand
import dev.martianzoo.tfm.script.commands.TfmMapCommand
import dev.martianzoo.tfm.script.commands.TfmPayCommand
import dev.martianzoo.tfm.script.commands.TfmPlayCommand
import dev.martianzoo.tfm.script.commands.TfmSampleCommand

/** @param useAnsiColors whether prompts and command output may contain ANSI escape sequences. */
public class ScriptSession(
    private val locale: String = Vocabulary.ENGLISH,
    internal val useAnsiColors: Boolean = false,
    hostCommands: (ScriptSession) -> List<ScriptCommand> = { emptyList() },
) {
  internal lateinit var game: World // TODO maybe remove and just have reader/events/...?

  internal lateinit var gameplay: TurnLayer
  internal var optionCodes: String = ""
    private set

  internal var playerCount: Int = 0
    private set

  internal var mode: ScriptMode = GREEN

  private fun newGame(
      setup: OptionCodeTranslation.Setup,
      purple: Boolean = false,
  ) {
    installGame(createGame(setup, locale), setup.optionCodes, setup.players, purple)
  }

  private fun installGame(
      candidateGame: World,
      candidateOptionCodes: String,
      candidatePlayerCount: Int,
      purple: Boolean,
  ) {
    val candidateGameplay = candidateGame.gameplay(ENGINE) as TurnLayer // default autoexec mode
    if (purple) {
      TfmWorkflow.Auto(candidateGame).launch()
    } else {
      TfmWorkflow.Manual(candidateGame).setupPhase()
    }
    optionCodes = candidateOptionCodes
    playerCount = candidatePlayerCount
    game = candidateGame
    gameplay = candidateGameplay
    if (purple) mode = PURPLE
  }

  /** Adapts the REPL's legacy option-code syntax to a raw canonical game configuration. */
  internal fun newGame(
      optionCodes: String,
      players: Int,
      selectedColonies: Set<ClassName> = emptySet(),
      purple: Boolean = false,
  ) {
    newGame(OptionCodeTranslation.setup(optionCodes, players, selectedColonies), purple)
  }

  internal fun newGame(
      configText: String,
      playerNames: List<String>,
      purple: Boolean = false,
  ) {
    val premise = Canon.gamePremise(GameConfig(configText, *playerNames.toTypedArray()))
    val options = OptionCodeTranslation.recognizedOptions(premise.modules)
    val candidateGame =
        Engine.newGame(
            premise,
            locale,
            inputOnlySynonyms = TFM_SCRIPT_CLASS_SYNONYMS,
        )
    installGame(
        candidateGame,
        OptionCodeTranslation.optionCodes(options),
        premise.actors.count { it is Player },
        purple,
    )
  }

  init {
    newGame("B", 2)
  }

  public fun prompt(): String =
      if (useAnsiColors) mode.color.foreground(promptPlain()) else promptPlain()

  internal fun promptPlain(): String =
      with(gameplay) {
        val phase = list("Phase").singleOrNull() ?: "(no phase)"
        val checkpoint = game.timeline.checkpoint()
        "$optionCodes $phase ${game.vocabulary.petsName(gameplay.actor)}/$playerCount @$checkpoint> "
      }

  private val inputRegex = Regex("""^\s*(\S+)(.*)$""")

  public class UsageException(message: String? = null) : Exception(message.orEmpty())

  // Splits on semicolons and executes each chunk; used by both interactive and server modes.
  @Suppress("TooGenericExceptionCaught") // TODO investigate
  public fun executeAll(input: String): List<String> {
    val allOutput = mutableListOf<String>()
    for (chunk in input.split(";").map { it.trim() }.filter { it.isNotEmpty() }) {
      val lines =
          try {
            command(chunk)
          } catch (e: Exception) {
            listOf("Error: ${e.message ?: e.toString()}")
          }
      allOutput += lines
      allOutput += ""
    }
    return allOutput
  }

  internal val commands =
      (listOf(
              AsCommand(this),
              AutoCommand(this),
              BecomeCommand(this),
              TfmBoardCommand(this),
              CountCommand(this),
              DescCommand(this),
              ExecCommand(this),
              HasCommand(this),
              HelpCommand(this),
              ListCommand(this),
              LogCommand(this),
              TfmMapCommand(this),
              ModeCommand(this),
              NewGameCommand(this),
              PhaseCommand(this),
              RollbackCommand(this),
              StatusCommand(this),
              TaskCommand(this),
              TasksCommand(this),
              TurnCommand(this),
              TfmActionCommand(this),
              TfmPayCommand(this),
              TfmPlayCommand(this),
              TfmSampleCommand(this),
          ) + hostCommands(this))
          .associateBy { it.name }

  internal fun access(): Access = // TODO maybe don't do this "just-in-time"...
  when (mode) {
        RED -> RedMode(gameplay.godMode())
        YELLOW -> YellowMode(gameplay.godMode())
        GREEN -> GreenMode(gameplay.godMode())
        BLUE -> BlueMode(gameplay.godMode())
        PURPLE -> PurpleMode()
      }

  internal fun describeExecutionResults(result: TaskResult): List<String> {
    val changes =
        result.changes
            .filterNot { isHidden(it, game.reader) }
            .map { event ->
              game.vocabulary.renderPets(event)
            }

    val newTaskLines = taskLines(result.tasksSpawned)
    val taskLines =
        if (newTaskLines.any()) {
          listOf("New tasks pending:") + newTaskLines
        } else {
          emptyList()
        }
    return if (changes.none() && taskLines.none()) {
      listOf("um, nothing happened")
    } else if (changes.any() && taskLines.any()) {
      changes + listOf("") + taskLines
    } else {
      changes + taskLines
    }
  }

  internal fun selectableTasks(ids: Set<TaskId>? = null): List<Task> =
      game.tasks
          .extract { it }
          .filter {
            it.assignee == gameplay.actor && (ids == null || it.id in ids)
          }

  internal fun taskLines(ids: Set<TaskId>? = null): List<String> =
      selectableTasks(ids).map { task -> game.vocabulary.renderPets(task, displayId = null) }

  internal fun onlyTask(): Task =
      selectableTasks().singleOrNull()
          ?: throw UsageException("this requires exactly one pending task")

  internal fun isHidden(event: ChangeEvent, game: GameReader): Boolean {
    val g = event.change.gaining
    val r = event.change.removing

    val changedTypes = listOfNotNull(g, r).map(game::resolve)
    val hidden = game.resolve(HIDDEN.expression)
    val phase = game.resolve(cn("Phase").expression)
    return changedTypes.all { it.isSubtypeOf(hidden) } &&
        changedTypes.none { it.isSubtypeOf(phase) }
  }

  public fun command(wholeCommand: String): List<String> {
    val stripped = wholeCommand.replace(Regex("//.*"), "")
    val groups = inputRegex.matchEntire(stripped)?.groupValues
    return if (groups == null) {
      emptyList()
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

  @Suppress("TooGenericExceptionCaught") // TODO seems appropriate but should we log?
  public fun command(command: ScriptCommand, args: String? = null): List<String> {
    return try {
      if (args == null) command.noArgs() else command.withArgs(args.trim())
    } catch (e: RuntimeException) {
      throw e
    } catch (e: UsageException) {
      listOf(e.message.orEmpty(), "Usage: ${command.usage}").filter { it.any() }
    } catch (e: Exception) {
      listOf(e.message.orEmpty())
    }
  }

  internal enum class ScriptMode(public val message: String, public val color: TfmColor) {
    RED("Change integrity: make changes without triggered effects", HEAT),
    YELLOW("Task integrity: changes have consequences", MEGACREDIT),
    GREEN("Operation integrity: clear task queue before starting new operation", PLANT),
    BLUE("Turn integrity: must perform a valid game turn for this phase", OCEAN_TILE),
    PURPLE("Game integrity: the engine fully controls the workflow", ENERGY),
  }

  internal fun actor(name: String): Actor {
    if (name == ENGINE.toString()) return ENGINE
    return player(name)
  }

  internal fun player(name: String): Player {
    // In case a configured synonym was used
    val type: Type = gameplay.resolve(name)
    return game.actors.filterIsInstance<Player>().singleOrNull { it.className == type.className }
        ?: throw UsageException("not a participating Player: $name")
  }

  internal fun canonicalColonyName(name: String): ClassName =
      colonyInputVocabulary.canonicalName(cn(name))

  private val colonyInputVocabulary: Vocabulary by lazy {
    Vocabulary.create(
        Canon,
        locale,
        TFM_SCRIPT_CLASS_SYNONYMS,
        activeClassNames = Canon.colonyTileClassNames,
    )
  }
}

internal fun createGame(
    setup: OptionCodeTranslation.Setup,
    locale: String = Vocabulary.ENGLISH,
): World {
  val config =
      GameConfig.create(
          included = setup.options + setup.selectedColonies,
          excluded = setup.excludedOptions,
          playerNames =
              if (setup.players == 1) listOf(cn("Me"))
              else (1..setup.players).map { cn("Player$it") },
      )
  return Engine.newGame(
      Canon.gamePremise(config),
      locale,
      inputOnlySynonyms = TFM_SCRIPT_CLASS_SYNONYMS,
  )
}

public val welcome: String =
    """
    Welcome to REgo PLastics. Type `help` or `help <command>` for help.
    Warning: this is a bare-bones tool that is not trying to be easy to use... at all

    """
        .trimIndent()
