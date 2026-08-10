package dev.martianzoo.script

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.HIDDEN
import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.Player
import dev.martianzoo.data.Task
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.engine.World
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
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
import dev.martianzoo.tfm.api.ApiUtils
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.TfmClasses.TILE
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.script.TFM_SCRIPT_CLASS_SYNONYMS
import dev.martianzoo.tfm.script.TfmColor
import dev.martianzoo.tfm.script.TfmColor.ENERGY
import dev.martianzoo.tfm.script.TfmColor.HEAT
import dev.martianzoo.tfm.script.TfmColor.MEGACREDIT
import dev.martianzoo.tfm.script.TfmColor.OCEAN_TILE
import dev.martianzoo.tfm.script.TfmColor.PLANT
import dev.martianzoo.tfm.script.commands.TfmBoardCommand
import dev.martianzoo.tfm.script.commands.TfmMapCommand
import dev.martianzoo.tfm.script.commands.TfmPayCommand
import dev.martianzoo.tfm.script.commands.TfmPlayCommand
import dev.martianzoo.tfm.script.commands.TfmSampleCommand
import dev.martianzoo.types.Type

/** @param useAnsiColors whether prompts and command output may contain ANSI escape sequences. */
public class ScriptSession(
    private val locale: String = Vocabulary.ENGLISH,
    internal val useAnsiColors: Boolean = false,
    hostCommands: (ScriptSession) -> List<ScriptCommand> = { emptyList() },
) {
  internal lateinit var game: World // TODO maybe remove and just have reader/events/...?

  internal lateinit var gameplay: TurnLayer
  internal lateinit var setup: OptionCodeTranslation.Setup

  internal var mode: ScriptMode = GREEN
  private val taskLabels = TaskLabels()

  private fun newGame(
      setup: OptionCodeTranslation.Setup,
      purple: Boolean = false,
  ) {
    val candidateGame = createGame(setup, locale)
    val candidateGameplay = candidateGame.gameplay(ENGINE) as TurnLayer // default autoexec mode
    if (purple) {
      TfmWorkflow.Auto(candidateGame).launch()
    } else {
      TfmWorkflow.Manual(candidateGame).setupPhase()
    }
    this.setup = setup
    game = candidateGame
    gameplay = candidateGameplay
    taskLabels.clear()
    if (purple) mode = PURPLE
  }

  /** Adapts the REPL's option-code syntax to an instruction executed in a setup world. */
  internal fun newGame(
      optionCodes: String,
      players: Int,
      selectedColonies: Set<ClassName> = emptySet(),
      purple: Boolean = false,
  ) {
    newGame(OptionCodeTranslation.setup(optionCodes, players, selectedColonies), purple)
  }

  internal fun newGame(
      setupInstruction: String,
      purple: Boolean = false,
  ) {
    val (candidateSetup, candidateGame) = createGame(setupInstruction, locale)
    val candidateGameplay = candidateGame.gameplay(ENGINE) as TurnLayer
    if (purple) {
      TfmWorkflow.Auto(candidateGame).launch()
    } else {
      TfmWorkflow.Manual(candidateGame).setupPhase()
    }
    setup = candidateSetup
    game = candidateGame
    gameplay = candidateGameplay
    taskLabels.clear()
    if (purple) mode = PURPLE
  }

  init {
    newGame("BM", 2)
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

    val resourceNames = listOf("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat")
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
            resourceNames.map {
              PlayerResourceSnapshot(
                  name = it,
                  stock = countIfLoaded(it),
                  production = tfm.production(cn(it)),
              )
            },
        tags = tagTypes.map { (name, type) -> PlayerTagSnapshot(name, countIfLoaded(type)) },
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
        name = map.className.toString(),
        areas =
            map.areas.sortedWith(compareBy({ it.row }, { it.column })).map { area ->
              val tile = tileKind(area.className)
              MarsMapAreaSnapshot(
                  row = area.row,
                  column = area.column,
                  kind = area.kind.toString().removeSuffix("Area").lowercase(),
                  bonuses = expandMapBonusCodes(area.code.drop(1)),
                  tile = tile?.first,
                  owner = tile?.second,
              )
            },
    )
  }

  private fun expandMapBonusCodes(code: String): List<String> {
    val result = mutableListOf<String>()
    var count = 1
    code.forEach { character ->
      if (character.isDigit()) {
        count = character.digitToInt()
      } else {
        repeat(count) { result += character.toString() }
        count = 1
      }
    }
    return result
  }

  internal fun promptPlain(): String =
      with(gameplay) {
        val phase = list("Phase").singleOrNull() ?: "(no phase)"
        val checkpoint = game.timeline.checkpoint()
        "${setup.optionCodes} $phase ${gameplay.actor}/${setup.players} @$checkpoint> "
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
        PURPLE -> PurpleMode(gameplay.godMode())
      }

  internal fun describeExecutionResults(result: TaskResult): List<String> {
    val changes =
        result.changes
            .filterNot { isHidden(it, game.reader) }
            .map { event ->
              game.vocabulary.renderPets(event)
            }

    val newTasks: Set<TaskId> = result.tasksSpawned
    val taskLines =
        if (newTasks.any()) {
          listOf("New tasks pending:") + taskLines(newTasks)
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

  internal fun selectableTasks(ids: Set<TaskId>? = null): List<Pair<String, Task>> {
    val allTasks = game.tasks.extract { it }
    val selectedTasks = if (ids == null) allTasks else allTasks.filter { it.id in ids }
    val existingIds = allTasks.mapTo(mutableSetOf()) { it.id }
    val labels =
        taskLabels.labelsFor(
            existingIds,
            selectedTasks.map { it.id },
            game.timeline.checkpoint().ordinal,
        )
    return selectedTasks.map { task -> labels.getValue(task.id) to task }
  }

  internal fun taskLines(ids: Set<TaskId>? = null): List<String> =
      selectableTasks(ids).map { (label, task) ->
        game.vocabulary.renderPets(task, queueAssignee = task.assignee, displayId = label)
      }

  internal fun resolveTaskLabel(label: String): TaskId? =
      taskLabels.resolve(game.tasks.ids(), label, game.timeline.checkpoint().ordinal)

  internal fun restoreTaskLabelsAfterRollback(rollbackOrdinal: Int) {
    taskLabels.restoreAfterRollback(game.tasks.ids(), rollbackOrdinal)
  }

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
    // In case a configured synonym or definition id was used
    val type: Type = gameplay.resolve(name)
    return Player(type.className)
  }

  internal fun canonicalColonyName(name: String): ClassName =
      colonyInputVocabulary.canonicalName(cn(name))

  private val colonyInputVocabulary: Vocabulary by lazy {
    Vocabulary.create(
        Canon.resolve(setOf(cn("TerraformingMars"), cn("ColoniesExpansion"))),
        locale,
        TFM_SCRIPT_CLASS_SYNONYMS,
    )
  }
}

internal fun createGame(
    setup: OptionCodeTranslation.Setup,
    locale: String = Vocabulary.ENGLISH,
): World {
  val setupWorld =
      Engine.newSetupWorld(
          Canon.setupWorldDefinition(
              setup.players,
              Canon.GameOptions(setup.options, setup.excludedOptions),
              setup.selectedColonies,
          ),
          locale,
          inputOnlySynonyms = TFM_SCRIPT_CLASS_SYNONYMS,
      )
  return Engine.newGame(setupWorld, Canon::assemble)
}

private fun createGame(
    setupInstruction: String,
    locale: String = Vocabulary.ENGLISH,
): Pair<OptionCodeTranslation.Setup, World> {
  val setupWorld =
      Engine.newSetupWorld(
          Canon.emptySetupWorldDefinition(),
          locale,
          inputOnlySynonyms = TFM_SCRIPT_CLASS_SYNONYMS,
      )
  setupWorld.gameplay(ENGINE).godMode().manual(setupInstruction)
  setupWorld.gameplay(ENGINE).godMode().manual("ValidateSetup")

  val options =
      setupWorld.reader.getComponents("GameOption").mapNotNullTo(linkedSetOf()) { type ->
        Canon.Option.entries.singleOrNull { it.className == type.className }
      }
  val players = setupWorld.reader.getComponents("Player").size
  val selectedColonies =
      setupWorld.reader.getComponents("SelectedColonyTile").mapTo(linkedSetOf()) { it.className }
  val setup =
      OptionCodeTranslation.Setup(
          OptionCodeTranslation.optionCodes(options),
          players,
          options,
          emptySet(),
          selectedColonies,
      )
  return setup to Engine.newGame(setupWorld, Canon::assemble)
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
