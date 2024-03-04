package net.k1ra.succubotserver.feature.api.model.login

import io.ktor.http.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Session(
    val token: String,
    val uid: String
) {

    init {
        initialSetup()
    }

   companion object {
       private object SessionTable: IntIdTable() {
           val uid = text("uid")
           val token = text("token")
       }

       fun isSessionValid(session: Session) : Boolean {
           var isValid = false

           transaction {
               SessionTable.select(SessionTable.token eq session.token).forEach{
                   isValid = session.uid == it[SessionTable.uid]
               }
           }

           return isValid
       }

       fun headerToSession(headers: Headers): Session? {
           var session: Session? = null
           val authInfo = headers["Authorization"]

           if (authInfo != null && authInfo.startsWith("Bearer ")) {
               val headerSplit = authInfo.replace("Bearer ","").split(":")
               session = Session(headerSplit[1], headerSplit[0])
           }

           return session
       }

       fun logoutUser(session: Session) {
           transaction {
               SessionTable.deleteWhere { (SessionTable.token eq session.token) and (SessionTable.uid eq session.uid) }
           }
       }

       fun destroyAllUserSessions(uid: String) {
           transaction {
               SessionTable.deleteWhere {(SessionTable.uid eq uid) }
           }
       }

       fun createSession(uidStr: String) : Session {
           //If user was successfully logged in, generate token
           //Enforce that the token is unique
           var token = UUID.randomUUID().toString()
           while (!isTokenUnique(token))
               token = UUID.randomUUID().toString()

           //Finally, insert session into the sessions table
           transaction {
               SessionTable.insert {
                   it[uid] = uidStr
                   it[SessionTable.token] = token
               }
           }

           return Session(token, uidStr)
       }

       private fun isTokenUnique(token: String) : Boolean {
           var result = true

           transaction {
               SessionTable.select(SessionTable.token eq token).forEach {
                   result = false
               }
           }

           return result
       }

       @Synchronized
       fun initialSetup() {
           transaction {
               if (!SessionTable.exists()) {
                   SchemaUtils.create(SessionTable)
               }
           }
       }
   }
}