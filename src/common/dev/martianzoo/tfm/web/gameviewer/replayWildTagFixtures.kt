package dev.martianzoo.tfm.web.gameviewer

// sneak avoids ordinary tag effects; Temporary removes the holder after the following operation.
internal fun fakeWildTags(tag: String, count: Int = 1): String {
  require(count > 0)
  val tags = if (count == 1) "$tag<FakeWildTagUse>" else "$count $tag<FakeWildTagUse>"
  return "FakeWildTagUse, $tags"
}
