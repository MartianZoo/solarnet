package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.SpecialClassNames.TILE
import dev.martianzoo.tfm.pets.PetParser.parsePets
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

data class MapAreaDefinition(
    /** Shortname of the MarsMap this area belongs to (e.g "Tharsis"). */
    val mapName: String,

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
    val typeText: String,

    /**
     * The pets instruction for this map area's bonus.
     */
    val bonusText: String?,

    /** A short code like `LPP` summarizing this map area. */
    val code: String,
) : Definition {

  init {
    require(mapName.isNotEmpty())
    require(row >= 1) { "bad row: $row" }
    require(column >= 1) { "bad column: $column" }
    require(bonusText?.isEmpty() != true) // nonempty if present
  }

  val bonus: Instruction? by lazy { bonusText?.let { parsePets(it) } }
  val type by lazy { parsePets<TypeExpression>(typeText) as GenericTypeExpression } // TODO

  override val asClassDeclaration by lazy {
    ClassDeclaration(className,
        abstract = false,
        supertypes = setOf(type),
        effectsRaw = bonus?.let {
          setOf(Effect(trigger, it, automatic = false))
        } ?: setOf(),
    )
  }

  override val className = ClassName("${mapName}${row}_$column")
}

val trigger = OnGain(TILE.specialize(THIS.type))
