package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.util.Grid

data class MapDefinition(
    val name: ClassName,
    val bundle: String,
    val areas: Grid<MapAreaDefinition>
) {

}
