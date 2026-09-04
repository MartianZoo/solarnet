package dev.martianzoo.script

import dev.martianzoo.engine.Agent
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.GameConfig
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
import dev.martianzoo.tfm.canon.ApiUtils
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmClasses.TILE
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.engine.isVisibleInLog
import dev.martianzoo.tfm.script.TFM_SCRIPT_CLASS_SYNONYMS
import dev.martianzoo.tfm.script.TfmColor
import dev.martianzoo.tfm.script.TfmColor.ENERGY
import dev.martianzoo.tfm.script.TfmColor.HEAT
import dev.martianzoo.tfm.script.TfmColor.MC
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

  internal lateinit var agent: Agent
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
    val candidateAgent = candidateGame.agent(ENGINE) // default autoexec mode
    if (purple) {
      TfmWorkflow.Auto(candidateGame).launch()
    } else {
      TfmWorkflow.Manual(candidateGame).setupPhase()
    }
    optionCodes = candidateOptionCodes
    playerCount = candidatePlayerCount
    game = candidateGame
    agent = candidateAgent
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

  /** Returns the current player-board values for a host UI without exposing mutable game state. */
  public fun playerSnapshot(playerName: String = "Player1"): PlayerSnapshot {
    val player = player(playerName)
    val tfm = TfmGameplay(game, player)

    fun countIfLoaded(type: String): Int =
        try {
          tfm.count(type)
        } catch (_: ExpressionException) {
          0
        }

    val resourceNames =
        linkedMapOf(
            "Megacredit" to "MC",
            "Steel" to "Steel",
            "Titanium" to "Titanium",
            "Plant" to "Plant",
            "Energy" to "Energy",
            "Heat" to "Heat",
        )
    val tagTypes =
        linkedMapOf(
            "building" to "BuildingTag",
            "space" to "SpaceTag",
            "science" to "ScienceTag",
            "power" to "PowerTag",
            "earth" to "EarthTag",
            "jovian" to "JovianTag",
            "venus" to "VenusTag",
            "plant" to "PlantTag",
            "microbe" to "MicrobeTag",
            "animal" to "AnimalTag",
            "city" to "CityTag",
            "event" to "PlayedEvent",
        )

    return PlayerSnapshot(
        playerName = player.className.toString(),
        phase = tfm.list("Phase").singleOrNull()?.toString()?.removeSuffix("Phase"),
        victoryPoints = countIfLoaded("VictoryPoint"),
        terraformRating = countIfLoaded("TerraformRating"),
        cards = countIfLoaded("ProjectCard"),
        resources =
            resourceNames.map { (displayName, type) ->
              PlayerResourceSnapshot(
                  name = displayName,
                  stock = countIfLoaded(type),
                  production = tfm.production(cn(type)),
              )
            },
        tags =
            tagTypes
                .filterValues { type -> countIfLoaded("Class<$type>") > 0 }
                .map { (name, type) -> PlayerTagSnapshot(name, countIfLoaded(type)) },
    )
  }

  /** Returns a display-oriented, read-only snapshot of the current Mars map. */
  public fun mapSnapshot(): MarsMapSnapshot {
    val reader = game.reader
    val map = ApiUtils.mapDefinition(reader)

    fun tileKind(areaName: dev.martianzoo.pets.ast.ClassName): Pair<String, String?>? {
      val tile =
          reader.getComponents(reader.resolve(TILE.of(areaName))).singleOrNull() ?: return null
      fun narrows(kind: String) = tile.narrows(reader.resolve(cn(kind).expression), reader)
      val kind =
          when {
            narrows("CityTile") -> "city"
            narrows("OceanTile") -> "ocean"
            narrows("GreeneryTile") -> "greenery"
            narrows("SpecialTile") -> "special"
            else -> "special"
          }
      val owner =
          tile.expressionFull.arguments
              .firstOrNull { Player.isValid(it.className) }
              ?.className
              ?.toString()
      return kind to owner
    }

    return MarsMapSnapshot(
        name = map.className.toString().removeSuffix("Map"),
        areas =
            map.areas.sortedWith(compareBy({ it.row }, { it.column })).map { area ->
              val tile = tileKind(area.className)
              MarsMapAreaSnapshot(
                  row = area.row,
                  column = area.column,
                  kind = area.kind.toString().removeSuffix("Area").lowercase(),
                  bonuses = area.expandedBonusCodes.map(Char::toString),
                  tile = tile?.first,
                  owner = tile?.second,
              )
            },
    )
  }

  internal fun promptPlain(): String =
      with(agent) {
        val phase = list("Phase").singleOrNull() ?: "(no phase)"
        val checkpoint = game.timeline.checkpoint()
        "$optionCodes $phase ${game.vocabulary.petsName(agent.actor)}/$playerCount @$checkpoint> "
      }

  private val inputRegex = Regex("""^\s*(\S+)(.*)$""")

  public class UsageException(message: String? = null) : Exception(message.orEmpty())

  // Splits on semicolons and executes each chunk for interactive and scripted callers.
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
        RED -> RedMode(agent)
        YELLOW -> YellowMode(agent)
        GREEN -> GreenMode(agent)
        BLUE -> BlueMode(agent)
        PURPLE -> PurpleMode()
      }

  internal fun describeExecutionResults(result: TaskResult): List<String> {
    val changes =
        result.changes
            .filter { it.isVisibleInLog(game.reader) }
            .map { event -> game.vocabulary.renderPets(event) }

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

  private fun selectableTasks(ids: Set<TaskId>? = null): List<Task> =
      game.tasks
          .extract { it }
          .filter { it.assignee == agent.actor && (ids == null || it.id in ids) }

  internal fun taskLines(ids: Set<TaskId>? = null): List<String> =
      selectableTasks(ids).map { task -> game.vocabulary.renderPets(task, displayId = null) }

  internal fun onlyTask(): Task =
      selectableTasks().singleOrNull()
          ?: throw UsageException("this requires exactly one pending task")

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
    YELLOW("Task integrity: changes have consequences", MC),
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
    val type: Type = agent.resolve(name)
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

public data class PlayerSnapshot(
    val playerName: String,
    val phase: String?,
    val victoryPoints: Int,
    val terraformRating: Int,
    val cards: Int,
    val resources: List<PlayerResourceSnapshot>,
    val tags: List<PlayerTagSnapshot>,
)

public data class PlayerResourceSnapshot(val name: String, val stock: Int, val production: Int)

public data class PlayerTagSnapshot(val name: String, val count: Int)

public data class MarsMapSnapshot(val name: String, val areas: List<MarsMapAreaSnapshot>)

public data class MarsMapAreaSnapshot(
    val row: Int,
    val column: Int,
    val kind: String,
    val bonuses: List<String>,
    val tile: String?,
    val owner: String?,
)
