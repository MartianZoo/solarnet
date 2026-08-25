package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.canon.CardDefinition.Deck

/** Bundle-wide content selected by a Module; individual entries are deliberately unsupported. */
public data class BundleContentSelection(
    public val bundleName: ClassName,
    internal val kinds: Set<Kind> = Kind.entries.toSet(),
    internal val cardDecks: Set<Deck>? = null,
) {
  init {
    require(cardDecks?.isNotEmpty() != false)
    require(cardDecks == null || Kind.CARDS in kinds)
  }

  /** Independently selectable kinds of content contributed by a bundle. */
  public enum class Kind {
    CARDS,
    MAPS,
    MILESTONES,
    AWARDS,
    COLONY_TILES,
  }
}
