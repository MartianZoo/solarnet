package dev.martianzoo.pets.util

/**
 * Fixed rectangular positions addressed by zero-based row and column. A null position and a
 * position outside the rectangle both read as null; neither contributes to this grid's [Set]
 * elements. [grid] places elements using caller-supplied coordinates and fixes the dimensions from
 * the largest coordinates supplied. Rows, columns, and diagonals retain null gaps.
 *
 * For a hex map, rows remain horizontal while columns slant up and right. [diagonals] cover the
 * other slant. Neighbor methods preserve their documented order and omit positions that read as
 * null.
 */
public interface Grid<E> : Set<E> {
  /** Number of indexed rows, including rows containing only null cells. */
  public val rowCount: Int
  /** Number of indexed columns, including columns containing only null cells. */
  public val columnCount: Int

  /** Rows in increasing index order; each has [columnCount] nullable cells. */
  public fun rows(): List<List<E?>>

  /** Columns in increasing index order; each has [rowCount] nullable cells. */
  public fun columns(): List<List<E?>>

  /** Diagonals in increasing column-minus-row order, padded with null to [rowCount] cells. */
  public fun diagonals(): List<List<E?>>

  /** The cell at zero-based coordinates, or null for an empty or out-of-grid cell. */
  public operator fun get(rowIndex: Int, columnIndex: Int): E?

  /** The complete row at [rowIndex], including null cells; an invalid index throws. */
  public fun row(rowIndex: Int): List<E?> = rows()[rowIndex]

  /** The complete column at [columnIndex], including null cells. */
  public fun column(columnIndex: Int): List<E?> = columns()[columnIndex]

  /**
   * The diagonal whose column index minus row index is [columnMinusRow]. Zero is the main diagonal;
   * each result has [rowCount] cells, with null where the diagonal lies outside the grid.
   */
  public fun diagonal(columnMinusRow: Int): List<E?>

  /** Existing cells directly above, left, right, and below ([r], [c]), in that order. */
  public fun cardinalNeighbors(r: Int, c: Int): List<E> =
      listOfNotNull(
          this[r - 1, c + 0],
          this[r + 0, c - 1],
          this[r + 0, c + 1],
          this[r + 1, c + 0],
      )

  /** Existing hex neighbors clockwise from the upper-right neighbor; absent cells are omitted. */
  public fun hexNeighbors(r: Int, c: Int): List<E> =
      listOfNotNull(
          this[r - 1, c],
          this[r, c + 1],
          this[r + 1, c + 1],
          this[r + 1, c],
          this[r, c - 1],
          this[r - 1, c - 1],
      )

  /**
   * Size of the largest hex-connected subset of [cells], or zero when [cells] is empty. [cells]
   * must be members of this grid; [rowOf] and [columnOf] locate each member for the adjacency test.
   */
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

  /** All existing surrounding cells: [hexNeighbors] first, then the two remaining diagonals. */
  public fun allNeighbors(r: Int, c: Int): List<E> =
      hexNeighbors(r, c) + listOfNotNull(this[r - 1, c + 1], this[r + 1, c - 1])

  public companion object {
    private fun <E> empty(): Grid<E> {
      return mutableGrid(listOf<E>(), { 0 }, { 0 }).immutable()
    }

    /**
     * Builds a fixed grid containing [cells] at their zero-based coordinates. Its dimensions reach
     * the largest supplied row and column; gaps are null, and a later cell at the same coordinates
     * replaces an earlier cell. The result copies its rows, so later changes to [cells] do not
     * change the grid. Negative coordinates fail while placing the cells.
     */
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
