package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.pets.util.Grid
import dev.martianzoo.tfm.canon.TfmClasses.PLACEMENT

/**
 * A map's Pets class [className] and the areas described by its source diagram. [areas] keeps their
 * hex-grid coordinates and diagram order; it does not describe live tile placement.
 */
public data class MarsMapDefinition(
    val className: ClassName,
    val areas: Grid<AreaDefinition>,
) {
  /**
   * An authored hex on a [MarsMapDefinition]. [row] and [column] are positive, one-based diagram
   * coordinates. [bonusText] is the decoded Pets instruction, if any; [code] retains the compact
   * source spelling from which the area kind and bonus were read.
   *
   * @throws IllegalArgumentException if [row] or [column] is less than 1.
   */
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

      /** Pets instruction text for the placement bonus, or null when the area has no bonus. */
      val bonusText: String?,

      /** Source code for the area, such as `LPP` for land with two plant bonuses. */
      val code: String,
  ) {

    init {
      require(row >= 1) { "bad row: $row" }
      require(column >= 1) { "bad column: $column" }
    }

    /** Parsed [bonusText], or null when no bonus is supplied; invalid Pets text fails parsing. */
    val bonus: InstructionGroup? = bonusText?.let {
      InstructionGroup.of(parse<InstructionTree>(it))
    }

    /**
     * Bonus sigils after the area-kind prefix, with repeated and digit-prefixed sigils expanded.
     * For example, an area with code `L3P` has bonus codes `P`, `P`, `P`.
     */
    public val expandedBonusCodes: List<Char> = MarsMapReader.expandBonusCodes(code.drop(1))

    /**
     * Concrete Pets declaration for this area, with [kind] as its supertype, numeric `row` and
     * `column` properties, and an on-placement effect for a nonempty [bonus]. Delegate bonuses
     * cannot be combined with other bonus kinds.
     */
    public val asClassDeclaration: ClassDeclaration
      get() =
          ClassDeclaration(
              className = className,
              kind = CONCRETE,
              supertypes = listOf(kind.expression),
              properties =
                  mapOf(
                      PropertyName("row") to NumberValue(row),
                      PropertyName("column") to NumberValue(column),
                  ),
              authoredEffects = toEffects(bonus, expandedBonusCodes),
          )
  }

  private companion object {
    fun toEffects(bonus: InstructionGroup?, bonusCodes: List<Char>) =
        listOfNotNull(
            bonus
                ?.let { InstructionGroup.createTree(it.instructions) }
                ?.takeUnless { it == NoOp }
                ?.let { instruction ->
                  val delegateBonus = 'D' in bonusCodes
                  require(!delegateBonus || bonusCodes.all { it == 'D' }) {
                    "Delegate placement bonuses cannot be combined with other bonuses"
                  }
                  val trigger =
                      if (delegateBonus) IfTrigger(TRIGGER, DELEGATE_REQUIREMENT) else TRIGGER
                  Effect(trigger, instruction, false)
                }
        )

    val TRIGGER: Trigger = OnGainOf.create(PLACEMENT.of(THIS))
    val DELEGATE_REQUIREMENT: Requirement = parse("Class<PartyDelegate>")
  }
}
