package dev.martianzoo.util

// this is honestly bogus - it can't grow
public class MutableGrid<E>(private val rows: List<List<E?>>) : Grid<E>, AbstractSet<E>() {

  override val rowCount = rows.size
  override val columnCount = rows[0].size

  override operator fun get(rowIndex: Int, columnIndex: Int): E? {
    return row(rowIndex)[columnIndex]
  }

  fun set(rowIndex: Int, columnIndex: Int, value: E): E? {
    return (row(rowIndex) as MutableList<E>).set(columnIndex, value)
  }

  override fun rows() = rows

  override fun columns() = List(columnCount) { column(it) }

  override fun diagonals() = List(rowCount + columnCount - 1) { diagonal(it - rowCount + 1) }

  override fun row(rowIndex: Int) = rows[rowIndex]

  override fun column(columnIndex: Int): MutableList<E?> {
    return MutableColumn(rows, columnIndex)
  }

  private fun all() = rows.flatten().filterNotNull().toSet()
  override val size get() = all().size
  override fun iterator() = all().iterator()
  override fun contains(element: E) = all().contains(element)
  override fun isEmpty() = false

  fun immutable(): Grid<E> {
    return MutableGrid(rows.map { it.toList() }.toList())
  }
  private class MutableColumn<E>(val rows: List<List<E?>>, val columnIndex: Int) :
      AbstractMutableList<E?>() {

    override val size = rows.size

    override fun get(index: Int) = rows[index][columnIndex]

    override fun set(index: Int, element: E?) = (rows[index] as MutableList<E?>).set(columnIndex, element)

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E?> {
      return MutableColumn(rows.subList(fromIndex, toIndex), columnIndex)
    }

    override fun add(index: Int, element: E?) = error("fixed-size")
    override fun removeAt(index: Int) = error("fixed-size")
  }

  // zero for the main diagonal, increasing to the right
  override fun diagonal(columnMinusRow: Int): MutableList<E?> {
    return MutableDiagonal(this, columnMinusRow)
  }

  private class MutableDiagonal<E>(
      val grid: MutableGrid<E>,
      val columnMinusRow: Int,
  ) : AbstractMutableList<E?>() {

    init {
      if (columnMinusRow <= 0 - grid.rowCount ||
          columnMinusRow >= grid.columnCount - 0) {
        throw IndexOutOfBoundsException(columnMinusRow)
      }
    }

    override val size = grid.rowCount

    override fun get(index: Int) = grid.row(index).getOrNull(columnMinusRow + index)

    override fun set(index: Int, element: E?) =
        (grid.row(index) as MutableList<E?>).set(columnMinusRow + index, element)

    override fun add(index: Int, element: E?) = error("fixed-size")
    override fun removeAt(index: Int) = error("fixed-size")
  }
}
