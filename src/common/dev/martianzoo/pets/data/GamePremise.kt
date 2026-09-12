package dev.martianzoo.pets.data

import dev.martianzoo.pets.api.SystemClasses.PLAYER
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.PremiseClassTable

/** The complete immutable input from which equivalent playable worlds are constructed. */
public data class GamePremise(
    public val catalog: Catalog,
    public val modules: Set<ClassName>,
    public val classSelections: Set<ClassSelection>,
    public val initialComponentTypes: Set<Expression>,
    /** Concrete Player Class Names in seat order. */
    public val playerNames: List<ClassName> = emptyList(),
    /** Concrete Component created by Admin immediately before the generated premise Class. */
    public val bootstrapClassName: ClassName? = null,
    /** Concrete configuration Class created during bootstrap, when the Catalog supplies one. */
    public val premiseClassName: ClassName? = null,
    /** Generated and ad-hoc declarations owned only by this premise. */
    public val premiseClassDeclarations: Set<ClassDeclaration> = emptySet(),
) {
  /** The premise-local declaration delta over the Catalog's reusable master table. */
  private val premiseClassTableLazy = lazy {
    PremiseClassTable(catalog.classTable, premiseClassDeclarations)
  }
  public val premiseClassTable: PremiseClassTable
    get() = premiseClassTableLazy.value

  /** The immutable active-class projection shared by every World built from this premise. */
  private val classTableLazy = lazy { ClassTable.forPremise(this) }
  public val classTable: ClassTable
    get() = classTableLazy.value

  init {
    val premiseDeclarationsByName = premiseClassTable.declarations
    val allKnownNames = catalog.allClassNames + premiseDeclarationsByName.keys
    val selectedNames = classSelections.map(ClassSelection::className)
    val invalidPlayerNames = playerNames.filter { playerName ->
      val configuredClass = catalog.classTable.findClass(playerName)
      val premiseDeclaration = premiseDeclarationsByName[playerName]
      (configuredClass?.abstract ?: premiseDeclaration?.abstract ?: true) ||
          !premiseClassTable.isSubtypeOf(playerName, PLAYER)
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
    require(selectedNames.all { it in allKnownNames }) {
      "individual class selections must belong to the premise Catalog: " +
          (selectedNames - allKnownNames)
    }
    require(selectedNames.none { it in catalog.modules }) {
      "Modules must use the premise's Module selection: ${selectedNames.filter { it in catalog.modules }}"
    }
    val initialClassNames =
        initialComponentTypes.flatMap { it.descendantsOfType<ClassName>() }.toSet()
    require(initialClassNames.all { it in allKnownNames }) {
      "initial component types must belong to the premise Catalog"
    }
    premiseClassName?.let { className ->
      val declaration =
          premiseDeclarationsByName[className] ?: catalog.allClassDeclarations[className]
      require(declaration != null && !declaration.abstract && declaration.dependencies.isEmpty()) {
        "premise class must be a concrete dependency-free Catalog Class: $className"
      }
    }
    bootstrapClassName?.let { className ->
      val declaration =
          premiseDeclarationsByName[className] ?: catalog.allClassDeclarations[className]
      require(declaration != null && !declaration.abstract && declaration.dependencies.isEmpty()) {
        "bootstrap class must be a concrete dependency-free Catalog Class: $className"
      }
    }
  }

  /** The administrative Actor plus the seated Players. */
  public val actors: List<Actor>
    get() = playerNames.map(::Player) + ADMIN
}
