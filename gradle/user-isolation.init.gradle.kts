import java.security.MessageDigest

// Local worktrees may be readable or writable by more than one account. Keep every account's
// generated state in its own home directory so Gradle never relies on shared-file ownership.
beforeSettings {
  if (System.getenv("CI") != null) return@beforeSettings

  val worktreePath = settingsDir.canonicalFile.toPath().normalize().toString()
  val worktreeId =
      MessageDigest.getInstance("SHA-256").digest(worktreePath.toByteArray()).take(12).joinToString(
          ""
      ) {
        "%02x".format(it)
      }
  val storageRoot =
      file(System.getProperty("user.home")).resolve(".gradle/solarnet-builds/$worktreeId")

  gradle.startParameter.projectCacheDir = storageRoot.resolve("project-cache")
  gradle.startParameter.projectProperties =
      gradle.startParameter.projectProperties +
          ("kotlin.project.persistent.dir" to storageRoot.resolve("kotlin").path)

  val temporaryDirectory = storageRoot.resolve("tmp")
  temporaryDirectory.mkdirs()
  System.setProperty("java.io.tmpdir", temporaryDirectory.path)

  gradle.lifecycle.beforeProject {
    val projectDirectoryName = if (path == ":") "root" else path.removePrefix(":").replace(':', '/')
    layout.buildDirectory.set(storageRoot.resolve("build/$projectDirectoryName"))
  }
}
