package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/** Card-specific draw filters that canonical Pets instructions do not yet represent. */
internal object EnglishFilteredDrawData {
  internal val byCardFront: Map<ClassName, EnglishDrawFilter> by lazy {
    parse(readEnglishFilteredDraws())
  }

  private fun parse(source: String): Map<ClassName, EnglishDrawFilter> {
    val lines = source.trimEnd('\r', '\n').lineSequence().toList()
    require(lines.firstOrNull() == HEADER) { "Unexpected English filtered-draw header" }

    val result =
        lines.drop(1).associate { line ->
          val columns = line.split('\t')
          require(columns.size == COLUMN_COUNT) { "Malformed English filtered-draw row: $line" }
          val filter =
              when (val encoded = columns[FILTER]) {
                "requirements" -> EnglishDrawFilter.Requirements
                else -> {
                  val (kind, className) =
                      encoded.split(':').takeIf { it.size == 2 }
                          ?: error("Malformed English draw filter: $encoded")
                  when (kind) {
                    "tag" -> EnglishDrawFilter.Tag(cn(className))
                    "icon" -> EnglishDrawFilter.Icon(cn(className))
                    else -> error("Unknown English draw filter kind: $kind")
                  }
                }
              }
          cn(columns[CARD_FRONT]) to filter
        }
    require(result.size == lines.size - 1) { "Duplicate English filtered-draw card front" }
    return result
  }

  private const val HEADER = "class_name\tdraw_filter"
  private const val COLUMN_COUNT = 2
  private const val CARD_FRONT = 0
  private const val FILTER = 1
}

internal sealed interface EnglishDrawFilter {
  data class Tag(val className: ClassName) : EnglishDrawFilter

  data class Icon(val className: ClassName) : EnglishDrawFilter

  data object Requirements : EnglishDrawFilter
}
