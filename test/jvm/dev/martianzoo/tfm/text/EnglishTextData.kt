package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

internal object EnglishTextData {
  internal data class Text(val englishName: String, val text: String)

  internal fun parse(source: String): Map<ClassName, Text> {
    val lines = source.trimEnd('\r', '\n').lineSequence().toList()
    require(lines.firstOrNull() in setOf(HEADER, "class_name\tenglish_name\tpublished_text")) {
      "Unexpected English text header"
    }
    val result =
        lines.drop(1).associate { line ->
          val columns = line.split('\t')
          require(columns.size == COLUMN_COUNT) { "Malformed English text row: $line" }
          cn(columns[CLASS_NAME]) to Text(columns[ENGLISH_NAME], columns[TEXT])
        }
    require(result.size == lines.size - 1) { "Duplicate English text class name" }
    return result
  }

  private const val HEADER = "class_name\tenglish_name\ttext"
  private const val COLUMN_COUNT = 3
  private const val CLASS_NAME = 0
  private const val ENGLISH_NAME = 1
  private const val TEXT = 2
}
