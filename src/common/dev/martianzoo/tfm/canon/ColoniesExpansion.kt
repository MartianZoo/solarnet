@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Then

private val coloniesCustomClasses: Set<CustomClass> =
    setOf(
        ColoniesExpansion.ColoniesSetup,
    )

internal val coloniesExpansionBundle: StandardFormBundle =
    StandardFormBundle("ColoniesExpansion", coloniesCustomClasses)

/** Namespace for Colonies' custom Pets implementations. */
private object ColoniesExpansion {
  private val AVAILABLE_TRADE_FLEET = cn("AvailableTradeFleet")

  internal object ColoniesSetup : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(AVAILABLE_TRADE_FLEET)

    override fun translate(reader: GameReader): Instruction {
      val fleetInstructions =
          reader
              .getComponents("Player")
              .map { player -> gain(AVAILABLE_TRADE_FLEET.of(player.expression)) }
              .toList()
      return Then.create(fleetInstructions)
    }
  }
}
