package mau

import cask.*
import io.circe.*
import io.circe.syntax.*
import mau.edition.*
import mau.engine.*
import mau.exec.RuleCompilation
import mau.exec.RuleExecutor
import mau.exec.Workspace
import mau.game.Command
import mau.round.*

import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.util.Properties

class GameSession(ruleExecutor: RuleExecutor, generator: RuleGenerator, engine: GameEngine)(using ec: ExecutionContext):
  private val userConnections = TrieMap.empty[User, Set[WsChannelActor]]
  private var currentState = AtomicReference(engine.initState)

  def pingOrRemove(user: User): Boolean =
    val isConnected = userConnections
      .updateWith(user)(_.map(pingConnections(user, _)).filter(_.nonEmpty))
      .isDefined
    if !isConnected then updateState(engine.exitGame(_, user))
    isConnected
  
  private def pingConnections(user: User, connections: Set[WsChannelActor]): Set[WsChannelActor] =
    connections.filter: connection =>
      try
        connection.run(Ws.Ping())
        true
      catch case e: IOException =>
        println(s"websocket of $user has disconnected")
        false

  def removeUser(user: User): Unit =
    userConnections.updateWith(user): opt =>
      opt.toSeq.flatten.foreach(_.send(Ws.Close()))
      None
    updateState(engine.exitGame(_, user))

  def submit(user: User, code: String): CompilationReport =
    val compilation = ruleExecutor.compile(code)
    compilation match
      case RuleCompilation.Success(rule) => updateState(engine.submit(_, user, rule))
      case RuleCompilation.Report(report) => ()
    compilation.toReport
    
  def compile(code: String): CompilationReport = ruleExecutor.compile(code).toReport
  def generate(prompt: String): Generation = generator.generate(prompt)
  def complete(code: String, position: Int): Seq[Completion] = ruleExecutor.complete(code, position)

  def isFull: Boolean = userConnections.size >= 5
  
  def addUser(user: User, connection: WsChannelActor)(using castor.Context, cask.util.Logger): WsActor =
    userConnections.update(user, Set(connection))
    updateState(engine.enterGame(_, user))
    createActor(user)

  def addConnection(user: User, connection: WsChannelActor)(using castor.Context, cask.util.Logger): WsActor =
    userConnections.updateWith(user)(_.map(_ + connection))
    notifyUser(user, connection, currentState.get)
    createActor(user)

  def tick(now: Instant): Unit = updateState(engine.tick(_, Instant.now()))

  private def createActor(user: User)(using castor.Context, cask.util.Logger): WsActor = WsActor:
      case Ws.Text(raw) =>
        try
          parser.decode[Command](raw) match 
            case Left(error) => throw error 
            case Right(command) => updateState(engine.handle(_, user, command))
        catch case e: Throwable =>
          println(e)
          e.printStackTrace()

  private def updateState[T](f: GameState => Option[GameState]): Unit =
    var updatedState: Option[GameState] = None
    val _ = currentState.updateAndGet: state =>
      try updatedState = f(state)
      catch case e: Throwable =>
        println(e)
        e.printStackTrace()
      updatedState.getOrElse(state)
    updatedState.foreach(notifyUsers)

  private def notifyUsers(state: GameState): Unit =
    for
      (user, connections) <- userConnections
      connection <- connections
    do notifyUser(user, connection, state)

  private def notifyUser(user: User, connection: WsChannelActor, state: GameState): Unit =
    state.project(user).foreach(proj => connection.send(Ws.Text(Printer.noSpaces.print(proj.asJson))))

object GameSession:
  def apply(genApiKey: String)(using ExecutionContext): GameSession =
    val workspace = Workspace.init()
    val classpath = Properties.javaClassPath.split(':').map(Paths.get(_))
    val ruleExecutor = RuleExecutor(workspace, classpath)
    val generator = RuleGenerator("l47vg0b974.execute-api.eu-west-1.amazonaws.com", genApiKey, ruleExecutor)
    val gameEngine = GameEngine(ruleExecutor)
    new GameSession(ruleExecutor, generator, gameEngine)

