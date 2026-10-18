package dev.martianzoo.tfm.web.almanac

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ClassSearchBrowserTest {
  @Test
  internal fun mixedCapitalsMatchCamelCaseClassNames() {
    assertTrue(matchesClassQuery("MagneticShieldGenerator", "MaSG"))
    assertTrue(matchesClassQuery("MagneticFieldGenerators", "MaFG"))
    assertTrue(matchesClassQuery("MagneticShieldGenerator", "magnetic"))
    assertFalse(matchesClassQuery("MagneticShieldGenerator", "MaGS"))
  }
}
