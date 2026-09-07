package dev.martianzoo.tfm.tests.replays

// exMachina applies this through sneak, so the tag gain does not fire ordinary tag effects.
internal fun fakeWildTags(tag: String, count: Int = 1): String {
  require(count > 0)
  val tags = if (count == 1) "$tag<FakeWildTagUse>" else "$count $tag<FakeWildTagUse>"
  return "FakeWildTagUse, $tags"
}
