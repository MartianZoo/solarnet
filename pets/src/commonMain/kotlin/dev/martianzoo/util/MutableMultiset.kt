package dev.martianzoo.util

internal interface MutableMultiset<E> : MutableCollection<E>, Multiset<E> {
  public fun setCount(element: E, newCount: Int): Int /*old count*/

  public fun add(element: E, occurrences: Int): Int /*new count*/

  public fun mustRemove(element: E, occurrences: Int): Int /* new count */

  public fun tryRemove(element: E, occurrences: Int): Int /* how many removed */
}
