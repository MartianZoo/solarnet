package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ClassDeclaration
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

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
    val typeText: String,

    /**
     * The pets instruction for this map area's bonus.
     */
    val bonusText: String?,
) : Definition {

  init {
    require(mapName.isNotEmpty())
    require(row >= 1) { "bad row: $row" }
    require(column >= 1) { "bad column: $column" }
    require(bonusText?.isEmpty() != true) // nonempty if present
  }

  val bonus: Instruction? by lazy { bonusText?.let { parse(it) } }
  val type by lazy { parse<TypeExpression>(typeText) as GenericTypeExpression }

  override val toClassDeclaration by lazy {
    ClassDeclaration(
        componentName(),
        abstract = false,
        supertypes = setOf(type),
        effectsRaw = { setOfNotNull(bonus?.let { Effect(trigger, it) }) }
    )
  }

  fun componentName() = "${mapName}${row}_$column"
}

val trigger = OnGain(te("Tile", THIS.type))
