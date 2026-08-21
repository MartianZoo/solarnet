package dev.martianzoo.tfm.api

import dev.martianzoo.pets.ast.ClassName

/** An organizational provider of declarations, definitions, metadata, and custom code. */
public abstract class Bundle(
    public val bundleName: ClassName,
) : TfmAuthority() {
  /**
   * Bundle-wide content selections for Modules provided by this bundle. An omitted Module selects
   * every content kind from its own bundle.
   */
  public open val moduleContentSelections: Map<ClassName, Set<BundleContentSelection>> = emptyMap()

  final override val bundles: List<Bundle> = listOf(this)
}
