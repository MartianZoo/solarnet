package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Instruction.Intensity

/** A renderer-facing modality resolved from a Pets instruction intensity. */
internal enum class Modality {
  REQUIRED,
  BEST_EFFORT,
  OPTIONAL,
}

internal fun Intensity?.modality(): Modality =
    when (this) {
      null,
      Intensity.MANDATORY -> Modality.REQUIRED
      Intensity.AMAP -> Modality.BEST_EFFORT
      Intensity.OPTIONAL -> Modality.OPTIONAL
    }
