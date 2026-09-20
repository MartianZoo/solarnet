package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName

/**
 * An internal organizational provider of declarations, data, metadata, and custom code.
 *
 * A Bundle is provenance and loading structure, not a selectable option or live component. Its
 * same-named Pets Module, when present, selects its content and represents its ambient rules in a
 * resolved game.
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
