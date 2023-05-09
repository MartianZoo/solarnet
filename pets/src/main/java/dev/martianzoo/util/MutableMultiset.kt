package dev.martianzoo.util

interface MutableMultiset<E> : MutableCollection<E>, Multiset<E> {
  fun setCount(element: E, newCount: Int): Int /*old count*/
  fun add(element: E, occurrences: Int): Int /*new count*/
  fun mustRemove(element: E, occurrences: Int): Int /* new count */
  fun tryRemove(element: E, occurrences: Int): Int /* how many removed */
}
