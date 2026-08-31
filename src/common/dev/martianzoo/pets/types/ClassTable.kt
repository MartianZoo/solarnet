package dev.martianzoo.pets.types

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.Exceptions
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.Dependency.TypeDependency

/** One Catalog master class universe or a playable active-class view backed by one. */
public abstract class ClassTable {
  public companion object {
    /** Forms and freezes the playable projection selected by [premise]. */
    public fun forPremise(premise: GamePremise): ClassTable {
      val masterTable = premise.catalog.classTable
      val initialClassNames =
          premise.initialComponentTypes.flatMap { it.descendantsOfType<ClassName>() }.toSet()
      val configurationNames: Set<ClassName> =
          premise.modules +
              premise.classSelections
                  .filter(ClassSelection::included)
                  .map(ClassSelection::className) +
              premise.playerClassNames +
              initialClassNames
      val moduleSelections = premise.modules.flatMap { premise.catalog.modules.getValue(it) }
      val (applicableModuleSelections, inapplicableModuleSelections) =
          moduleSelections.partition { selection ->
            selection.appliesTo(configurationNames, masterTable)
          }
      val moduleIncluded =
          applicableModuleSelections
              .filter(ClassSelection::included)
              .mapTo(linkedSetOf(), ClassSelection::className)
      val conditionallyExcluded =
          inapplicableModuleSelections
              .filter(ClassSelection::included)
              .mapTo(hashSetOf(), ClassSelection::className) - moduleIncluded
      val moduleExcluded =
          applicableModuleSelections
              .filterNot(ClassSelection::included)
              .mapTo(hashSetOf(), ClassSelection::className) + conditionallyExcluded
      val selectedByModules = moduleIncluded - moduleExcluded
      val explicitlyIncluded =
          premise.classSelections
              .filter(ClassSelection::included)
              .mapTo(linkedSetOf(), ClassSelection::className)
      val explicitlyExcluded =
          premise.classSelections
              .filterNot(ClassSelection::included)
              .mapTo(linkedSetOf(), ClassSelection::className)
      val excluded = (moduleExcluded - explicitlyIncluded) + explicitlyExcluded
      val roots =
          premise.modules +
              ((selectedByModules - explicitlyExcluded) + explicitlyIncluded) +
              initialClassNames +
              premise.actors.map(Actor::className)

      val table =
          ClassLoader.projection(
                  premise.catalog,
                  masterTable,
                  premise.modules,
                  premise.classSelections,
              )
              .apply { loadAll(roots) }
              .freeze()
      val unexpectedModules =
          premise.catalog.modules.keys.filterTo(linkedSetOf()) { table.isActive(it) } -
              premise.modules
      require(unexpectedModules.isEmpty()) {
        "structural activation selected unrequested Modules: $unexpectedModules"
      }
      val activePlayerClassNames =
          Player.players(5).map(Player::className).filterTo(linkedSetOf(), table::isActive)
      require(activePlayerClassNames == premise.playerClassNames.toSet()) {
        "active Player classes do not match occupied seats: $activePlayerClassNames"
      }
      val reactivated = excluded.filterTo(linkedSetOf(), table::isActive)
      require(reactivated.isEmpty()) {
        "structural activation conflicts with excluded classes: $reactivated"
      }
      PremiseViability.validate(table, roots)
      return table
    }
  }

  /** The Catalog whose compiled class universe backs this table. */
  internal abstract val catalog: Catalog

  /** Creates a dispatcher for the selected marked syntax configured by this table's Catalog. */
  public fun transformDispatcher(
      kinds: Set<String> = catalog.transformHandlerFactories.keys,
  ): PetTransformer {
    val handlers =
        catalog.transformHandlerFactories.filterKeys(kinds::contains).mapValues { (_, factory) ->
          factory(this)
        }
    return TransformHandler.dispatcher(handlers)
  }

  /** The Catalog-scoped table whose compiled class universe backs this projection. */
  internal abstract val masterTable: ClassTable

  /** Immutable component-count limits compiled for the classes active in this table. */
  public val componentLimits: ClassLimitTable by lazy { ClassLimitTable.create(this) }

  /** The `Component` class, which is the root of the class hierarchy. */
  public abstract val componentClass: Class

  /** The `Class` class, the other class that is required to exist. */
  public abstract val classClass: Class

  /** Every class inhabited in this view. A master table contains its complete universe. */
  public abstract fun allClasses(): Set<Class>

  /** Every class name inhabited in this view. */
  public abstract val allClassNames: Set<ClassName>

  /** Returns the Catalog-known [Class] having this canonical name. */
  public abstract fun findClass(name: ClassName): Class?

  /** Returns the [Class] having this canonical name, or throws. */
  public fun getClass(name: ClassName): Class =
      findClass(name) ?: throw Exceptions.classNotFound(name)

  /** Returns the inhabited [Class] having this canonical name; null if unknown or inactive. */
  public fun findActiveClass(name: ClassName): Class? = findClass(name)?.takeIf(::isActive)

  /** Whether [name] names a class inhabited in this view. */
  public fun isActive(name: ClassName): Boolean = name in allClassNames

  /** Whether [klass] belongs to this Catalog universe and is inhabited in this view. */
  public fun isActive(klass: Class): Boolean =
      klass.classTable === masterTable && klass.className in allClassNames

  /** Whether [type] belongs to the Catalog universe backing this table. */
  public fun knows(type: Type): Boolean = type.classTable === masterTable

  /** Whether [type] and every structural dependency it binds are inhabited in this view. */
  public fun isActive(type: Type): Boolean =
      knows(type) && isActive(type.rootClass) && type.dependencies.activeIn(this)

  private val activeSubclassesByClass = mutableMapOf<Class, Set<Class>>()

  /** Active subclasses of [klass], including [klass] itself when it is active. */
  public fun allSubclasses(klass: Class): Set<Class> {
    require(klass.classTable === masterTable) { "$klass belongs to a different Catalog" }
    if (this === masterTable) return klass.allSubclasses()
    return activeSubclassesByClass.getOrPut(klass) {
      klass.allSubclasses().filterTo(linkedSetOf(), ::isActive)
    }
  }

  private val activeDirectSubclassesByClass = mutableMapOf<Class, Set<Class>>()

  /** Active subclasses exactly one nominal step below [klass]. */
  public fun directSubclasses(klass: Class): Set<Class> {
    require(klass.classTable === masterTable) { "$klass belongs to a different Catalog" }
    if (this === masterTable) return klass.directSubclasses()
    return activeDirectSubclassesByClass.getOrPut(klass) {
      klass.directSubclasses().filterTo(linkedSetOf(), ::isActive)
    }
  }

  /** Active concrete structural narrowings of [type]. */
  public fun allConcreteSubtypes(type: Type): Sequence<GroundType> {
    val type = type.groundType
    require(type.classTable === masterTable) { "$type belongs to a different Catalog" }
    return allSubclasses(type.rootClass).asSequence().filterNot(Class::abstract).flatMap { klass ->
      val dependencies = type.dependencies glb klass.baseType.dependencies
      if (dependencies == null) {
        emptySequence()
      } else {
        concreteSubtypesSameClass(klass.withAllDependencies(dependencies))
      }
    }
  }

  /** Active concrete structural narrowings with the same root Class as [type]. */
  public fun concreteSubtypesSameClass(type: Type): Sequence<GroundType> {
    val type = type.groundType
    require(type.classTable === masterTable) { "$type belongs to a different Catalog" }
    if (type.rootClass.abstract || !isActive(type.rootClass)) return emptySequence()
    return type.dependencies.concreteSubtypesSameClass(type, this).filter(::isActive)
  }

  /** The sole active concrete narrowing of [type] that satisfies [info], if there is one. */
  public fun singleConcreteSubtype(type: Type, info: TypeInfo): GroundType? {
    val type = type.groundType
    if (type.rootClass.className == CLASS && type.refinement != null) {
      return allConcreteSubtypes(type).filter { it.narrows(type, info) }.take(2).singleOrNull()
    }
    val klass = allSubclasses(type.rootClass).singleOrNull { !it.abstract } ?: return null
    val intersection = type glb klass.baseType ?: return null
    val dependencies = intersection.dependencies.singleConcreteSubtype(info, this) ?: return null
    val candidate = intersection.rootClass.withAllDependencies(dependencies)
    return candidate.takeIf { !it.abstract && it.narrows(type, info) }
  }

  /** Returns the [Type] represented by [expression]. */
  public abstract fun resolve(expression: Expression): GroundType

  /** Resolves every type expression in [node], throwing if any is invalid. */
  public fun checkAllTypes(node: PetNode): Unit = node.visitDescendants {
    if (it is Expression) {
      resolve(it.uncomplemented()).expression
      false
    } else {
      true
    }
  }

  /**
   * Tests [candidate] against [constraint] within [domain]. The explicit domain lets a complement
   * expression act as a constraint without pretending that it has a standalone type.
   */
  public fun matchesConstraint(
      candidate: Type,
      constraint: Expression,
      domain: Type,
      info: TypeInfo,
  ): Boolean {
    require(candidate.classTable === masterTable && domain.classTable === masterTable) {
      "constraint types belong to a different Catalog"
    }
    val key = Key(domain.className, 0)
    val domainDependency = TypeDependency(key, domain.groundType)
    val constrained = domainDependency.intersect(constraint) ?: return false
    return TypeDependency(key, candidate.groundType).narrows(constrained, info)
  }
}
