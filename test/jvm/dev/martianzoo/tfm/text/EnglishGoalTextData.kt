package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

internal object EnglishGoalTextData {
  internal data class Text(val englishName: String, val text: String)

  internal fun parse(source: String): Map<ClassName, Text> {
    val lines = source.trimEnd('\r', '\n').lineSequence().toList()
    require(lines.firstOrNull() == HEADER) { "Unexpected English goal-text header" }
    val result =
        lines.drop(1).associate { line ->
          val columns = line.split('\t')
          require(columns.size == COLUMN_COUNT) { "Malformed English goal-text row: $line" }
          cn(columns[CLASS_NAME]) to Text(columns[ENGLISH_NAME], columns[TEXT])
        }
    require(result.size == lines.size - 1) { "Duplicate English goal-text class name" }
    return result
  }

  private const val HEADER = "class_name\tenglish_name\ttext"
  private const val COLUMN_COUNT = 3
  private const val CLASS_NAME = 0
  private const val ENGLISH_NAME = 1
  private const val TEXT = 2
}
