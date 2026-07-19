package dev.martianzoo.data

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Requirement

/**
 * All information about a particular game component (card, map area, milestone, etc.). These
 * instances are later converted into [ClassDeclaration]s.
 */
public interface Definition : HasClassName {
  /** Stable identity within this definition kind. */
  public val definitionId: String
    get() = className.toString()

  /** The class name this definition will be known as; see [ClassDeclaration.className]. */
  override val className: ClassName

  /** A shorter name, to be supplied as [ClassDeclaration.shortName]. */
  public val shortName: ClassName

  /** The full identity of the bundle that owns this definition. */
  public val bundle: String

  /** Stable same-kind identity replaced by this definition, if any. */
  public val replacesId: String?
    get() = null

  /** Requirement that the configured game must satisfy for this definition to apply. */
  public val loadRequirement: Requirement?
    get() = null

  /**
   * Converts this definition to a class declaration. As much information as possible should be
   * represented appropriately as effects of the class, so that there is less need for custom
   * instructions to refer back to this definition.
   */
  public val asClassDeclaration: ClassDeclaration
}
