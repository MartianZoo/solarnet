package dev.martianzoo.tfm.api

import dev.martianzoo.pets.ast.ClassName

/**
 * Bundle-wide content selected by a Module; individual definitions are deliberately unsupported.
 */
public data class BundleContentSelection(
    public val bundleName: ClassName,
    public val kinds: Set<Kind> = Kind.entries.toSet(),
) {
  /** Independently selectable kinds of content contributed by a bundle. */
  public enum class Kind {
    CARDS,
    STANDARD_ACTIONS,
    MAPS,
    MILESTONES,
    AWARDS,
    COLONY_TILES,
  }
}
