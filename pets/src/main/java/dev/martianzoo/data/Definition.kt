package dev.martianzoo.data

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.ast.ClassName

/**
 * All information about a particular game component (card, map area, milestone, etc.). These
 * instances are later converted into [ClassDeclaration]s.
 */
public interface Definition : HasClassName {
  /** The class name this definition will be known as; see [ClassDeclaration.className]. */
  override val className: ClassName

  /** A shorter name, to be supplied as [ClassDeclaration.shortName]. */
  public val shortName: ClassName

  /**
   * A textual identifier for the bundle this definition belongs to, which can be used to easily
   * include or exclude sets of definitions. See `Canon` for reserved ids.
   */
  public val bundle: String

  /**
   * Converts this definition to a class declaration. As much information as possible should be
   * represented appropriately as effects of the class, so that there is less need for custom
   * instructions to refer back to this definition.
   */
  public val asClassDeclaration: ClassDeclaration
}
