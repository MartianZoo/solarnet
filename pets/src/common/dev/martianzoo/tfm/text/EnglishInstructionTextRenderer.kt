package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.ClassTable

/** Derives English instructions using the vocabulary loaded in [classTable]. */
public class EnglishInstructionTextRenderer(classTable: ClassTable) {
  private val english =
      English(
          classTable,
          TerraformingMarsDescribers.descriptions.filterKeys { it in classTable.allClassNames },
      )

  /**
   * Describes [instruction], addressing [player] as "you" when supplied. Use the task assignee's
   * Player class, or null for neutral wording. Unsupported fragments remain bracketed Pets. The
   * instruction and player must belong to the constructor's vocabulary.
   */
  public fun render(instruction: InstructionTree, player: ClassName? = null): String =
      english.describe(instruction, player)
}
