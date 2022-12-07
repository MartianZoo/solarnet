package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse

data class MarsAreaDefinition(
    /** Shortname of the MarsMap this area belongs to (e.g "Tharsis"). */
    val mapName: String,

    /** The row number of this area; the top row is row `1`. */
    val row: Int,

    /**
     * The column number of this area. Columns are slanted like `/`, and the leftmost column is
     * numbered `1`.
     */
    val column: Int,

    /**
     * The type of this area; standard types include "LandArea", "WaterArea", "VolcanicArea",
     * and "NoctisArea".
     */
    val typePetaform: String,

    /**
     * The petaform instruction for this map area's bonus.
     */
    val bonusPetaform: String?,

    /** In cryptic text form */
    val textual: String,
) : Definition {

  init {
    require(mapName.isNotEmpty())
    require(row >= 1) { "bad row: $row" }
    require(column >= 1) { "bad column: $column" }
    require(bonusPetaform?.isEmpty() != true) // nonempty if present
  }

  val bonus: Instruction? by lazy { bonusPetaform?.let { parse(it) } }
  val type: Expression by lazy { parse(typePetaform) }

  override val asComponentDefinition by lazy {
    val effects =
        if (bonusPetaform == null) {
          setOf()
        } else {
          setOf("Tile<This>: $bonusPetaform") // don't want to have to do this in code like that
        }
    ComponentDefinition(componentName(), supertypesPetaform = setOf(typePetaform), effectsPetaform = effects)
  }

  fun componentName() = "${mapName}${row}_$column"
}
