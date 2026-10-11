package dev.martianzoo.tfm.canon

import dev.martianzoo.catalog.Catalog
import dev.martianzoo.pets.ast.ClassName

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
 * A bundle has a short lowercase source name independent of its Classes. An explicit primary Module
 * association, when present, lets that Module select the bundle's cards and colony tiles by
 * default.
 */
public abstract class Bundle(
    /** Source identity, also used by `name:cards`; never a Pets Class Name. */
    public val bundleName: String,
    /** Gameplay Module that selects this bundle's cards and colony tiles by default, if any. */
    internal val primaryModuleName: ClassName? = null,
) : TfmCatalog() {
  init {
    require(BUNDLE_NAME.matches(bundleName)) { "invalid bundle name: $bundleName" }
  }

  /** Declarations that live in this bundle's card resource, including auxiliary classes. */
  internal open val cardResourceClassNames: Set<ClassName> = emptySet()

  /** Direct Content relationships keyed by the replacement. */
  internal open val replacements: Map<ClassName, ClassName> = emptyMap()

  /** Cards omitted from this bundle's default pool while remaining individually selectable. */
  internal open val excludedDefaultCards: Set<ClassName> = emptySet()

  final override val bundles: List<Bundle> = listOf(this)

  private companion object {
    private val BUNDLE_NAME = Regex("[a-z][a-z0-9]*")
  }
}
