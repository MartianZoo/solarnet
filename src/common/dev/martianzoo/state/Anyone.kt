package dev.martianzoo.state

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression

/** A runtime identity that can own game-state components. */
internal sealed interface Anyone : HasClassName, HasExpression
