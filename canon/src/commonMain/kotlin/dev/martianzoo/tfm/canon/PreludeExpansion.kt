@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.TransformNode
import dev.martianzoo.tfm.api.ApiUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.data.TfmClasses.MEGACREDIT
import dev.martianzoo.tfm.data.TfmClasses.PROD

/** The Prelude expansion rules currently supported by Canon. */
internal object PreludeExpansion :
    CanonicalBundle(name = "PreludeExpansion", legacyCode = "P", cards = true) {
  override val customClasses: Set<CustomClass> = setOf(GainLowestProduction)

  private object GainLowestProduction : CustomClass("GainLowestProduction") {
    override fun translate(reader: GameReader, owner: Type): Instruction {
      val lowest = lookUpProductionLevels(reader, owner.expression).values.min()
      val options =
          standardResourceNames(reader).mapNotNull {
            val target = if (it == MEGACREDIT) lowest + 5 else lowest
            if (target >= 0) parse<Instruction>("=$target $it: $it") else null
          }
      return TransformNode.wrap(Or.create(options), PROD)
    }
  }
}
