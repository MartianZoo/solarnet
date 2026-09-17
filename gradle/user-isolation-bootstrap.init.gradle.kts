// Tooling API clients such as IntelliJ do not execute the gradlew shell script. When installed in
// ~/.gradle/init.d, this bootstrap finds and applies each Solarnet worktree's isolation script. The
// wrapper already supplies that script explicitly, so do not apply it twice there.
val explicitInitScripts = gradle.startParameter.initScripts.map { it.canonicalFile }.toSet()
val solarnetIsolationScript =
    generateSequence(gradle.startParameter.currentDir.canonicalFile) { it.parentFile }
        .map { it.resolve("gradle/user-isolation.init.gradle.kts") }
        .firstOrNull { it.isFile }

if (
    solarnetIsolationScript != null &&
        solarnetIsolationScript.canonicalFile !in explicitInitScripts
) {
  apply(from = solarnetIsolationScript)
}
