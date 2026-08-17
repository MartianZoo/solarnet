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

  /** Configuration condition that must hold for this definition to be active. */
  public val setupRequirement: Requirement?
    get() = null

  /**
   * Converts this definition to a class declaration. As much information as possible should be
   * represented appropriately as properties or effects of the class, so that custom behavior does
   * not need to refer back to this definition.
   */
  public val asClassDeclaration: ClassDeclaration
}
