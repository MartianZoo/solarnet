package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/** Card-specific draw filters that canonical Pets instructions do not yet represent. */
internal object EnglishFilteredDrawData {
  internal val byCardFront: Map<ClassName, ClassName> by lazy { parse(readEnglishFilteredDraws()) }

  private fun parse(source: String): Map<ClassName, ClassName> {
    val lines = source.trimEnd('\r', '\n').lineSequence().toList()
    require(lines.firstOrNull() == HEADER) { "Unexpected English filtered-draw header" }

    val result =
        lines.drop(1).associate { line ->
          val columns = line.split('\t')
          require(columns.size == COLUMN_COUNT) { "Malformed English filtered-draw row: $line" }
          cn(columns[CARD_FRONT]) to cn(columns[FILTER_CLASS])
        }
    require(result.size == lines.size - 1) { "Duplicate English filtered-draw card front" }
    return result
  }

  private const val HEADER = "class_name\tfilter_class"
  private const val COLUMN_COUNT = 2
  private const val CARD_FRONT = 0
  private const val FILTER_CLASS = 1
}
