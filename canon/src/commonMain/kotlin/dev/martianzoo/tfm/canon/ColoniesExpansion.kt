@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.tfm.api.tfmRuleset
import dev.martianzoo.types.Type

internal val coloniesCustomClasses: Set<CustomClass> =
    setOf(
        ColoniesExpansion.AddColonyTile,
        ColoniesExpansion.ColoniesSetup,
    )

/** Namespace for Colonies' custom Pets implementations. */
internal object ColoniesExpansion {
  private val ADD_COLONY_TILE = cn("AddColonyTile")
  private val DELAYED_COLONY_TILE = cn("DelayedColonyTile")
  private val RESERVE_TRADE_FLEET = cn("ReserveTradeFleet")

  internal object AddColonyTile : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(DELAYED_COLONY_TILE)

    override fun translate(reader: GameReader, tileClassType: Type): Instruction {
      val name = tileClassType.expression.arguments.single().className
      val tile = reader.tfmRuleset.colonyTile(name)
      val resourceType = tile.resourceType
      return if (resourceType == null) {
        gain(name)
      } else {
        gain(
            DELAYED_COLONY_TILE.of(name.classExpression(), resourceType.classExpression()),
        )
      }
    }
  }

  internal object ColoniesSetup : CustomClass() {
    override val requiredClassNames: Set<ClassName> = setOf(ADD_COLONY_TILE, RESERVE_TRADE_FLEET)

    override fun translate(reader: GameReader): Instruction {
      val fleetInstructions =
          reader.getComponents("Player").map { player ->
            gain(RESERVE_TRADE_FLEET.of(player.expression))
          }
      val colonyBySelectionClass =
          reader.tfmRuleset.colonyTileDefinitions.associateBy { "${it.className}Selected" }
      val tileInstructions =
          reader.getComponents("SelectedColonyTile").map { selection ->
            val colony = colonyBySelectionClass.getValue(selection.className.toString())
            gain(ADD_COLONY_TILE.of(colony.className.classExpression()))
          }
      return Then.create(tileInstructions + fleetInstructions)
    }
  }
}
