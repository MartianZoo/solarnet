package dev.martianzoo.pets.util

// TODO: Replace this fixed-size mutable set/grid hybrid with a collection whose contracts match.
internal class MutableGrid<E>(private val rows: List<List<E?>>) : Grid<E>, AbstractSet<E>() {

  override val rowCount: Int by rows::size
  override val columnCount: Int by rows[0]::size

  @Suppress("TooGenericExceptionCaught") // TODO fix
  override operator fun get(rowIndex: Int, columnIndex: Int): E? {
    return try {
      row(rowIndex)[columnIndex]
    } catch (_: Exception) {
      null
    }
  }

  internal fun set(rowIndex: Int, columnIndex: Int, value: E): E? {
    @Suppress("UNCHECKED_CAST") val row = row(rowIndex) as MutableList<E>
    return row.set(columnIndex, value)
  }

  override fun rows(): List<List<E?>> = rows

  override fun columns(): List<List<E?>> = List(columnCount) { column(it) }

  override fun diagonals(): List<List<E?>> =
      List(rowCount + columnCount - 1) { diagonal(it - rowCount + 1) }

  override fun row(rowIndex: Int): List<E?> = rows[rowIndex]

  override fun column(columnIndex: Int): MutableList<E?> {
    return MutableColumn(rows, columnIndex)
  }

  private fun all() = rows.flatten().filterNotNull().toSet()

  override val size: Int
    get() = all().size

  override fun iterator(): Iterator<E> = all().iterator()

  override fun contains(element: E): Boolean = all().contains(element)

  override fun isEmpty(): Boolean = all().isEmpty()

  internal fun immutable(): Grid<E> {
    return MutableGrid(rows.map { it.toList() })
  }

  private class MutableColumn<E>(val rows: List<List<E?>>, val columnIndex: Int) :
      AbstractMutableList<E?>() {

    override val size by rows::size

    override fun get(index: Int) = rows[index][columnIndex]

    override fun set(index: Int, element: E?) =
        (rows[index] as MutableList<E?>).set(columnIndex, element)

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
      if (columnMinusRow <= 0 - grid.rowCount || columnMinusRow >= grid.columnCount - 0) {
        throw IndexOutOfBoundsException("$columnMinusRow")
      }
    }

    override val size by grid::rowCount

    override fun get(index: Int) = grid.row(index).getOrNull(columnMinusRow + index)

    override fun set(index: Int, element: E?) =
        (grid.row(index) as MutableList<E?>).set(columnMinusRow + index, element)

    override fun add(index: Int, element: E?) = error("fixed-size")

    override fun removeAt(index: Int) = error("fixed-size")
  }
}
