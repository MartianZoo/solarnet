package dev.martianzoo.pets.data

import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.types.ClassTable

/** The complete immutable input from which equivalent playable worlds are constructed. */
public data class GamePremise(
    public val catalog: Catalog,
    public val modules: Set<ClassName>,
    public val classSelections: Set<ClassSelection>,
    public val initialComponentTypes: Set<Expression>,
    /** Concrete Player Class Names in seat order. */
    public val playerNames: List<ClassName> = emptyList(),
    /**
     * Concrete configuration Class created immediately after Admin, when the Catalog supplies one.
     */
    public val premiseClassName: ClassName? = null,
) {
  /** The immutable active-class projection shared by every World built from this premise. */
  private val classTableLazy = lazy { ClassTable.forPremise(this) }
  public val classTable: ClassTable
    get() = classTableLazy.value

  init {
    val selectedNames = classSelections.map(ClassSelection::className)
    val invalidPlayerNames = playerNames.filter { playerName ->
      val playerClass = catalog.classTable.findClass(Player.CLASS_NAME)
      val configuredClass = catalog.classTable.findClass(playerName)
      playerClass == null ||
          configuredClass == null ||
          configuredClass.abstract ||
          !configuredClass.isSubtypeOf(playerClass)
    }
    require(invalidPlayerNames.isEmpty()) {
      "player names must be concrete Player classes: $invalidPlayerNames"
    }
    require(playerNames.distinct().size == playerNames.size) {
      "a game premise cannot seat the same player name more than once"
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
    premiseClassName?.let { className ->
      val declaration = catalog.allClassDeclarations[className]
      require(declaration != null && !declaration.abstract && declaration.dependencies.isEmpty()) {
        "premise class must be a concrete dependency-free Catalog Class: $className"
      }
    }
  }

  /** The administrative Actor plus the seated Players. */
  public val actors: List<Actor>
    get() = playerNames.map(::Player) + ADMIN

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
      )
}
