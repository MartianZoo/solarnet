package dev.martianzoo.data

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression

/** The complete immutable input from which equivalent playable worlds are constructed. */
public data class GamePremise(
    public val authority: Authority,
    public val modules: Set<ClassName>,
    public val classSelections: Set<ClassSelection>,
    public val initialComponentTypes: Set<Expression>,
) {
  init {
    val selectedNames = classSelections.map(ClassSelection::className)
    require(modules.all { it in authority.modules }) {
      "unknown Modules: ${modules - authority.modules.keys}"
    }
    require(selectedNames.distinct().size == selectedNames.size) {
      "a game premise cannot select the same individual class more than once"
    }
    require(classSelections.all { it.requirement == null }) {
      "individual class selections must be exact, not conditional"
    }
    require(selectedNames.all { it in authority.allClassNames }) {
      "individual class selections must belong to the premise Authority"
    }
    require(selectedNames.none { it in authority.modules }) {
      "Modules must use the premise's Module selection: ${selectedNames.filter { it in authority.modules }}"
    }
    val initialClassNames =
        initialComponentTypes.flatMap { it.descendantsOfType<ClassName>() }.toSet()
    require(initialClassNames.all { it in authority.allClassNames }) {
      "initial component types must belong to the premise Authority"
    }
  }

  /** The administrative Actor plus the players selected as individual classes. */
  public val actors: List<Actor>
    get() =
        classSelections
            .filter(ClassSelection::included)
            .map(ClassSelection::className)
            .filter(Player::isValid)
            .sorted()
            .map(::Player) + ENGINE
}
