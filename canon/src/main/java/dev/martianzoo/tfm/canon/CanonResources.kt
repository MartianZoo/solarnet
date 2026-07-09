package dev.martianzoo.tfm.canon

internal object CanonResources {
  fun read(filename: String): String {
    val dir = Canon::class.java.packageName.replace('.', '/')
    return Canon::class.java.getResource("/$dir/$filename")!!.readText()
  }
}
