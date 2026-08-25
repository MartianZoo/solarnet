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
   * selects its own map and the concrete members of its milestone and award pool superclasses.
   */
  public open val moduleContentSelections: Map<ClassName, Set<BundleContentSelection>> = emptyMap()

  /** Ordinary card pools belonging to individual Modules within this bundle. */
  public open val moduleCardDefinitions: Map<ClassName, Set<CardDefinition>> = emptyMap()

  final override val bundles: List<Bundle> = listOf(this)
}
