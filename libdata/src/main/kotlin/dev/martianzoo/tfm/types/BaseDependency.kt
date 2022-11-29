package dev.martianzoo.tfm.types

// ("Owned", table.resolve("Anyone"))
// ("Tile", table.resolve("Area"))
// ("Cardbound", table.resolve("CardFront"))
// ("Production", table.resolve("StandardResource"), true)
// ("Adjacency", table.resolve("Tile"), 0)
// ("Adjacency", table.resolve("Tile"), 1)
data class BaseDependency(
    val dependentTypeName: String,
    val dependencyType: CType,
    val isTypeOnly: Boolean = false,
    val index: Int = 0,
)
