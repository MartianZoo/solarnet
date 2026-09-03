package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName

/** Bundle-wide content selected by a Module; individual entries are deliberately unsupported. */
public data class BundleContentSelection(
    public val bundleName: ClassName,
    internal val kinds: Set<Kind> = Kind.entries.toSet(),
) {
  /** Independently selectable kinds of content contributed by a bundle. */
  public enum class Kind {
    CARDS,
    MAPS,
    COLONY_TILES,
  }
}
