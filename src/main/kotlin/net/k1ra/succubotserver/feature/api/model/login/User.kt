package net.k1ra.succubotserver.feature.api.model.login

import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.collections.ArrayList

class User(
    val name: String, //User's display name
    val picture: String, //Url of user's profile picture
    val email: String, //User's email
    val uid: String, //User ID
    val token: String, //Session token
    val admin: Boolean, //Is user an admin or not
    val native: Boolean, //Does account come from built-in account system or an external source like LDAP
) {
    init {
        initialSetup()
    }

    class UserDataShort(
        val name: String, //User's display name
        val picture: String, //Url of user's profile picture
        val uid: String, //User ID
    )

   companion object {
       private object UsersTable: IntIdTable() {
           val name = text("name")
           val picture = text("picture")
           val email = text("email")
           val password = text("password")
           val uid = text("uid")
           val admin = bool("admin")
           val native = bool("native")
       }

       fun login(request: LoginRequest) : User? {
           var user: User? = null

           //Should only either return 1 or 0 records
           transaction {
               UsersTable.select((UsersTable.email eq request.email)).forEach {
                    if (Password.verify(request.password, it[UsersTable.password]))
                        user = User(
                            it[UsersTable.name],
                            it[UsersTable.picture],
                            it[UsersTable.email],
                            it[UsersTable.uid],
                            Session.createSession(it[UsersTable.uid]).token,
                            it[UsersTable.admin],
                            it[UsersTable.native]
                        )
               }
           }

           return user
       }

       fun createSession(uid: String) : User {
           var user: User? = null

           transaction {
               UsersTable.select((UsersTable.uid eq uid)).forEach {
                   user = User(
                       it[UsersTable.name],
                       it[UsersTable.picture],
                       it[UsersTable.email],
                       it[UsersTable.uid],
                       Session.createSession(it[UsersTable.uid]).token,
                       it[UsersTable.admin],
                       it[UsersTable.native]
                   )
               }
           }

           return user!!
       }

       fun getAllUsersForAdminPanel()  : ArrayList<User> {
           val users: ArrayList<User> = arrayListOf()

           transaction {
               UsersTable.selectAll().forEach {
                   if (it[UsersTable.password] != "Deleted User")
                    users.add(User(
                        it[UsersTable.name],
                        it[UsersTable.picture],
                        it[UsersTable.email],
                        it[UsersTable.uid],
                        "",
                        it[UsersTable.admin],
                        it[UsersTable.native]
                    ))
               }
           }

           return users
       }

       fun validatePassword(uid: String, password: String) : Boolean {
           var result = false

           transaction {
               UsersTable.select(UsersTable.uid eq uid).forEach {
                   if (Password.verify(password, it[UsersTable.password]))
                       result = true
               }
           }

           return result
       }

       fun updatePassword(uid: String, pwd: String) {
           val password = if (pwd.isNotEmpty())
               Password.hash(pwd)
           else
               "null"

           transaction {
               UsersTable.update({UsersTable.uid eq uid}){
                   it[UsersTable.password] = password
               }
           }
       }

       fun doesEmailExistOutsideUid(email: String, uid: String?) : Boolean {
           var response = false

           transaction {
               UsersTable.select(UsersTable.email eq email).forEach {
                   if (uid == null || it[UsersTable.uid] != uid)
                    response = true
               }
           }
           return response
       }

       fun uidToEmail(uid: String) : String {
           var result = ""

           transaction {
               UsersTable.select(UsersTable.uid eq uid).forEach {
                   result = it[UsersTable.email]
               }
           }
           return result
       }

       suspend fun isUserAdmin(uid: String) : Boolean {
           if (!isNative(uid) && ServerSetting.getLdapLoginEnabled())
               updateUserAdminStatus(uid, Ldap().isUserAdmin(uidToEmail(uid)))

           var result = false

           transaction {
               UsersTable.select(UsersTable.uid eq uid).forEach {
                   result = it[UsersTable.admin]
               }
           }

           return result
       }

       fun isNative(uid: String) : Boolean {
           var result = false

           transaction {
               UsersTable.select(UsersTable.uid eq uid).forEach {
                   result = it[UsersTable.native]
               }
           }

           return result
       }

       fun emailToUid(email: String) : String {
           var result = ""

           transaction {
               UsersTable.select(UsersTable.email eq email).forEach {
                   result = it[UsersTable.uid]
               }
           }
           return result
       }

       fun getUserByUid(uid: String) : UserDataShort? {
           var user: UserDataShort? = null

           //Should only either return 1 or 0 records
           transaction {
               UsersTable.select(UsersTable.uid eq uid).forEach {
                   user = UserDataShort(
                       it[UsersTable.name],
                       it[UsersTable.picture],
                       it[UsersTable.uid],
                   )
               }
           }

           return user
       }

       fun getUserByUidFull(uid: String) : User? {
           var user: User? = null

           //Should only either return 1 or 0 records
           transaction {
               UsersTable.select(UsersTable.uid eq uid).forEach {
                   user = User(
                       it[UsersTable.name],
                       it[UsersTable.picture],
                       it[UsersTable.email],
                       it[UsersTable.uid],
                       Session.createSession(it[UsersTable.uid]).token,
                       it[UsersTable.admin],
                       it[UsersTable.native]
                   )
               }
           }

           return user
       }

       fun createUid() : String {
           var uid = UUID.randomUUID().toString()
           while (containsUid(uid))
               uid = UUID.randomUUID().toString()

           return uid
       }

       fun containsUid(uid: String) : Boolean {
           var result = false

           transaction {
               UsersTable.select { UsersTable.uid eq uid }.forEach {
                   result = true
               }
           }

           return result
       }

       fun insertUserRaw(user: User, password: String?) {
           val password = if (password != null)
               Password.hash(password)
           else
               "null"

           transaction {
               UsersTable.insert {
                   it[name] = user.name
                   it[picture] = user.picture
                   it[email] = user.email
                   it[UsersTable.password] = password
                   it[uid] = user.uid
                   it[admin] = user.admin
                   it[native] = user.native
               }
           }
       }

       fun updateUserName(uid: String, name: String) {
           transaction {
               UsersTable.update({UsersTable.uid eq uid}) {
                   it[UsersTable.name] = name
               }
           }
       }

       fun updateUserEmail(uid: String, email: String) : Boolean {
           return if (!doesEmailExistOutsideUid(email, uid)) {
               transaction {
                   UsersTable.update({ UsersTable.uid eq uid }) {
                       it[UsersTable.email] = email
                   }
               }
               true
           } else {
               false
           }
       }

       fun updateUserAdminStatus(uid: String, admin: Boolean) {
           transaction {
               UsersTable.update({UsersTable.uid eq uid}) {
                   it[UsersTable.admin] = admin
               }
           }
       }

       fun setImage(uid: String, imageUrl: String) {
           transaction {
               UsersTable.update({UsersTable.uid eq uid}) {
                   it[picture] = imageUrl
               }
           }
       }

       fun deleteUser(uid: String) {
           transaction {
               UsersTable.update({UsersTable.uid eq uid}) {
                   it[name] = "Deleted User"
                   it[picture] = "Deleted User"
                   it[password] = "Deleted User"
                   it[admin] = false
               }
           }
       }

       @Synchronized
       fun initialSetup() {
           transaction {
               if (!UsersTable.exists()) {
                   SchemaUtils.create(UsersTable)

                   UsersTable.insert {
                       it[name] = "Root"
                       it[picture] = "https://img3.gelbooru.com//images/7d/06/7d0609cc49c3c5e55f98c05ac2cea284.gif"
                       it[password] = Password.hash("Change me!")
                       it[email] = "root"
                       it[uid] = "ROOT"
                       it[admin] = true
                       it[native] = true
                   }
               }
           }
       }
   }
}