package dev.martianzoo.tfm.api

import dev.martianzoo.data.BundleMetadata
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Requirement

/** An organizational provider of declarations, definitions, metadata, and custom code. */
public abstract class Bundle(
    public val bundleName: ClassName,
) : TfmAuthority() {
  /** Premise-resolution metadata stored with this bundle. */
  public open val metadata: BundleMetadata = BundleMetadata()

  /**
   * Bundle-wide content selections for Modules provided by this bundle. An omitted Module selects
   * every content kind from its own bundle.
   */
  public open val moduleContentSelections: Map<ClassName, Set<BundleContentSelection>> = emptyMap()

  final override val bootstrapValidations: List<Set<Requirement>>
    get() = metadata.bootstrapValidations

  final override val bundles: List<Bundle> = listOf(this)
}
