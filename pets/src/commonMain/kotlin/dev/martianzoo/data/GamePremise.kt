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
    /** The concrete player classes, in seat order. */
    public val playerClassNames: List<ClassName> =
        classSelections
            .filter(ClassSelection::included)
            .map(ClassSelection::className)
            .filter(Player::isDefaultClassName)
            .sorted(),
    /** Authority class names replaced only for this playable world's class catalog. */
    public val classNameAliases: Map<ClassName, ClassName> = emptyMap(),
) {
  /** The session-specific class catalog after applying [classNameAliases]. */
  public val runtimeClassAuthority: Authority = renamedAuthority(authority, classNameAliases)

  init {
    val selectedNames = classSelections.map(ClassSelection::className)
    require(modules.all { it in runtimeClassAuthority.modules }) {
      "unknown Modules: ${modules - runtimeClassAuthority.modules.keys}"
    }
    require(selectedNames.distinct().size == selectedNames.size) {
      "a game premise cannot select the same individual class more than once"
    }
    require(classSelections.all { it.requirement == null }) {
      "individual class selections must be exact, not conditional"
    }
    require(selectedNames.all { it in runtimeClassAuthority.allClassNames }) {
      "individual class selections must belong to the premise Authority: " +
          (selectedNames - runtimeClassAuthority.allClassNames)
    }
    require(selectedNames.none { it in runtimeClassAuthority.modules }) {
      "Modules must use the premise's Module selection: ${selectedNames.filter { it in runtimeClassAuthority.modules }}"
    }
    val initialClassNames =
        initialComponentTypes.flatMap { it.descendantsOfType<ClassName>() }.toSet()
    require(initialClassNames.all { it in runtimeClassAuthority.allClassNames }) {
      "initial component types must belong to the premise Authority"
    }
    require(playerClassNames.distinct().size == playerClassNames.size) {
      "a game premise cannot seat the same player class more than once"
    }
    require(
        playerClassNames.all { name ->
          classSelections.any { it.included && it.className == name }
        }
    ) {
      "every player class must be affirmatively selected"
    }
  }

  /** The administrative Actor plus the players selected as individual classes. */
  public val actors: List<Actor>
    get() = playerClassNames.map(::Player) + ENGINE
}
