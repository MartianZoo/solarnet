package dev.martianzoo.tfm.tests

// Temporary removes the holder when pending gameplay finishes. Supply these tags through
// runOperation in card tests so their gains exercise the normal trigger machinery.
internal fun fakeWildTags(tag: String, count: Int = 1): String {
  require(count > 0)
  val tags = if (count == 1) "$tag<FakeWildTagUse>" else "$count $tag<FakeWildTagUse>"
  return "FakeWildTagUse, $tags"
}

internal fun fakeWildTags(
    firstTag: String,
    secondTag: String,
    vararg additionalTags: String,
): String {
  val tags = listOf(firstTag, secondTag, *additionalTags).joinToString { "$it<FakeWildTagUse>" }
  return "FakeWildTagUse, $tags"
}
