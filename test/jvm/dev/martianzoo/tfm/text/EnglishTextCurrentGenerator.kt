package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.displayName
import dev.martianzoo.tfm.canon.Canon
import java.io.File

private object EnglishTextCurrentGenerator {
  @JvmStatic
  fun main(args: Array<String>) {
    require(args.size == 3)
    val globalEvents = args[2] == "global-events"
    require(globalEvents || args[2] == "goals")
    val output = File(args[0])
    val refusalOutput = File(args[1])
    val published =
        if (globalEvents)
            EnglishTextData.parse(
                readEnglishCardText("english-global-event-published-wording-evidence.tsv")
            )
        else emptyMap()
    val previousOrder =
        if (globalEvents) published.keys else EnglishTextData.parse(output.readText()).keys
    val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)
    val milestone = Canon.classTable.getClass(cn("Milestone"))
    val award = Canon.classTable.getClass(cn("Award"))
    val globalEvent = Canon.classTable.getClass(cn("GlobalEvent"))
    val components =
        Canon.classTable.allClassNames
            .map(Canon.classTable::getClass)
            .filter {
              !it.abstract &&
                  if (globalEvents) it.isSubtypeOf(globalEvent)
                  else it.isSubtypeOf(milestone) || it.isSubtypeOf(award)
            }
            .associateBy { it.className }
    val orderedNames =
        previousOrder.filter(components::containsKey) +
            components.keys.filterNot(previousOrder::contains).sortedBy { it.toString() }
    val renderings = orderedNames.map { className ->
      val component = components.getValue(className)
      val rendering =
          if (globalEvents) english.renderGlobalEvent(component) else english.renderGoal(component)
      Row(className, published[className]?.englishName ?: displayName(Canon, className), rendering)
    }
    val rows = renderings.map { row ->
      listOf(row.className.toString(), row.englishName, row.rendering.text)
          .also { columns ->
            require(columns.none { value -> '\t' in value || '\n' in value || '\r' in value })
          }
          .joinToString("\t")
    }
    output.writeText(
        (listOf("class_name\tenglish_name\ttext") + rows).joinToString("\n", postfix = "\n")
    )
    val refusalRows =
        renderings
            .flatMap { row ->
              row.rendering.unresolved.map { unresolved ->
                Refusal(row.className, unresolved)
              }
            }
            .groupBy { refusal -> refusal.unresolved.reason }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<RefusalReason, List<Refusal>>> { it.value.size }
                    .thenBy { it.key.name }
            )
            .map { (reason, entries) ->
              val examples =
                  entries.take(2).joinToString(" | ") { refusal ->
                    "${refusal.className}: ${refusal.unresolved.node}"
                  }
              listOf(entries.size, reason, examples).joinToString("\t")
            }
    refusalOutput.writeText(
        (listOf("count\treason\texamples") + refusalRows).joinToString("\n", postfix = "\n")
    )
  }

  private data class Row(
      val className: ClassName,
      val englishName: String,
      val rendering: EnglishRendering,
  )

  private data class Refusal(val className: ClassName, val unresolved: Unresolved)
}
