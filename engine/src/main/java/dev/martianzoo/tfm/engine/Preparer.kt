package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.engine.Game.ComponentGraph

internal class Preparer(
    private val agentReader: GameReader,
    private val components: ComponentGraph
) {
}
