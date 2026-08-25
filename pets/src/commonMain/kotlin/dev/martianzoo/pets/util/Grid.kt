package dev.martianzoo.pets.util

/**
 * A fixed-size two-dimensional array of nullable elements, where the elements "know" their own row
 * and column number (as opposed to their being placed there). Important: there is no distinction
 * made between a null cell, a missing cell, and a cell that is "off the edge" of the grid!
 *
 * This actually works equally well for a hex grid. Imagine a parallelogram- shaped section of the
 * hex grid, slanted like `/ /`. That is, rows are still horizontal, but what this class considers
 * to be "columns" will actually slant up and to the right. In this case, the *diagonals* of this
 * grid represent the columns that slant the other way.
 */
public interface Grid<E> : Set<E> {
  public val rowCount: Int
  public val columnCount: Int

  public fun rows(): List<List<E?>>

  public fun columns(): List<List<E?>>

  public fun diagonals(): List<List<E?>>

  public operator fun get(rowIndex: Int, columnIndex: Int): E?

  public fun row(rowIndex: Int): List<E?> = rows()[rowIndex]

  public fun column(columnIndex: Int): List<E?> = columns()[columnIndex]

  // zero for the main diagonal, increasing to the right
  // all diagonals have size = grid height, using additional nulls as necessary
  public fun diagonal(columnMinusRow: Int): List<E?>

  public fun cardinalNeighbors(r: Int, c: Int): List<E> =
      listOfNotNull(
          this[r - 1, c + 0],
          this[r + 0, c - 1],
          this[r + 0, c + 1],
          this[r + 1, c + 0],
      )

  /** Returns existing hex neighbors clockwise from the upper-right neighbor. */
  public fun hexNeighbors(r: Int, c: Int): List<E> =
      listOfNotNull(
          this[r - 1, c],
          this[r, c + 1],
          this[r + 1, c + 1],
          this[r + 1, c],
          this[r, c - 1],
          this[r - 1, c - 1],
      )

  /** Returns the size of the largest contiguous group in [cells], using hex adjacency. */
  public fun largestContiguousGroupSize(
      cells: Set<E>,
      rowOf: (E) -> Int,
      columnOf: (E) -> Int,
  ): Int {
    val remaining = cells.toMutableSet()
    var largest = 0

    while (remaining.isNotEmpty()) {
      val first = remaining.first()
      remaining.remove(first)
      val pending = mutableListOf(first)
      var size = 0

      while (pending.isNotEmpty()) {
        val cell = pending.removeLast()
        size++

        for (neighbor in hexNeighbors(rowOf(cell), columnOf(cell))) {
          if (remaining.remove(neighbor)) pending.add(neighbor)
        }
      }

      largest = maxOf(largest, size)
    }

    return largest
  }

  public fun allNeighbors(r: Int, c: Int): List<E> =
      hexNeighbors(r, c) + listOfNotNull(this[r - 1, c + 1], this[r + 1, c - 1])

  public companion object {
    internal fun <E> empty(): Grid<E> {
      return mutableGrid(listOf<E>(), { 0 }, { 0 }).immutable()
    }

    public fun <E> grid(cells: Iterable<E>, rowFn: (E) -> Int, columnFn: (E) -> Int): Grid<E> {
      return mutableGrid(cells, rowFn, columnFn).immutable()
    }

    private fun <E> mutableGrid(
        cells: Iterable<E>,
        rowFn: (E) -> Int,
        columnFn: (E) -> Int,
    ): MutableGrid<E> {
      val maxRowIndex = cells.maxOfOrNull(rowFn) ?: 0
      val maxColIndex = cells.maxOfOrNull(columnFn) ?: 0

      val grid = MutableGrid<E>(List(maxRowIndex + 1) { MutableList(maxColIndex + 1) { null } })
      cells.forEach { grid.set(rowFn(it), columnFn(it), it) }
      return grid
    }
  }
}
