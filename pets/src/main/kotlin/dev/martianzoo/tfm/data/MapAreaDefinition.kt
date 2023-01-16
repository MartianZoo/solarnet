package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.SpecialClassNames.TILE
import dev.martianzoo.tfm.pets.Parsing.parsePets
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Instruction

data class MapAreaDefinition(
    /** Mame of the MapDefinition this area belongs to (e.g "Tharsis"). */
    val mapName: ClassName,

    override val bundle: String,

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
    val type: ClassName,

    /**
     * The pets instruction for this map area's bonus.
     */
    val bonusText: String?,

    /** A short code like `LPP` summarizing this map area. */
    val code: String,
) : Definition {

  init {
    require(row >= 1) { "bad row: $row" }
    require(column >= 1) { "bad column: $column" }
    require(bonusText?.isEmpty() != true) // nonempty if present
  }

  val bonus: Instruction? by lazy { bonusText?.let { parsePets(it) } }

  override val asClassDeclaration by lazy {
    ClassDeclaration(
        id = id,
        name = name,
        abstract = false,
        supertypes = setOf(type.type),
        effectsRaw = bonus?.let {
          setOf(Effect(trigger, it, automatic = false))
        } ?: setOf(),
    )
  }

  override val id = ClassName("${bundle}${row}$column")
  override val name = ClassName("${mapName}_${row}_$column")
}

val trigger = OnGain(TILE.specialize(THIS.type))
