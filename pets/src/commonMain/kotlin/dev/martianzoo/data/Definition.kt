package dev.martianzoo.data

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Requirement

/**
 * Structured information about a game component not yet authored directly as a [ClassDeclaration].
 * These instances are later converted into declarations.
 */
public interface Definition : HasClassName {
  /** The class name this definition will be known as; see [ClassDeclaration.className]. */
  override val className: ClassName

  /** Condition under which ordinary content selection includes this definition. */
  public val automaticSelectionRequirement: Requirement?
    get() = null

  /** Non-bundle condition that explicit and automatic selection must both satisfy. */
  public val compatibilityRequirement: Requirement?
    get() = null

  /**
   * Converts this definition to a class declaration. As much information as possible should be
   * represented appropriately as properties or effects of the class, so that custom behavior does
   * not need to refer back to this definition.
   */
  public val asClassDeclaration: ClassDeclaration
}
