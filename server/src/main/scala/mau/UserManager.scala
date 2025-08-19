package mau

import scala.collection.concurrent.TrieMap
import java.util.UUID
import mau.round.User

class UserManager:
  private val usersToSessions: TrieMap[String, String] = TrieMap.empty
  private val sessionsToUsers: TrieMap[String, User] = TrieMap.empty

  def users: Seq[User] = sessionsToUsers.values.toSeq

  def createUser(name: String, avatar: String): Option[String] = synchronized:
    val user = User(name, avatar)
    Option.when(!usersToSessions.contains(user.name)):
      val sessionId = UUID.randomUUID().toString
      usersToSessions.addOne(user.name -> sessionId)
      sessionsToUsers.addOne(sessionId -> user)
      sessionId

  def getUser(sessionId: String): Option[User] = sessionsToUsers.get(sessionId)
  
  def removeUser(user: User): Unit = synchronized:
    usersToSessions.remove(user.name).foreach(sessionsToUsers.remove)

  def removeSession(sessionId: String): Unit = synchronized:
    sessionsToUsers.remove(sessionId).foreach(u => usersToSessions.remove(u.name))
