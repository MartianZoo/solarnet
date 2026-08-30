package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName

/**
 * An organizational provider of declarations, category-specific data, metadata, and custom code.
 */
public abstract class Bundle(
    public val bundleName: ClassName,
) : TfmCatalog() {
  /**
   * Exceptional cross-bundle or narrowed content selections. A Module named for its owning bundle
   * selects that bundle's ordinary cards and colony tiles without an entry here; a map Module
   * selects its own map and its explicitly configured milestone and award pools.
   */
  public open val moduleContentSelections: Map<ClassName, Set<BundleContentSelection>> = emptyMap()

  /** Class exclusions applied when individual Modules in this bundle are selected. */
  public open val moduleClassExclusions: Map<ClassName, Set<ClassName>> = emptyMap()

  /** Declarations that live in this bundle's card resource, including auxiliary classes. */
  internal open val cardResourceClassNames: Set<ClassName> = emptySet()

  /** Card-resource declaration names grouped by the Module matching their resource directory. */
  internal open val moduleCardClassNames: Map<ClassName, Set<ClassName>> = emptyMap()

  final override val bundles: List<Bundle> = listOf(this)
}
