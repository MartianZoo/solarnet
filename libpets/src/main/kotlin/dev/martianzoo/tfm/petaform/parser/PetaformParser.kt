package dev.martianzoo.tfm.petaform.parser

import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Predicate

/**
 * Turns Petaform source code into the appropriate API objects.
 */
interface PetaformParser {
  /** Parses the entire `input`, expecting an Expression. */
  fun parseExpression(petaform: String) : Expression

  /** Parses the entire `input`, expecting a Predicate. */
  fun parsePredicate(petaform: String) : Predicate
}
