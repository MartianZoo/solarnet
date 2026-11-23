package dev.martianzoo.tfm.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Deferral.NOW
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Dispatch
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.state.tfmCatalog

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
      arguments(requireNotNull(configLines.firstOrNull()) { "Config needs players" }.text).also {
          names ->
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
  public val agents: Agents = Agents(TfmEngine.newGame(premise)) { NONE }
  private val players =
      agents.world.actors.filterIsInstance<Player>().associate { it.toString() to agents[it] }

  /** Executes the archive once. Malformed entries and illegal tasks fail with their source line. */
  public fun run(afterEntry: () -> Unit = {}) {
    cardNames?.start(agents)
    agents[ADMIN].beginOperation("SetupPhase FROM BootstrapPhase")
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
      val args =
          call
              ?.groupValues
              ?.get(2)
              ?.let {
                if (name == "SelectTask")
                    listOf(it).also {
                      require(it.single().isNotBlank()) { "SelectTask requires a Pets instruction" }
                    }
                else arguments(it)
              }
              .orEmpty()
      val expectedArgs =
          when (name) {
            "PlayCard" -> 1..2
            "SelectTask",
            "UseAction1",
            "UseAction2",
            "UseStandardProject" -> 1..1
            "Pay" -> 1..Int.MAX_VALUE
            "ExMachina",
            "Setup",
            "Research",
            "Production",
            "WorldGovernment",
            "GlobalEvent",
            "SendDelegate0",
            "SendDelegate5",
            "Trade1",
            "Trade2",
            "Trade3",
            "UsePolicy",
            "ConvertHeat",
            "ConvertPlants",
            "ClaimMilestone",
            "FundAward",
            "DoRequiredActions",
            "Pass" -> 0..0
            else -> null
          }
      require(context == null || (expectedArgs == null && entry.body == null)) {
        "VIA qualifies a Pets task, not an archive command or block"
      }
      if (expectedArgs != null) {
        require(args.size in expectedArgs) { "Unexpected arguments for $name: $args" }
      }
      when (name) {
        "Setup",
        "Research",
        "Production",
        "WorldGovernment",
        "GlobalEvent",
        "ExMachina" -> require(!entry.body.isNullOrEmpty()) { "$name requires a nonempty body" }
        "Pay",
        "SelectTask",
        "Pass" -> require(entry.body == null) { "$name does not take a body" }
        else ->
            if (expectedArgs == null) {
              require(entry.body == null) { "Unknown block: $text" }
            }
      }
      // A recipient's explicit request may include several pending NOW releases.
      if (!topLevel && inherited != null && inherited !== agent && name != "ExMachina") {
        require((expectedArgs == null || name == "Pay") && entry.body == null) {
          "A recipient response must be a Pets task or Pay"
        }
        require(context == null) { "VIA on a recipient response is not supported" }
        if (name == "Pay") pay(agent, args, inherited)
        else agent.doTask(text, releasedBy = inherited)
        return
      }
      val actions =
          setOf(
              "PlayCard",
              "UseAction1",
              "UseAction2",
              "UseStandardProject",
              "SendDelegate0",
              "SendDelegate5",
              "Trade1",
              "Trade2",
              "Trade3",
              "UsePolicy",
              "ConvertHeat",
              "ConvertPlants",
              "ClaimMilestone",
              "FundAward",
              "DoRequiredActions",
              "Pass",
          )
      if (topLevel && name in actions && agent.has("ActionPhase")) {
        val holder = players.values.singleOrNull { it.has("Baton") }
        if (holder != null && holder !== agent) {
          check(holder.has("MAX 0 ActionScope")) { "Previous action is unfinished" }
          holder.doTask("Ok")
        }
      }
      when (name) {
        "PlayCard" -> {
          val card = args[0]
          val back = cardBack(agents.world.reader.tfmCatalog.card(cn(card)))!!.className
          val location = args.getOrElse(1) { "Hand" }
          val granted =
              agent.tasks
                  .extract { it }
                  .any {
                    cn("PlayCard") in it.instruction.descendantsOfType<ClassName>()
                  }
          if (!granted) agent.doTask("UseAction<PlayCardAction, Action1>")
          agent.doTask("PlayCard<Class<$back>, Class<$card>, $location>")
        }
        "SelectTask" -> agent.selectTask(args.single())
        "Pay" -> pay(agent, args)
        "ExMachina" -> {
          require(entry.body!!.all { it.body == null }) {
            "ExMachina requires flat Pets instructions"
          }
          agent.exMachina(entry.body!!.joinToString(", ") { it.text })
          return
        }
        "UseAction1",
        "UseAction2" -> {
          val classes = agent.reader.classTable
          require(
              classes.getClass(cn(args.single())).isSubtypeOf(classes.getClass(cn("ActionCard")))
          ) {
            "$name requires an action card; use UsePolicy for the ruling policy"
          }
          useAction(
              agent,
              "UseActionOnCardAction",
              "UseAction<${args.single()}, Action${name.last()}>",
          )
        }
        "UseStandardProject" -> {
          useAction(agent, "UseStandardProjectAction", "UseAction<${args.single()}, Action1>")
        }
        "SendDelegate0" -> agent.doTask("UseAction<LobbyAction, Action1>")
        "SendDelegate5" -> agent.doTask("UseAction<PaidLobbyAction, Action1>")
        "Trade1",
        "Trade2",
        "Trade3" -> agent.doTask("UseAction<TradeAction, Action${name.last()}>")
        "UsePolicy",
        "ConvertHeat",
        "ConvertPlants",
        "ClaimMilestone",
        "FundAward" -> agent.doTask("UseAction<${name}Action, Action1>")
        "Setup",
        "Research",
        "Production",
        "WorldGovernment",
        "GlobalEvent" -> {
          val phase =
              when (name) {
                "Setup" -> "SetupPhase"
                "Research" -> "ResearchPhase"
                "Production" -> "ProductionPhase"
                else -> "SolarPhase"
              }
          check(agents[ADMIN].has(phase)) { "$name outside $phase" }
        }
        "Pass",
        "DoRequiredActions" -> agent.doTask(name)
        else -> if (context == null) agent.doTask(text) else agent.doTask(text, context)
      }
      entry.body.orEmpty().forEach { execute(it, agent, false) }
      if (topLevel && name in actions) {
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

  private fun useAction(agent: Agent, opener: String, request: String) {
    val instruction = agent.parseAs(InstructionTree::class, request) as InstructionTree
    val granted =
        agent.tasks
            .extract { it }
            .any {
              it.deferral == agent.tasks.earliestDeferral &&
                  instruction.narrows(it.instruction, agent.reader)
            }
    if (!granted) agent.doTask("UseAction<$opener, Action1>")
    agent.doTask(request)
  }

  private fun pay(agent: Agent, args: List<String>, releasedBy: Agent? = null) {
    val amounts = args.map { argument ->
      val gain =
          parse<InstructionTree>(argument) as? Gain
              ?: error("Pay requires positive, fixed resource amounts")
      require(gain.quantifier == null || gain.quantifier == MANDATORY) {
        "Pay requires exact amounts, without ? or *"
      }
      val resource = gain.scaledEx.expression.toString()
      val expression = gain.scaledEx.expression
      val held =
          expression.arguments.size == 1 &&
              agent.reader.classTable
                  .getClass(expression.className)
                  .isSubtypeOf(agent.reader.classTable.getClass(cn("CardResource")))
      require(resource in setOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat") || held) {
        "Unsupported payment resource: $resource"
      }
      val amount = gain.scaledEx.scalar.toString().toIntOrNull()
      require(amount != null && amount > 0) { "Pay requires a positive integer amount" }
      val payment =
          if (held) "PayFromCard<${expression.arguments.single()}>" else "Pay<Class<$resource>>"
      Triple(resource, amount, payment)
    }
    agents.world.timeline.atomic {
      fun releases() =
          releasedBy
              ?.tasks
              ?.extract { task ->
                val dispatch = task.instruction as? Dispatch
                if (dispatch?.deferral == NOW && dispatch.actor == agent.actor.expression)
                    task to ((dispatch.body as? Then)?.first ?: dispatch.body)
                else null
              }
              .orEmpty()
              .filterNotNull()
      fun declineUnused() {
        val paid = agent.count("Owed") == 0
        // Optional sources (e.g. unused Helion heat) can still block the zero-MC remainder.
        fun unused(instruction: InstructionTree): Boolean =
            (paid || NoOp.narrows(instruction, agent.reader)) &&
                instruction.descendantsOfType<Change>().any {
                  it.gaining?.className in setOf(cn("Pay"), cn("PayFromCard"))
                }
        // On another player's turn, payment offers arrive inside NOW releases too.
        releases()
            .filter { unused(it.second) }
            .sortedBy { if (NoOp.narrows(it.second, agent.reader)) 0 else 1 }
            .forEach { (task, _) ->
              releasedBy!!.doTask(task.id)
              agent.doTask("Ok")
            }
        agent.tasks
            .extract { it }
            .filter { unused(it.instruction) }
            .sortedBy { if (NoOp.narrows(it.instruction, agent.reader)) 0 else 1 }
            .forEach { agent.doTask("Ok", it.id) }
      }
      for ((resource, amount, payment) in amounts) {
        if (payment.startsWith("Pay<") && agent.count("Owed<Class<$resource>>") > 0) declineUnused()
        val request = "$amount $payment FROM $resource"
        if (releasedBy == null) agent.doTask(request)
        else {
          val instruction = agent.parseAs(InstructionTree::class, request) as InstructionTree
          val offer = releases().singleOrNull { instruction.narrows(it.second, agent.reader) }
          if (offer != null) {
            releasedBy.doTask(offer.first.id)
            agent.doTask(request)
          } else agent.doTask(request, releasedBy)
        }
      }
      check(agent.count("Owed") == 0) { "Pay did not cover the bill" }
      declineUnused()
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

  private fun arguments(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    val closing = mutableListOf<Char>()
    var start = 0
    val result = mutableListOf<String>()
    text.forEachIndexed { i, ch ->
      when (ch) {
        '<' -> closing += '>'
        '(' -> closing += ')'
        '[' -> closing += ']'
        '>',
        ')',
        ']' -> {
          require(closing.lastOrNull() == ch) { "Mismatched argument delimiter in $text" }
          closing.removeAt(closing.lastIndex)
        }
        ',' ->
            if (closing.isEmpty()) {
              result += text.substring(start, i).trim()
              start = i + 1
            }
      }
    }
    require(closing.isEmpty()) { "Unclosed argument delimiter in $text" }
    result += text.substring(start).trim()
    require(result.none { it.isEmpty() }) { "Empty argument in $text" }
    return result
  }
}
