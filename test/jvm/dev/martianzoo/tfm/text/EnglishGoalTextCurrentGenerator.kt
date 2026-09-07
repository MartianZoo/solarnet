package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.canon.Canon
import java.io.File

private object EnglishGoalTextCurrentGenerator {
  @JvmStatic
  fun main(args: Array<String>) {
    require(args.size == 2)
    val output = File(args[0])
    val refusalOutput = File(args[1])
    val targets = EnglishGoalTextData.parse(readEnglishCardText("english-goal-text-goals.tsv"))
    val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)
    val renderings = targets.map { (className, target) ->
      val rendering = english.renderGoal(Canon.classTable.getClass(className))
      GoalRow(className, target.englishName, rendering)
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
                GoalRefusal(row.className, unresolved)
              }
            }
            .groupBy { refusal -> refusal.unresolved.reason }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<RefusalReason, List<GoalRefusal>>> { it.value.size }
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

  private data class GoalRow(
      val className: ClassName,
      val englishName: String,
      val rendering: EnglishGoalRendering,
  )

  private data class GoalRefusal(val className: ClassName, val unresolved: Unresolved)
}
