package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.data.SpecialClassNames.MARS_MAP
import dev.martianzoo.tfm.data.SpecialClassNames.TILE
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.util.Grid

data class MarsMapDefinition(
    override val className: ClassName,
    override val bundle: String,
    val areas: Grid<AreaDefinition>
) : Definition {
  override val id by ::className
  override val asClassDeclaration =
      ClassDeclaration(
          className = className,
          id = id,
          abstract = false,
          supertypes = setOf(MARS_MAP.expr),
      )

  data class AreaDefinition(
      /** Mame of the MarsMapDefinition this area belongs to (e.g "Tharsis"). */
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
       * The kind of area; standard kinds include "LandArea", "WaterArea", "VolcanicArea", and
       * "NoctisArea".
       */
      val kind: ClassName,

      /** The pets instruction for this map area's bonus. */
      val bonusText: String?,

      /** A short code like `LPP` summarizing this map area. */
      val code: String,
  ) : Definition {

    init {
      require(row >= 1) { "bad row: $row" }
      require(column >= 1) { "bad column: $column" }
    }

    val bonus: Instruction? = bonusText?.let(::instruction)

    override val asClassDeclaration by lazy {
      ClassDeclaration(
          className = className,
          id = id,
          abstract = false,
          supertypes = setOf(kind.expr),
          effectsIn = bonus?.let { setOf(Effect(trigger, it, false)) } ?: setOf(),
      )
    }

    override val id =
        if (row > 9 || column > 9) {
          cn("${bundle}_${row}_$column")
        } else {
          cn("$bundle$row$column")
        }

    override val className = cn("${mapName}_${row}_$column")
  }
}

private val trigger = OnGainOf.create(TILE.addArgs(THIS))
