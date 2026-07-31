package dev.martianzoo.data

import dev.martianzoo.pets.ast.ClassName

/** Fully resolved, immutable facts from which equivalent playable worlds can be constructed. */
public data class GamePremise(
    public val ruleset: Ruleset,
    public val rootClassNames: Set<ClassName>,
    public val actors: List<Actor>,
    public val initialComponents: List<String>,
)
