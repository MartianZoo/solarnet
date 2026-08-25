@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Then

internal val coloniesCustomClasses: Set<CustomClass> =
    setOf(
        ColoniesExpansion.ColoniesSetup,
    )

/** Namespace for Colonies' custom Pets implementations. */
private object ColoniesExpansion {
  private val RESERVE_TRADE_FLEET = cn("ReserveTradeFleet")

  internal object ColoniesSetup : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(RESERVE_TRADE_FLEET)

    override fun translate(reader: GameReader): Instruction {
      val fleetInstructions =
          reader
              .getComponents("Player")
              .map { player ->
                gain(RESERVE_TRADE_FLEET.of(player.expression))
              }
              .toList()
      return Then.create(fleetInstructions)
    }
  }
}
