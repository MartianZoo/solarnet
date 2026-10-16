package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.Instruction.Quantifier

/** A renderer-facing modality resolved from a Pets instruction quantifier. */
internal enum class Modality {
  REQUIRED,
  BEST_EFFORT,
  OPTIONAL,
}

internal fun Quantifier?.modality(): Modality =
    when (this) {
      null,
      Quantifier.MANDATORY -> Modality.REQUIRED
      Quantifier.AMAP -> Modality.BEST_EFFORT
      Quantifier.OPTIONAL -> Modality.OPTIONAL
    }
