package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.data.Catalog

/**
 * An internal organizational provider of declarations, data, metadata, and custom code.
 *
 * A Bundle is provenance and loading structure, not a selectable option or live component. It may
 * provide both Modules and Content. A Module is an ambient-rule choice whose own declaration
 * closure is intrinsic; Content names authored game items intended for individual selection.
 * [Catalog.modules] associates a Module with applicable Content it selects by default. Neither role
 * requires another Pets supertype. Maps currently fill both roles, while parties do not yet support
 * individual selection.
 *
 * The current conventional loader associates card resources and colony tiles with a same-named
 * Module when one exists. That naming convention is packaging policy, not the semantic identity of
 * the Bundle, Module, or Content.
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
