package dev.martianzoo.util

// A crappy abstraction for a fixed-size 2-dim array of nullable elements, where the elements
// know their own row and column number.
// Elements are adjacent if they are adjacent in one of the rows() or columns()

// This works equally well for a "parallelogram" shaped hex grid slanted this way / /
// In that case, adjacent elements in the "diagonals" are also adjacent in the grid.

interface Grid<E> : Set<E> {
  val rowCount: Int
  val columnCount: Int

  fun rows(): List<List<E?>>
  fun columns(): List<List<E?>>
  fun diagonals(): List<List<E?>>

  operator fun get(rowIndex: Int, columnIndex: Int): E?
  fun row(rowIndex: Int): List<E?> = rows()[rowIndex]
  fun column(columnIndex: Int): List<E?> = columns()[columnIndex]

  // zero for the main diagonal, increasing to the right
  // all diagonals have size = grid height, using additional nulls as necessary
  fun diagonal(columnMinusRow: Int): List<E?>

  companion object {
    fun <E> grid(cells: Iterable<E>, rowFn: (E) -> Int, columnFn: (E) -> Int): Grid<E> {
      return mutableGrid(cells, rowFn, columnFn)
    }

    fun <E> mutableGrid(cells: Iterable<E>, rowFn: (E) -> Int, columnFn: (E) -> Int): MutableGrid<E> {
      val maxRowIndex = cells.maxOfOrNull(rowFn) ?: 0
      val maxColIndex = cells.maxOfOrNull(columnFn) ?: 0

      val grid = MutableGrid<E>(
          List(maxRowIndex + 1) {
            MutableList(maxColIndex + 1) { null }
          }
      )
      cells.forEach { grid.set(rowFn(it), columnFn(it), it) }
      return grid
    }
  }
}
