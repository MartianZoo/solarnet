package dev.martianzoo.pets.util

import io.kotest.assertions.throwables.shouldThrow
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

  @Test
  internal fun lookupReturnsNullOutsideTheGrid() {
    grid[-1, 2] shouldBe null
    grid[5, 2] shouldBe null
    grid[2, -1] shouldBe null
    grid[2, 5] shouldBe null
  }

  @Test
  internal fun indexedViewsRejectCoordinatesOutsideTheGrid() {
    shouldThrow<IndexOutOfBoundsException> { grid.row(-1) }
    shouldThrow<IndexOutOfBoundsException> { grid.column(5)[0] }
    shouldThrow<IndexOutOfBoundsException> { grid.diagonal(-5) }
    shouldThrow<IndexOutOfBoundsException> { grid.diagonal(5) }
  }

  @Test
  internal fun columnAndDiagonalViewsAreFixedSize() {
    @Suppress("UNCHECKED_CAST") val column = grid.column(0) as MutableList<Cell?>
    @Suppress("UNCHECKED_CAST") val diagonal = grid.diagonal(0) as MutableList<Cell?>

    shouldThrow<IllegalStateException> { column.add(null) }
    shouldThrow<IllegalStateException> { column.removeAt(0) }
    shouldThrow<IllegalStateException> { diagonal.add(null) }
    shouldThrow<IllegalStateException> { diagonal.removeAt(0) }
  }

  private data class Cell(val row: Int, val column: Int)
}
