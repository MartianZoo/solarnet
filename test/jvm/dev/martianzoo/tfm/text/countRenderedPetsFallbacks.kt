package dev.martianzoo.tfm.text

internal fun countRenderedPetsFallbacks(text: String): Int {
  var depth = 0
  var count = 0
  text.forEach { character ->
    when (character) {
      '[' -> {
        if (depth == 0) count++
        depth++
      }
      ']' -> {
        require(depth > 0) { "Unmatched closing bracket in rendered text: $text" }
        depth--
      }
    }
  }
  require(depth == 0) { "Unclosed bracket in rendered text: $text" }
  return count
}
