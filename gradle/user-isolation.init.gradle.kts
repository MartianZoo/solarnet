import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.security.MessageDigest
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class StorageLeaseService :
    BuildService<StorageLeaseService.Parameters>, AutoCloseable {
  interface Parameters : BuildServiceParameters {
    val worktreeRoot: DirectoryProperty
  }

  private data class Lease(
      val root: File,
      val channel: FileChannel,
      val lock: FileLock,
  )

  private val lease: Lease by lazy { acquire(parameters.worktreeRoot.get().asFile) }

  val storageRoot: File
    get() = lease.root

  override fun close() {
    lease.lock.release()
    lease.channel.close()
  }

  private fun acquire(worktreeRoot: File): Lease {
    var slot = 0
    while (true) {
      val slotRoot = worktreeRoot.resolve("slots/$slot")
      slotRoot.mkdirs()
      check(slotRoot.isDirectory) { "cannot create Gradle storage $slotRoot" }
      val channel = FileChannel.open(slotRoot.resolve("invocation.lock").toPath(), CREATE, WRITE)
      val lock =
          try {
            channel.tryLock()
          } catch (_: OverlappingFileLockException) {
            null
          }
      if (lock != null) return Lease(slotRoot, channel, lock)
      channel.close()
      slot++
    }
  }
}

// Local worktrees may be readable or writable by more than one account. Keep every account's
// generated state in its own home directory. Concurrent invocations lease separate stable slots so
// they cannot delete or overwrite each other's task outputs; ordinary sequential builds reuse slot
// zero and its caches.
beforeSettings {
  if (System.getenv("CI") != null) return@beforeSettings

  val worktreePath = settingsDir.canonicalFile.toPath().normalize().toString()
  val worktreeId =
      MessageDigest.getInstance("SHA-256").digest(worktreePath.toByteArray()).take(12).joinToString(
          ""
      ) {
        "%02x".format(it)
      }
  val worktreeRoot =
      file(System.getProperty("user.home")).resolve(".gradle/solarnet-builds/$worktreeId")
  val storageRoot =
      gradle.sharedServices
          .registerIfAbsent("solarnet-storage-$worktreeId", StorageLeaseService::class) {
            parameters.worktreeRoot.set(worktreeRoot)
          }
          .get()
          .storageRoot

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
