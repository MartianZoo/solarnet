package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.TfmClasses.MEGACREDIT
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_RESOURCE

internal fun isStandardResource(className: ClassName): Boolean {
  val resourceClass = Canon.classTable.findClass(className) ?: return false
  return !resourceClass.abstract &&
      resourceClass.isSubtypeOf(Canon.classTable.getClass(STANDARD_RESOURCE))
}

internal fun componentNoun(className: ClassName, count: Int): String =
    when {
      className == MEGACREDIT -> "M€"
      className == plant && count != 1 -> "plants"
      else -> unCamelCase(className.toString())
    }

private fun unCamelCase(name: String): String = buildString {
  name.forEachIndexed { index, character ->
    val previous = name.getOrNull(index - 1)
    val next = name.getOrNull(index + 1)
    if (character == '_') {
      append(' ')
    } else {
      val startsWord =
          previous != null &&
              character.isUpperCase() &&
              (previous.isLowerCase() ||
                  previous.isDigit() ||
                  (previous.isUpperCase() && next?.isLowerCase() == true))
      if (startsWord) append(' ')
      append(character.lowercaseChar())
    }
  }
}

private val plant = cn("Plant")
