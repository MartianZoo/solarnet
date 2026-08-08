package dev.martianzoo.types

/** An immutable, nonnegative arbitrary-width integer specialized for bit masks. */
internal class BigInt private constructor(private val words: LongArray) {
  infix fun or(that: BigInt): BigInt {
    if (words.isEmpty()) return that
    if (that.words.isEmpty()) return this

    val larger = if (words.size >= that.words.size) words else that.words
    val smaller = if (words.size < that.words.size) words else that.words
    val result = larger.copyOf()
    for (index in smaller.indices) {
      result[index] = result[index] or smaller[index]
    }
    return BigInt(result)
  }

  fun hasBit(index: Int): Boolean {
    require(index >= 0)
    val wordIndex = index ushr 6
    return wordIndex < words.size && words[wordIndex] and (1L shl (index and 63)) != 0L
  }

  companion object {
    val ZERO: BigInt = BigInt(LongArray(0))

    fun bit(index: Int): BigInt {
      require(index >= 0)
      val words = LongArray((index ushr 6) + 1)
      words[index ushr 6] = 1L shl (index and 63)
      return BigInt(words)
    }
  }
}
