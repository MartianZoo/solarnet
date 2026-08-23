package dev.martianzoo.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GridTest {
  private val cells = List(5) { row -> List(5) { column -> Cell(row, column) } }.flatten()
  private val grid = Grid.grid(cells, Cell::row, Cell::column)

  @Test
  internal fun largestContiguousGroupSize() {
    grid.largestContiguousGroupSize(emptySet(), Cell::row, Cell::column) shouldBe 0

    val ringAroundCenter =
        setOf(
            grid[1, 1]!!,
            grid[1, 2]!!,
            grid[2, 3]!!,
            grid[3, 3]!!,
            grid[3, 2]!!,
            grid[2, 1]!!,
        )
    val separatePair = setOf(grid[4, 0]!!, grid[4, 1]!!)
    val isolated = grid[0, 4]!!

    grid.largestContiguousGroupSize(
        ringAroundCenter + separatePair + isolated,
        Cell::row,
        Cell::column,
    ) shouldBe 6
  }

  private data class Cell(val row: Int, val column: Int)
}
