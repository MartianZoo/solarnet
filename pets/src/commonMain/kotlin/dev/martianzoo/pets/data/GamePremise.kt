package dev.martianzoo.pets.data

import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.types.ClassTable

/** The complete immutable input from which equivalent playable worlds are constructed. */
public data class GamePremise(
    public val catalog: Catalog,
    public val modules: Set<ClassName>,
    public val classSelections: Set<ClassSelection>,
    public val initialComponentTypes: Set<Expression>,
    /** User-facing player names in seat order. */
    public val playerNames: List<ClassName> = emptyList(),
) {
  /** The immutable active-class projection shared by every World built from this premise. */
  public val classTable: ClassTable by lazy { ClassTable.forPremise(this) }

  /** Canonical Player1 through Player5 class names for the occupied seats. */
  public val playerClassNames: List<ClassName> =
      Player.players(playerNames.size).map(Player::className)

  private val petsNameAliases: Map<ClassName, ClassName> =
      playerClassNames
          .zip(playerNames)
          .filter { (canonical, configured) -> canonical != configured }
          .toMap()

  init {
    val selectedNames = classSelections.map(ClassSelection::className)
    require(playerClassNames.all { it in catalog.allClassNames }) {
      "Catalog lacks player classes: ${playerClassNames - catalog.allClassNames}"
    }
    require(playerNames.distinct().size == playerNames.size) {
      "a game premise cannot seat the same player name more than once"
    }
    require(
        playerClassNames.zip(playerNames).all { (canonical, configured) ->
          configured == canonical || configured !in catalog.allClassNames
        }
    ) {
      "player name collides with a Catalog class"
    }
    require(modules.all { it in catalog.modules }) {
      "unknown Modules: ${modules - catalog.modules.keys}"
    }
    require(selectedNames.distinct().size == selectedNames.size) {
      "a game premise cannot select the same individual class more than once"
    }
    require(classSelections.all { it.requirement == null }) {
      "individual class selections must be exact, not conditional"
    }
    require(selectedNames.all { it in catalog.allClassNames }) {
      "individual class selections must belong to the premise Catalog: " +
          (selectedNames - catalog.allClassNames)
    }
    require(selectedNames.none { it in catalog.modules }) {
      "Modules must use the premise's Module selection: ${selectedNames.filter { it in catalog.modules }}"
    }
    val initialClassNames =
        initialComponentTypes.flatMap { it.descendantsOfType<ClassName>() }.toSet()
    require(initialClassNames.all { it in catalog.allClassNames }) {
      "initial component types must belong to the premise Catalog"
    }
  }

  /** The administrative Actor plus the seated Players. */
  public val actors: List<Actor>
    get() = playerClassNames.map { Player(it) } + ENGINE

  /** Builds presentation and input translation for this premise's projected class names. */
  public fun createVocabulary(
      activeClassNames: Set<ClassName>,
      locale: String = Vocabulary.ENGLISH,
      inputOnlySynonyms: Iterable<Pair<String, String>> = emptyList(),
  ): Vocabulary =
      Vocabulary.create(
          catalog,
          locale,
          inputOnlySynonyms,
          activeClassNames,
          petsNameAliases,
      )
}
