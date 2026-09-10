package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName

/**
 * An organizational provider of declarations, category-specific data, metadata, and custom code.
 */
public abstract class Bundle(
    public val bundleName: ClassName,
) : TfmCatalog() {
  /** Declarations that live in this bundle's card resource, including auxiliary classes. */
  internal open val cardResourceClassNames: Set<ClassName> = emptySet()

  /** Card-resource declaration names grouped by the Module matching their resource directory. */
  internal open val moduleCardClassNames: Map<ClassName, Set<ClassName>> = emptyMap()

  final override val bundles: List<Bundle> = listOf(this)
}
