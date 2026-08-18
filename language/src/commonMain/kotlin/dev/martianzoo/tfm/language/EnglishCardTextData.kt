package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

internal object EnglishCardTextData {
  internal val byCardFront: Map<ClassName, Text> by lazy { parse(readEnglishCardText()) }

  internal data class Text(val top: String, val bottom: String)

  private fun parse(source: String): Map<ClassName, Text> {
    val lines = source.trimEnd('\r', '\n').lineSequence().toList()
    require(lines.firstOrNull() == HEADER) { "Unexpected English card-text header" }

    val result =
        lines.drop(1).associate { line ->
          val columns = line.split('\t')
          require(columns.size == COLUMN_COUNT) { "Malformed English card-text row: $line" }
          cn(columns[CLASS_NAME]) to Text(bottom = columns[BOTTOM_TEXT], top = columns[TOP_TEXT])
        }
    require(result.size == lines.size - 1) { "Duplicate English card-text class name" }
    return result
  }

  private const val HEADER = "class_name\tid\tenglish_name\tbottom_text\ttop_text"
  private const val COLUMN_COUNT = 5
  private const val CLASS_NAME = 0
  private const val BOTTOM_TEXT = 3
  private const val TOP_TEXT = 4
}
