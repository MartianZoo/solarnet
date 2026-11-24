package dev.martianzoo.tfm.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog

/**
 * Interprets a human-readable Terraforming Mars archive through Agents with Player autoexecution
 * disabled. [catalogForConfig] supplies custom content when required; ordinary imports use Canon.
 * Construction validates headers and creates the game. Call [run] once to execute the entries.
 * Missing Cards headers mean untracked play; supplied names are checked by [CardTracking].
 */
public class GameArchiveImporter(
    source: String,
    catalogForConfig: (GameConfig) -> TfmCatalog = { Canon },
) {
  private data class Entry(val text: String, val line: Int, val body: List<Entry>? = null)

  private val entries = parseEntries(source)

  private fun header(index: Int, name: String): Entry =
      requireNotNull(entries.getOrNull(index)) { "Missing $name block" }
          .also {
            require(
                it.text == name && it.body != null && it.body.all { child -> child.body == null }
            ) {
              "Archive line ${it.line}: expected a flat $name block"
            }
          }

  private val configEntry = header(0, "Config")
  private val configLines = configEntry.body!!
  private val names =
      TfmRoutines.splitArguments(
              requireNotNull(configLines.firstOrNull()) { "Config needs players" }.text
          )
          .also { names ->
            require(
                names.isNotEmpty() &&
                    names.distinct().size == names.size &&
                    names.all { Regex("[A-Za-z][A-Za-z0-9_]*").matches(it) && it != "Admin" }
            ) {
              "Config needs distinct player names"
            }
          }
  private val config =
      GameConfig(configLines.drop(1).joinToString("\n") { it.text }, *names.toTypedArray())
  private val deck = header(1, "GlobalEventDeck").body!!.map { parse<ClassName>(it.text) }
  private var bodyStart = 2
  private val rules =
      entries.getOrNull(bodyStart)?.takeIf { it.text == "SetupRules" }?.also { bodyStart++ }

  private fun pets(entry: Entry): String =
      entry.text + (entry.body?.joinToString("\n", " {\n", "\n}") { pets(it) } ?: "")

  private val localRules =
      rules
          ?.let { requireNotNull(it.body).flatMap { rule -> parseClasses(pets(rule)) }.toSet() }
          .orEmpty()
  private val cardEntry =
      entries.getOrNull(bodyStart)?.takeIf { it.text == "Cards" }?.also { bodyStart++ }
  private val cardNames = cardEntry?.let {
    require(!it.body.isNullOrEmpty() && it.body.all { line -> line.body == null }) {
      "Cards requires a nonempty flat header"
    }
    ArchiveCards(it.body.map { line -> line.text })
  }
  private val localSelections =
      config.includedClassNames.intersect(localRules.map { it.className }.toSet())
  private val premise =
      catalogForConfig(config)
          .gamePremise(
              config.copy(includedClassNames = config.includedClassNames - localSelections),
              additionalClassDeclarations = localRules,
          )
          .let {
            it.copy(
                drawOrder = deck,
                classSelections = it.classSelections + localSelections.map(::ClassSelection),
            )
          }
  /** The imported live game, including any successfully executed prefix after a failure. */
  public val agents: Agents = Agents(TfmEngine.newGame(premise), TfmRoutines.registered) { NONE }
  private val players =
      agents.world.actors.filterIsInstance<Player>().associate { it.toString() to agents[it] }

  /** Executes the archive once. Malformed entries and illegal tasks fail with their source line. */
  public fun run(afterEntry: () -> Unit = {}) {
    cardNames?.start(agents)
    agents.transact {
      agents.world
          .actorEngine(ADMIN)
          .addTasks(
              InstructionGroup.of(Parsing.parse<InstructionTree>("SetupPhase FROM BootstrapPhase!"))
          )
    }
    entries.drop(bodyStart).forEach {
      execute(it, null, true)
      afterEntry()
    }
    cardNames?.finish(agents)
  }

  private fun execute(entry: Entry, inherited: Agent?, topLevel: Boolean) {
    try {
      if (entry.text in players) {
        require(topLevel && inherited == null) { "Player blocks must be at top level" }
        require(!entry.body.isNullOrEmpty()) { "Player block requires a nonempty body" }
        entry.body.forEach { execute(it, players.getValue(entry.text), topLevel) }
        return
      }
      val prefix = Regex("^(\\w+):\\s*(.*)$").matchEntire(entry.text)
      val agent =
          prefix?.let { players.getValue(it.groupValues[1]) }
              ?: requireNotNull(inherited) {
                "A player is required"
              }
      val qualified =
          (prefix?.groupValues?.get(2) ?: entry.text).split(Regex("\\s+VIA\\s+"), limit = 2)
      val text = qualified.first()
      val context = qualified.getOrNull(1)?.let { parse<ClassName>(it) }
      val call = Regex("(\\w+)\\((.*)\\)").matchEntire(text)
      val name = call?.groupValues?.get(1) ?: text
      val routine = TfmRoutines.registered[name]
      val args =
          call
              ?.groupValues
              ?.get(2)
              ?.let {
                routine?.arguments(it) ?: TfmRoutines.splitArguments(it)
              }
              .orEmpty()
      val expectedArgs =
          when {
            name in TfmRoutines.phaseGroups -> 0..0
            else -> routine?.argumentCount
          }
      require(
          context == null ||
              (entry.body == null && (expectedArgs == null || name == "exMachinaDropTask"))
      ) {
        "VIA qualifies a Pets task or exMachinaDropTask"
      }
      if (expectedArgs != null) {
        require(args.size in expectedArgs) { "Unexpected arguments for $name: $args" }
      }
      when {
        name in TfmRoutines.phaseGroups ->
            require(!entry.body.isNullOrEmpty()) { "$name requires a nonempty body" }
        (routine != null && !routine.startsAction) || name == "pass" ->
            require(entry.body == null) { "$name does not take a body" }
        expectedArgs == null -> require(entry.body == null) { "Unknown block: $text" }
      }
      // A recipient's explicit request may include several pending NOW releases.
      if (!topLevel && inherited != null && inherited !== agent && name != "exMachina") {
        require(
            (expectedArgs == null || name in setOf("pay", "exMachinaDropTask")) &&
                entry.body == null
        ) {
          "A recipient response must be a Pets task, pay, or exMachinaDropTask"
        }
        require(context == null) { "VIA on a recipient response is not supported" }
        if (routine != null) agent.doRoutine(name, *args.toTypedArray(), releasedBy = inherited)
        else agent.doTask(text, releasedBy = inherited)
        return
      }
      when {
        name == "exMachinaDropTask" && context != null ->
            agent.exMachinaDropTask(args.single(), context)
        routine != null -> agent.doRoutine(name, *args.toTypedArray())
        name in TfmRoutines.phaseGroups -> TfmRoutines.checkPhase(agents, name)
        context == null -> agent.doTask(text)
        else -> agent.doTask(text, context)
      }
      entry.body.orEmpty().forEach { execute(it, agent, false) }
      if (topLevel && routine?.startsAction == true) {
        check(agent.has("MAX 0 ActionScope")) { "Action effects are unfinished" }
      }
      check(agent.autoExecPolicy == NONE)
    } catch (missing: CardTracking.NamesRequiredException) {
      throw missing
    } catch (failure: Exception) {
      throw IllegalStateException(
          "Archive line ${entry.line}: ${entry.text}\n${failure.message}",
          failure,
      )
    }
  }

  private fun parseEntries(source: String): List<Entry> {
    val lines =
        source
            .lineSequence()
            .mapIndexedNotNull { i, raw ->
              raw.substringBefore("//").trim().takeIf { it.isNotEmpty() }?.let { (i + 1) to it }
            }
            .toList()
    var cursor = 0
    fun block(nested: Boolean): List<Entry> {
      val result = mutableListOf<Entry>()
      while (cursor < lines.size) {
        val (line, text) = lines[cursor++]
        if (text == "}") {
          require(nested) { "Unexpected } at line $line" }
          return result
        }
        result +=
            if (text.endsWith("{")) Entry(text.dropLast(1).trim(), line, block(true))
            else Entry(text, line)
      }
      require(!nested) { "Missing closing brace" }
      return result
    }
    return block(false)
  }
}
