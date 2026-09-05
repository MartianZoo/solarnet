package dev.martianzoo.tools

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class KotlinFileComplexityAnalyzerTest {
  private val analyzerClasspath: List<File>
    get() = System.getProperty("java.class.path").split(File.pathSeparator).map(::File)

  @Test
  fun `counts functions and control flow across a file`() {
    val source =
        """
        fun constant() = 1

        fun classify(value: Int): String {
          if (value < 0 || value == 4) return "special"
          for (candidate in 0..value) {
            when (candidate) {
              0 -> continue
              1, 2 -> return "small"
              else -> Unit
            }
          }
          return "other"
        }
        """
            .trimIndent()

    KotlinFileComplexityAnalyzer(analyzerClasspath).use { analyzer ->
      assertEquals(9, analyzer.analyze("src/sample.kt", source))
    }
  }

  @Test
  fun `includes test helpers but excludes files containing test cases`() {
    KotlinFileComplexityAnalyzer(analyzerClasspath).use { analyzer ->
      assertEquals(
          1,
          analyzer.analyze(
              "test/Helper.kt",
              """
              import kotlin.test.BeforeTest
              class Helper { @BeforeTest fun prepare() = Unit }
              """
                  .trimIndent(),
          ),
      )
      assertNull(
          analyzer.analyze(
              "test/ExampleTest.kt",
              """
              import kotlin.test.Test
              class ExampleTest { @Test fun example() = Unit }
              """
                  .trimIndent(),
          )
      )
    }
  }

  @Test
  fun `does not classify production annotations as test scope`() {
    KotlinFileComplexityAnalyzer(analyzerClasspath).use { analyzer ->
      assertEquals(
          1,
          analyzer.analyze(
              "src/Example.kt",
              """
              annotation class Test
              class Example { @Test fun operation() = Unit }
              """
                  .trimIndent(),
          ),
      )
    }
  }
}
