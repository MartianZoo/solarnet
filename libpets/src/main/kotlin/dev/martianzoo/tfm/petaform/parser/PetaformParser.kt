package dev.martianzoo.tfm.petaform.parser

import dev.martianzoo.tfm.petaform.api.Expression

/**
 * Turns Petaform source code into the appropriate API objects.
 */
interface PetaformParser {
  /** Parses the entire `input`, expecting an Expression. */
  fun parseExpression(petaformSource: String) : Expression
}
