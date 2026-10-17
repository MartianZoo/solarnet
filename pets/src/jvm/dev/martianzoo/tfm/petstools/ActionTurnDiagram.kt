package dev.martianzoo.tfm.petstools

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import java.io.File
import java.security.MessageDigest

/**
 * A source-derived teaching view, not a simulator. Editorial choices select the subject, panel
 * order, and headings; the Pets AST supplies every rule, condition, instruction, and destination.
 * Instructions retain their lexical variables. A destination links to a class of events, not a
 * promise that a particular choice or gated instruction will execute.
 */
internal object ActionTurnDiagram {
  private data class Panel(val title: String, val events: Set<String>)

  private data class Rule(
      val owner: String,
      val path: String,
      val effect: Effect,
      val matchingEvents: Set<String>,
  ) {
    val line: Int
      get() = effect.sourceLocation?.line ?: error("Missing effect source location")
  }

  private val panels =
      listOf(
          Panel("Start the action phase", setOf("+ActionPhase")),
          Panel("Receive the baton", setOf("+Baton")),
          Panel("Choose the first action or pass", setOf("+OfferAction")),
          Panel(
              "Accept an action",
              setOf("+StandardAction", "+DoRequiredActions", "+PlayCard"),
          ),
          Panel("Keep the scope through its work", setOf("+ActionScope")),
          Panel("Finish an action", setOf("-ActionScope")),
          Panel("Choose the optional second action", setOf("-SecondAction")),
          Panel("Pass for the generation", setOf("+Pass")),
          Panel("Pass the baton", setOf("+PassBaton", "-Baton")),
      )
  private val eventPanels = panels.flatMapIndexed { i, p -> p.events.map { it to i } }.toMap()
  private val actionEntries = setOf("StandardAction", "DoRequiredActions", "PlayCard")

  @JvmStatic
  fun main(args: Array<String>) {
    require(args.size == 2) {
      "Usage: ActionTurnDiagram <Canon bundles directory> <output directory>"
    }
    val sources =
        File(args[0], "base")
            .walkTopDown()
            .filter { it.isFile && it.extension == "pets" }
            .sortedBy { it.path }
            .associate { it.relativeTo(File(args[0])).invariantSeparatorsPath to it.readText() }
    val output = File(args[1]).also { it.mkdirs() }
    File(output, "action-turns.dot").writeText(render(sources))
    File(output, "overview.dot").writeText(overview(sources))
    panels.indices.forEach { index ->
      File(output, "panel-${index + 1}.dot").writeText(render(sources, index))
    }
    File(output, "rules.md").writeText(evidence(sources))
    println("Wrote ${File(output, "action-turns.dot")}")
  }

  private fun rules(sources: Map<String, String>): List<Rule> {
    val declarations =
        sources.toSortedMap().flatMap { (path, source) ->
          parseClasses(source).map { path to it }
        }
    val parents = declarations.associate { (_, declaration) ->
      declaration.className.toString() to declaration.supertypes.map { it.className.toString() }
    }
    fun ancestors(name: String): Set<String> {
      val found = mutableSetOf<String>()
      val pending = mutableListOf(name)
      while (pending.isNotEmpty()) {
        val current = pending.removeLast()
        if (found.add(current)) pending.addAll(parents[current].orEmpty())
      }
      return found
    }
    val eventAncestors = eventPanels.keys.associateWith { ancestors(it.drop(1)) }
    val activePhaseClasses = ancestors("ActionPhase")
    // Retain listeners carried by ActionPhase or its ancestors, but not other phase branches.
    // Match triggers through declared supertypes: ActionPhase fires TurnBasedPhase listeners.
    return declarations
        .filter { (_, declaration) ->
          val owner = declaration.className.toString()
          "Phase" !in ancestors(owner) || owner in activePhaseClasses
        }
        .flatMap { (path, declaration) ->
          val owner = declaration.className.toString()
          declaration.authoredEffects.mapNotNull { effect ->
            val observed =
                effect.trigger.descendantsOfType<Trigger.BasicTrigger>().flatMap {
                  events(it, owner)
                }
            val matching =
                eventAncestors
                    .filter { (event, ancestors) ->
                      observed.any { it.first() == event.first() && it.drop(1) in ancestors }
                    }
                    .keys
            if (
                matching.isEmpty() ||
                    (owner in actionEntries &&
                        effect.instruction.descendantsOfType<Instruction.Gain>().none {
                          it.gaining.className.toString() == "ActionScope"
                        })
            )
                null
            else Rule(owner, path, effect, matching)
          }
        }
  }

  private fun event(sign: String, expression: Expression, owner: String): String =
      sign + expression.className.toString().let { if (it == "This") owner else it }

  private fun events(trigger: Trigger, owner: String): List<String> =
      when (trigger) {
        Trigger.WhenGain -> listOf("+$owner")
        Trigger.WhenRemove -> listOf("-$owner")
        is Trigger.OnGainOf -> listOf(event("+", trigger.expression, owner))
        is Trigger.OnRemoveOf -> listOf(event("-", trigger.expression, owner))
        is Trigger.Or -> trigger.triggers.flatMap { events(it, owner) }
        is Trigger.IfTrigger -> events(trigger.inner, owner)
        else -> error("Unreviewed trigger in $owner: $trigger")
      }

  private fun changes(rule: Rule): List<String> =
      rule.effect.instruction
          .descendantsOfType<Instruction.Change>()
          .flatMap { change ->
            listOfNotNull(
                change.gaining?.let { event("+", it, rule.owner) },
                change.removing?.let { event("-", it, rule.owner) },
            )
          }
          .distinct()

  private fun destinations(rule: Rule): List<String> = changes(rule).filter { it in eventPanels }

  // Line breaks reveal the authored instruction tree; they never flatten OR into parallel work or
  // THEN into unrelated effects. Leaf instructions keep their normalized Pets spelling.
  private fun instructionLines(tree: InstructionTree, indent: String = ""): List<String> =
      when (tree) {
        is InstructionGroup ->
            listOf("${indent}ALL {") +
                tree.instructions.flatMap { instructionLines(it, "$indent  ") } +
                "$indent}"
        is Instruction.Or ->
            listOf("${indent}CHOOSE / OR {") +
                tree.instructions.flatMap { instructionLines(it, "$indent  ") } +
                "$indent}"
        is Instruction.Then ->
            listOf("${indent}IN ORDER / THEN {") +
                tree.instructions.flatMap { instructionLines(it, "$indent  ") } +
                "$indent}"
        is Instruction.Dispatch ->
            listOf("$indent${tree.deferral.keyword}${tree.actor?.let { " $it" } ?: ""} {") +
                instructionLines(tree.body, "$indent  ") +
                "$indent}"
        is Instruction.Each ->
            listOf("${indent}EACH ${tree.selector} {") +
                instructionLines(tree.body, "$indent  ") +
                "$indent}"
        is Instruction.Gated ->
            listOf("${indent}IF ${tree.gate} {") +
                instructionLines(tree.inner, "$indent  ") +
                "$indent}"
        else -> listOf("$indent$tree")
      }

  fun render(sources: Map<String, String>, onlyPanel: Int? = null): String {
    val selected = rules(sources)
    require(selected.isNotEmpty()) { "No action-turn rules found" }
    return buildString {
      appendLine(
          "// Generated from Pets AST; do not edit. See rules.md for scope and source hashes."
      )
      appendLine("digraph ActionTurns {")
      appendLine(
          "graph [rankdir=LR, bgcolor=\"#f7f8fc\", fontname=\"Helvetica\", fontsize=24, labelloc=t,"
      )
      appendLine(
          "  label=" +
              quote(
                  if (onlyPanel == null)
                      "HOW ACTION-PHASE TURNS WORK\nAn event happens. Every matching live rule reacts. Follow the numbered destinations."
                  else ""
              ) +
              ","
      )
      appendLine("  pack=28, packmode=\"array_u2\", nodesep=0.20, ranksep=0.32, pad=0.3];")
      appendLine("node [shape=plain, fontname=\"Helvetica\", fontsize=12];")
      appendLine("edge [color=\"#a8b1c3\", arrowsize=0.65];")
      panels.forEachIndexed { panelIndex, panel ->
        if (onlyPanel != null && panelIndex != onlyPanel) return@forEachIndexed
        val panelRules = selected.filter {
          it.matchingEvents.any(panel.events::contains)
        }
        if (panelRules.isEmpty()) {
          check(onlyPanel == null) {
            "No rules for ${panel.title}; review the diagram's event selection"
          }
          return@forEachIndexed
        }
        appendLine("subgraph cluster_$panelIndex {")
        appendLine(
            "graph [id=\"panel-${panelIndex + 1}\", label=${quote("${panelIndex + 1}  ${panel.title}")}, fontsize=19, labeljust=l, color=\"#d9dfea\", style=\"rounded,filled\", fillcolor=white, margin=16, sortv=$panelIndex];"
        )
        appendLine(
            "event_$panelIndex [id=\"event-$panelIndex\", label=<" +
                table("EVENT", panel.events.map { "$it happens" }, "#24364b", white = true) +
                ">];"
        )
        panelRules.forEachIndexed { ruleIndex, rule ->
          val id = "p${panelIndex}r$ruleIndex"
          val ruleLabel =
              table(
                  "${rule.owner} reacts",
                  listOf("ON ${rule.effect.trigger}") +
                      panel.events
                          .filter {
                            it in rule.matchingEvents &&
                                it !in events(rule.effect.trigger, rule.owner)
                          }
                          .map { "$it matches this supertype trigger" } +
                      "${rule.path}:${rule.line}",
                  "#e9edf6",
              )
          val instructionColor =
              when {
                rule.effect.automatic -> "#dcf3e9"
                rule.effect.instruction.descendantsOfType<Instruction.Dispatch>().any {
                  it.deferral.keyword == "FINALLY"
                } -> "#fff0d6"
                rule.effect.instruction.descendantsOfType<Instruction.Or>().isNotEmpty() ->
                    "#e4edff"
                else -> "#f0f2f7"
              }
          val title = if (rule.effect.automatic) "AUTOMATIC  ::" else "WORK / OFFER  :"
          val destinationRows =
              destinations(rule)
                  .map { destination ->
                    val target = eventPanels.getValue(destination) + 1
                    "<TR><TD ALIGN=\"LEFT\" HREF=\"#panel-$target\"><FONT COLOR=\"#2456a6\">${html("→ $target  $destination")}</FONT></TD></TR>"
                  }
                  .joinToString("")
          val instructionLabel =
              table(
                  title,
                  instructionLines(rule.effect.instruction),
                  instructionColor,
                  extraRows = destinationRows,
              )
          appendLine(
              "${id}_rule [tooltip=${quote("${rule.owner}: ${rule.effect}")}, label=<$ruleLabel>];"
          )
          appendLine(
              "${id}_work [tooltip=${quote("${rule.effect.instruction}")}, label=<$instructionLabel>];"
          )
          appendLine("event_$panelIndex -> ${id}_rule -> ${id}_work;")
        }
        appendLine("}")
      }
      appendLine("}")
    }
  }

  /** Possible paths between the detailed panels, derived from their authored changes. */
  fun overview(sources: Map<String, String>): String = buildString {
    val selected = rules(sources)
    appendLine("digraph Overview {")
    appendLine("graph [rankdir=TB, bgcolor=\"#f7f8fc\", nodesep=0.35, ranksep=0.5, pad=0.2];")
    appendLine(
        "node [shape=box, style=\"rounded,filled\", fillcolor=\"#e9edf6\", color=\"#a8b1c3\", fontname=Helvetica, fontsize=14, margin=0.15];"
    )
    appendLine("edge [color=\"#7184a2\", arrowsize=0.7];")
    panels.forEachIndexed { index, panel ->
      if (selected.any { it.matchingEvents.any(panel.events::contains) }) {
        appendLine(
            "flow_$index [label=${quote("${index + 1}  ${panel.title}\n${panel.events.joinToString(" / ")}")}, URL=\"#panel-${index + 1}\"];"
        )
      }
    }
    selected
        .flatMap { rule ->
          rule.matchingEvents.flatMap { source ->
            destinations(rule).map { target ->
              eventPanels.getValue(source) to eventPanels.getValue(target)
            }
          }
        }
        .distinct()
        .sortedWith(compareBy({ it.first }, { it.second }))
        .forEach { (source, target) ->
          appendLine("flow_$source -> flow_$target;")
        }
    // Production is the endpoint of this action-phase view, not another expanded panel.
    val exits = selected.filter { "+ProductionPhase" in changes(it) }
    if (exits.isNotEmpty()) {
      appendLine("production [label=\"+ProductionPhase\", shape=oval, fillcolor=\"#dcf3e9\"];")
      exits
          .flatMap { it.matchingEvents }
          .map(eventPanels::getValue)
          .distinct()
          .sorted()
          .forEach {
            appendLine("flow_$it -> production;")
          }
    }
    appendLine("}")
  }

  private fun table(
      title: String,
      lines: List<String>,
      color: String,
      white: Boolean = false,
      extraRows: String = "",
  ): String {
    val ink = if (white) "#ffffff" else "#213047"
    val body =
        lines
            .flatMap { wrap(it, 52) }
            .joinToString("") {
              "<TR><TD ALIGN=\"LEFT\"><FONT FACE=\"Courier\" POINT-SIZE=\"11\" COLOR=\"$ink\">${html(it)}</FONT></TD></TR>"
            }
    return "<TABLE BORDER=\"0\" CELLBORDER=\"0\" CELLSPACING=\"0\" CELLPADDING=\"6\" BGCOLOR=\"$color\">" +
        "<TR><TD ALIGN=\"LEFT\"><FONT COLOR=\"$ink\"><B>${html(title)}</B></FONT></TD></TR>" +
        body +
        extraRows +
        "</TABLE>"
  }

  private fun wrap(text: String, width: Int): List<String> {
    val lines = mutableListOf<String>()
    var rest = text
    while (rest.length > width) {
      val cut = rest.lastIndexOf(' ', width).takeIf { it > 8 } ?: break
      lines += rest.take(cut)
      rest = "  " + rest.drop(cut + 1)
    }
    return lines + rest
  }

  private fun html(text: String): String =
      text
          .replace("&", "&amp;")
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("\"", "&quot;")
          .replace(" ", "&#160;")

  private fun quote(text: String): String =
      "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

  fun evidence(sources: Map<String, String>): String = buildString {
    appendLine("# Action-phase turn switchboard")
    appendLine()
    appendLine(
        "Generated from the current working-tree Pets files. Panel order and titles are editorial; every rule, guard, instruction, and numbered destination is extracted by the Pets parser. Destinations describe possible authored changes, including choices and gates, rather than guaranteed execution."
    )
    appendLine()
    appendLine(
        "Read each panel left to right. Every matching live rule reacts; vertical order is not execution order. An owner such as InRound or Pass must exist. This and Me@ retain their lexical Pets meaning; they do not always denote the player taking the action."
    )
    appendLine()
    appendLine(
        "Green means automatic (::); blue marks an OR offer; amber contains FINALLY. FINALLY waits for ordinary and LATER work, including other players' responses. Scope removal then leads to subsequent play. ALL means unordered grouped work; THEN is ordered. A numbered destination takes you to that event's panel. The overview connects those same possible destinations; read each panel's guards and choices before following an arrow. Follow the actual guards: in this view ActionPhase exists, so MAX 0 ActionPhase branches do not run."
    )
    appendLine()
    appendLine("## Reading routes")
    appendLine()
    appendLine(
        "Normal play starts at 1; follow the numbered destinations. At each return to panel 6, read the current SecondAction and ExtraAction conditions. Passing enters panel 8. Panel 2 explains why a player carrying Pass immediately forwards the Baton. Panel 9 covers rotation, solo play, and the final release into production."
    )
    appendLine()
    appendLine("## Scope of this view")
    appendLine()
    appendLine(
        "This scans all base/*.pets files for listeners on the displayed events. Rule owners are discovered from source. Event matching follows declared class supertypes; listeners carried by ActionPhase and its ancestors are retained, while other phase branches are excluded. Action-entry classes contribute their scope-opening effects only. Payment, card effects, dependency-driven expiry, inherited effect specialization, signal disposal, and task dispatch internals are not expanded. This is not a state-space explorer or a proof of reachable choices. Unrecognized trigger syntax fails generation instead of disappearing."
    )
    appendLine()
    appendLine("## Source fingerprint (SHA-256)")
    appendLine()
    sources.toSortedMap().forEach { (path, source) ->
      val hash =
          MessageDigest.getInstance("SHA-256")
              .digest(source.toByteArray(Charsets.UTF_8))
              .joinToString("") { "%02x".format(it) }
      appendLine("- `$path`: `$hash`")
    }
    appendLine()
    appendLine("## Extracted rules")
    rules(sources).forEach { rule ->
      appendLine()
      appendLine("### ${rule.owner} — ${rule.path}:${rule.line}")
      appendLine()
      appendLine("```pets\n${rule.effect}\n```")
    }
  }
}
