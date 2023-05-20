package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasClassName.Companion.classNames
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression

public abstract class MClassTable {
  public companion object {
    private val cache = mutableMapOf<GameSetup, MClassTable>()

    /** Loads only the classes required for [setup] and returns a frozen class table. */
    public fun forSetup(setup: GameSetup): MClassTable {
      if (setup in cache) return cache[setup]!!

      val toLoad: List<HasClassName> = setup.allDefinitions() + setup.players()

      val loader = MClassLoader(setup.authority)
      loader.loadAll(toLoad.classNames())

      if ("P" in setup.bundles) loader.load(ClassName.cn("PreludePhase")) // TODO eww

      return loader.freeze().also { cache[setup] = it }
    }
  }

  abstract val authority: Authority

  /** The `Component` class, which is the root of the class hierarchy. */
  abstract val componentClass: MClass

  /** The `Class` class, the other class that is required to exist. */
  abstract val classClass: MClass

  /** All classes loaded by this class loader; can only be accessed after the loader is frozen. */
  abstract val allClasses: Set<MClass>
  internal abstract val allClassNamesAndIds: Set<ClassName>

  abstract val transformers: Transformers

  /**
   * Returns the [MClass] whose [MClass.className] or [MClass.shortName] is [name], or throws an
   * exception.
   */
  abstract fun getClass(name: ClassName): MClass

  /** Returns the [MType] represented by [expression]. */
  abstract fun resolve(expression: Expression): MType

  /** Returns the corresponding [MType] to [type] (possibly [type] itself). */
  abstract fun resolve(type: Type): MType

  internal abstract fun defaults(className: ClassName): Defaults
  abstract val cacheSize: Int
}
