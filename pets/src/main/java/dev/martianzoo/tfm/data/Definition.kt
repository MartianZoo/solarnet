package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.HasClassName

/** All information about a particular game component (card, map area, milestone, etc.). */
interface Definition : HasClassName {
  /**
   * The class name this definition will be known as, in its class form. Definitions sharing any
   * name in common cannot be used in the same game.
   */
  override val className: ClassName

  val shortName: ClassName

  /**
   * A textual identifier for the bundle this definition belongs to, which can be used to easily
   * include or exclude sets of definitions. See `Canon` for reserved ids.
   */
  val bundle: String

  /**
   * Converts this definition to a class declaration. As much information as possible should be
   * represented appropriately as effects of the class, so that there is less need for custom
   * instructions to refer back to this definition. For example, instead of `Tile` having an effect
   * like `@lookUpMapBonus(MarsArea)`, each `MarsArea` class should have an effect like `Tile<This>:
   * 2 Plant`.
   */
  val asClassDeclaration: ClassDeclaration
}
