package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.PetNode

/** A Pets node retained as source because the current renderer declined to interpret it. */
internal data class Unresolved(val node: PetNode, val reason: RefusalReason)
