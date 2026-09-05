package dev.martianzoo.tools

import java.io.File
import java.net.URLClassLoader
import java.security.MessageDigest

/** Runs compiler and Detekt internals in a child classloader, isolated from application classes. */
internal class KotlinFileComplexityAnalyzer(classpath: Collection<File>) : AutoCloseable {
  private val classpathFiles = classpath.sortedBy(File::getAbsolutePath)
  private val classLoader =
      URLClassLoader(
          classpathFiles.map(File::toURI).map { it.toURL() }.toTypedArray(),
          ClassLoader.getPlatformClassLoader(),
      )
  private val disposableClass = loadClass("com.intellij.openapi.Disposable")
  private val disposerClass = loadClass("com.intellij.openapi.util.Disposer")
  private val disposable =
      disposerClass
          .getMethod("newDisposable", String::class.java)
          .invoke(null, "Kotlin file complexity analyzer")
  private val environmentConfigFilesClass =
      loadClass("org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles")
  private val compilerConfigurationClass =
      loadClass("org.jetbrains.kotlin.config.CompilerConfiguration")
  private val environment = createEnvironment()
  private val psiFactory = createPsiFactory()
  private val namedFunctionClass = loadClass("org.jetbrains.kotlin.psi.KtNamedFunction")
  private val psiElementClass = loadClass("com.intellij.psi.PsiElement")
  private val psiElementVisitorClass = loadClass("com.intellij.psi.PsiElementVisitor")
  private val psiTreeUtilClass = loadClass("com.intellij.psi.util.PsiTreeUtil")
  private val complexityConfigClass = loadClass("dev.detekt.metrics.CyclomaticComplexity\$Config")
  private val complexityClass = loadClass("dev.detekt.metrics.CyclomaticComplexity")

  /** Changes whenever cached complexity values must be discarded. */
  val implementationFingerprint: String by lazy {
    val digest = MessageDigest.getInstance("SHA-256")
    javaClass.getResourceAsStream("KotlinFileComplexityAnalyzer.class").use { stream ->
      digest.update(requireNotNull(stream) { "Analyzer class bytes are unavailable" }.readBytes())
    }
    classpathFiles.forEach { file ->
      digest.update(file.name.toByteArray())
      digest.update(file.length().toString().toByteArray())
    }
    digest.digest().hex()
  }

  /** Returns null for a test-case file, which is outside the report's scope. */
  fun analyze(relativePath: String, source: String): Int? {
    val kotlinFile =
        psiFactory.javaClass
            .getMethod("createFile", String::class.java, String::class.java)
            .invoke(psiFactory, relativePath.substringAfterLast('/'), source)
    if (relativePath.startsWith("test/") && containsTestCase(kotlinFile)) return null

    val config = complexityConfigClass.getConstructor().newInstance()
    complexityConfigClass
        .getMethod("setIgnoreSimpleWhenEntries", Boolean::class.javaPrimitiveType)
        .invoke(config, false)
    val visitor = complexityClass.getConstructor(complexityConfigClass).newInstance(config)
    psiElementClass.getMethod("accept", psiElementVisitorClass).invoke(kotlinFile, visitor)
    return complexityClass.getMethod("getComplexity").invoke(visitor) as Int
  }

  override fun close() {
    disposerClass.getMethod("dispose", disposableClass).invoke(null, disposable)
    classLoader.close()
  }

  private fun createEnvironment(): Any {
    val configuration = compilerConfigurationClass.getConstructor().newInstance()
    val configFiles = environmentConfigFilesClass.getField("JVM_CONFIG_FILES").get(null)
    val environmentClass = loadClass("org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment")
    return environmentClass
        .getMethod(
            "createForProduction",
            disposableClass,
            compilerConfigurationClass,
            environmentConfigFilesClass,
        )
        .invoke(null, disposable, configuration, configFiles)
  }

  private fun createPsiFactory(): Any {
    val projectClass = loadClass("com.intellij.openapi.project.Project")
    val project = environment.javaClass.getMethod("getProject").invoke(environment)
    return loadClass("org.jetbrains.kotlin.psi.KtPsiFactory")
        .getConstructor(projectClass, Boolean::class.javaPrimitiveType)
        .newInstance(project, false)
  }

  private fun containsTestCase(kotlinFile: Any): Boolean {
    @Suppress("UNCHECKED_CAST")
    val functions =
        psiTreeUtilClass
            .getMethod("findChildrenOfType", psiElementClass, Class::class.java)
            .invoke(null, kotlinFile, namedFunctionClass) as Collection<Any>
    return functions.any { function ->
      @Suppress("UNCHECKED_CAST")
      val annotations =
          function.javaClass.getMethod("getAnnotationEntries").invoke(function) as List<Any>
      annotations.any { annotation ->
        val shortName = annotation.javaClass.getMethod("getShortName").invoke(annotation)
        shortName != null &&
            shortName.javaClass.getMethod("asString").invoke(shortName) in TEST_CASE_ANNOTATIONS
      }
    }
  }

  private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

  private fun loadClass(name: String): Class<*> = Class.forName(name, true, classLoader)

  private companion object {
    val TEST_CASE_ANNOTATIONS =
        setOf("Test", "TestFactory", "TestTemplate", "ParameterizedTest", "RepeatedTest")
  }
}
