package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.canon.Canon

internal fun isStandardResource(className: ClassName): Boolean =
    concrete(className) && Describers[className].standardResource == true

internal fun componentNoun(className: ClassName, count: Int): String {
  return when (val noun = Describers[className].noun) {
    is ComponentDescriber.Noun.Counted -> if (count == 1) noun.singular else noun.plural
    is ComponentDescriber.Noun.Fixed -> noun.text
    ComponentDescriber.Noun.ClassName,
    null -> unCamelCase(className.toString())
  }
}

internal fun tagName(className: ClassName): Pair<String, Boolean>? {
  if (!concrete(className)) return null
  val style = Describers[className].tag ?: return null
  val ordinaryName = className.toString().removeSuffix("Tag").lowercase()
  val isPlanetTag = style == ComponentDescriber.Tag.PLANET
  val name = if (isPlanetTag) ordinaryName.replaceFirstChar(Char::uppercaseChar) else ordinaryName
  return name to isPlanetTag
}

internal fun cardResourceNoun(className: ClassName, count: Int): String? {
  if (!concrete(className)) return null
  val style = Describers[className].cardResource ?: return null
  val noun = unCamelCase(className.toString())
  return noun +
      when (style) {
        ComponentDescriber.CardResource.ORDINARY -> if (count == 1) "" else "s"
        ComponentDescriber.CardResource.SUFFIXED -> if (count == 1) " resource" else " resources"
      }
}

private fun concrete(className: ClassName): Boolean {
  val componentClass = Canon.classTable.findClass(className) ?: return false
  return !componentClass.abstract
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
