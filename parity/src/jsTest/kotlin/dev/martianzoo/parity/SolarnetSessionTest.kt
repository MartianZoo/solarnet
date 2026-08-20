package dev.martianzoo.parity

import kotlin.js.JsNonModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class SolarnetSessionTest {
  @Test
  fun createsCanonicalGameInNode() {
    val session = SolarnetSession("CorporateEraExpansion", 2, NodeFiles::readUtf8)
    try {
      val snapshot = Json.parseToJsonElement(session.snapshot()).jsonObject
      assertEquals(1, snapshot.getValue("generation").jsonPrimitive.content.toInt())
      assertEquals("CorporationPhase", snapshot.getValue("phase").jsonPrimitive.content)
      assertEquals(
          listOf("Player1", "Player2"),
          snapshot.getValue("players").jsonArray.map { it.jsonPrimitive.content },
      )

      session.apply(
          """{"operation":"selectCorporation","player":1,"corporation":"InterplanetaryCinematics","projectCards":0}"""
      )
      val afterBoth =
          Json.parseToJsonElement(
                  session.apply(
                      """{"operation":"selectCorporation","player":2,"corporation":"CrediCor","projectCards":0}"""
                  )
              )
              .jsonObject
      assertEquals("ActionPhase", afterBoth.getValue("phase").jsonPrimitive.content)
    } finally {
      session.close()
    }
  }
}

@JsModule("fs")
@JsNonModule
private external object NodeFileSystem {
  @Suppress("UnusedParameter") fun readFileSync(path: String, encoding: String): String
}

private object NodeFiles {
  fun readUtf8(path: String): String = NodeFileSystem.readFileSync("kotlin/$path", "utf8")
}
