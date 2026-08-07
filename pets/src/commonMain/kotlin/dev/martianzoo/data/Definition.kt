package dev.martianzoo.data

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Requirement

/**
 * All information about a particular game component (card, map area, milestone, etc.). These
 * instances are later converted into [ClassDeclaration]s.
 */
public interface Definition : HasClassName {
  /** The class name this definition will be known as; see [ClassDeclaration.className]. */
  override val className: ClassName

  /** Setup-world condition that must hold for this definition to be active. */
  public val setupRequirement: Requirement?
    get() = null

  /**
   * Converts this definition to a class declaration. As much information as possible should be
   * represented appropriately as effects of the class, so that there is less need for custom
   * instructions to refer back to this definition.
   */
  public val asClassDeclaration: ClassDeclaration
}
