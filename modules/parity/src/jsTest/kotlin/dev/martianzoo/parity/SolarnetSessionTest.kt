package dev.martianzoo.parity

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.TaskException
import kotlin.js.JsNonModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
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

      val initialEvents = Json.parseToJsonElement(session.eventsSince(0)).jsonObject
      val initialCursor = initialEvents.getValue("nextCursor").jsonPrimitive.content.toInt()
      assertTrue(initialEvents.getValue("lines").jsonArray.isNotEmpty())
      assertTrue(
          Json.parseToJsonElement(session.eventsSince(initialCursor))
              .jsonObject
              .getValue("lines")
              .jsonArray
              .isEmpty()
      )

      session.apply(
          """{"operation":"selectCorporation","player":1,"corporation":"InterplanetaryCinematics","projectCards":1}"""
      )
      val firstMoveEvents = Json.parseToJsonElement(session.eventsSince(initialCursor)).jsonObject
      val firstMoveCursor = firstMoveEvents.getValue("nextCursor").jsonPrimitive.content.toInt()
      assertTrue(firstMoveCursor > initialCursor)
      assertTrue(firstMoveEvents.getValue("lines").jsonArray.isNotEmpty())

      val afterBoth =
          Json.parseToJsonElement(
                  session.apply(
                      """{"operation":"selectCorporation","player":2,"corporation":"CrediCor","projectCards":0}"""
                  )
              )
              .jsonObject
      assertEquals("ActionPhase", afterBoth.getValue("phase").jsonPrimitive.content)

      val secondMoveEvents =
          Json.parseToJsonElement(session.eventsSince(firstMoveCursor)).jsonObject
      val secondMoveCursor = secondMoveEvents.getValue("nextCursor").jsonPrimitive.content.toInt()
      assertTrue(secondMoveCursor > firstMoveCursor)
      assertTrue(secondMoveEvents.getValue("lines").jsonArray.isNotEmpty())

      val afterProject =
          Json.parseToJsonElement(
                  session.apply(
                      """{"operation":"playProject","player":1,"cardId":"105","payment":{"megacredits":1,"steel":0,"titanium":0}}"""
                  )
              )
              .jsonObject
      assertEquals("ActionPhase", afterProject.getValue("phase").jsonPrimitive.content)

      val projectEvents = Json.parseToJsonElement(session.eventsSince(secondMoveCursor)).jsonObject
      assertTrue(
          projectEvents.getValue("nextCursor").jsonPrimitive.content.toInt() > secondMoveCursor
      )
      val projectLines = projectEvents.getValue("lines").jsonArray.map { it.jsonPrimitive.content }
      assertTrue(projectLines.any { "EarthOffice" in it })
      assertTrue(
          projectLines.any {
            "Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1>" in it
          }
      )

      assertFailsWith<NarrowingException> {
        session.apply("""{"operation":"pass","player":1}""")
      }
      session.apply("""{"operation":"endTurn","player":1}""")
      val endTurnEvents =
          Json.parseToJsonElement(
                  session.eventsSince(
                      projectEvents.getValue("nextCursor").jsonPrimitive.content.toInt()
                  )
              )
              .jsonObject
      val endTurnCursor = endTurnEvents.getValue("nextCursor").jsonPrimitive.content.toInt()
      val endTurnLines = endTurnEvents.getValue("lines").jsonArray.map { it.jsonPrimitive.content }
      assertTrue(endTurnLines.any { "NewTurn<Player2>" in it })

      assertFailsWith<TaskException> {
        session.apply("""{"operation":"endTurn","player":2}""")
      }
      session.apply("""{"operation":"pass","player":2}""")
      val passEvents = Json.parseToJsonElement(session.eventsSince(endTurnCursor)).jsonObject
      val passLines = passEvents.getValue("lines").jsonArray.map { it.jsonPrimitive.content }
      assertTrue(passLines.any { "+Pass<Player2>" in it })
      assertTrue(passLines.any { "NewTurn<Player1>" in it })
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
