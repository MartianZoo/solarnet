package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
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
) {
  public data class AreaDefinition(
      /** Generated identity of this area, such as `Tharsis_1_1` or `Demo_01_01`. */
      val className: ClassName,

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

    /** Placement-bonus sigils with multiplier prefixes expanded. */
    public val expandedBonusCodes: List<Char> = MarsMapReader.expandBonusCodes(code.drop(1))

    public val asClassDeclaration: ClassDeclaration
      get() =
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
