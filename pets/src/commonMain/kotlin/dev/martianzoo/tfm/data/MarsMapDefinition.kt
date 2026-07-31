package dev.martianzoo.tfm.data

import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.tfm.data.TfmClasses.MARS_MAP
import dev.martianzoo.tfm.data.TfmClasses.TILE
import dev.martianzoo.util.Grid

public data class MarsMapDefinition(
    override val className: ClassName,
    val areas: Grid<AreaDefinition>,
) : Definition {
  override val shortName: ClassName by ::className
  override val asClassDeclaration: ClassDeclaration =
      ClassDeclaration(
          className = className,
          shortName = shortName,
          kind = CONCRETE,
          supertypes = setOf(MARS_MAP.expression),
      )

  public data class AreaDefinition(
      /** Mame of the MarsMapDefinition this area belongs to (e.g "Tharsis"). */
      private val mapName: ClassName,

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

    val bonus: Instruction? = bonusText?.let(::parse)

    override val asClassDeclaration: ClassDeclaration by lazy {
      ClassDeclaration(
          className = className,
          shortName = shortName,
          kind = CONCRETE,
          supertypes = setOf(kind.expression),
          effects = toEffects(bonus),
      )
    }

    override val className: ClassName = cn("${mapName}_${row}_$column")
    override val shortName: ClassName by ::className
  }

  private companion object {
    fun toEffects(bonus: Instruction?) = listOfNotNull(bonus?.let { Effect(TRIGGER, it, false) })

    val TRIGGER: Trigger = OnGainOf.create(TILE.of(THIS))
  }
}
