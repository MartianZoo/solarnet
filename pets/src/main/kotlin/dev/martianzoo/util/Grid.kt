package dev.martianzoo.util

/**
 * A fixed-size two-dimensional array of nullable elements, where the elements
 * "know" their own row and column number (as opposed to their being placed there).
 * Important: there is no distinction made between a null cell, a missing cell,
 * and a cell that is "off the edge" of the grid!
 *
 * This actually works equally well for a hex grid. Imagine a parallelogram-
 * shaped section of the hex grid, slanted like `/ /`. That is, rows are still
 * horizontal, but what this class considers to be "columns" will actually slant
 * up and to the right. In this case, the *diagonals* of this grid represent the
 * columns that slant the other way.
 */
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
      return mutableGrid(cells, rowFn, columnFn).immutable()
    }

    fun <E> mutableGrid(
        cells: Iterable<E>,
        rowFn: (E) -> Int,
        columnFn: (E) -> Int,
    ): MutableGrid<E> {
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
