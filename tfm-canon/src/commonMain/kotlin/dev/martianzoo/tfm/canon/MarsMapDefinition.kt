package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.pets.util.Grid
import dev.martianzoo.tfm.canon.TfmClasses.TILE

public data class MarsMapDefinition(
    val className: ClassName,
    val areas: Grid<AreaDefinition>,
    val defaultMilestones: Set<ClassName> = emptySet(),
    val defaultAwards: Set<ClassName> = emptySet(),
) {
  public data class AreaDefinition(
      /** Prefix used by area class names, such as the `Tharsis_1_1` prefix. */
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
  ) {

    init {
      require(row >= 1) { "bad row: $row" }
      require(column >= 1) { "bad column: $column" }
    }

    val bonus: InstructionGroup? = bonusText?.let {
      InstructionGroup.of(parse<InstructionTree>(it))
    }

    public val asClassDeclaration: ClassDeclaration by lazy {
      ClassDeclaration(
          className = className,
          kind = CONCRETE,
          supertypes = setOf(kind.expression),
          properties =
              mapOf(
                  PropertyName("row") to NumberValue(row),
                  PropertyName("column") to NumberValue(column),
              ),
          authoredEffects = toEffects(bonus),
      )
    }

    public val className: ClassName = cn("${mapName}_${row}_$column")
  }

  private companion object {
    fun toEffects(bonus: InstructionGroup?) =
        listOfNotNull(
            bonus
                ?.let { InstructionGroup.createTree(it.instructions) }
                ?.takeUnless { it == NoOp }
                ?.let { Effect(TRIGGER, it, false) }
        )

    val TRIGGER: Trigger = OnGainOf.create(TILE.of(THIS))
  }
}
