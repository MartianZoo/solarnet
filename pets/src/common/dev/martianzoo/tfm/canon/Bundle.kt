package dev.martianzoo.tfm.canon

import dev.martianzoo.catalog.Catalog
import dev.martianzoo.pets.ast.ClassName

/**
 * An internal organizational provider of declarations, data, metadata, and custom code.
 *
 * A Bundle is provenance and loading structure, not a selectable option or live component. It may
 * provide both Modules and Content. A Module is an ambient-rule choice whose own declaration
 * closure is intrinsic. Content classes are the nonabstract subclasses of `Content`: authored game
 * items intended for individual selection. [Catalog.modules] associates a Module with applicable
 * Content it selects by default. Maps inherit both `Content` and `Module`, while parties inherit
 * `Content` but do not yet support individual selection.
 *
 * A bundle has a short lowercase source name independent of its Classes. Its primary Module, when
 * present, controls default selection of the bundle's cards, goals, and colony tiles, plus
 * availability of supporting declarations. Canon supplies this association from its authored bundle
 * manifest.
 */
public abstract class Bundle(
    /** Source identity, also used by `name:cards`; never a Pets Class Name. */
    public val bundleName: String,
    /** Gameplay Module governing this bundle's default Content and support availability, if any. */
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
