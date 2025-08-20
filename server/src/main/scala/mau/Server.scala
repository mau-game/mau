package mau

import cask.*
import cask.router.Result
import io.circe.*
import io.circe.syntax.*
import mau.edition.*
import mau.game.LoginParams
import mau.round.User
import mau.utils.Sanitizer

import java.time.Instant
import java.util.concurrent.Executors
import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scala.util.Properties

object Server extends MainRoutes:
  private val isLocal: Boolean = Properties.propIsSet("mau.local")
  private val globalPassword: String = if !isLocal then Env.envOrThrow("MAU_PASSWORD") else ""
  private val genApiKey: String = Properties.envOrElse("CODEGEN_API_KEY", "")

  private val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  private val userManager = UserManager()
  private val gameSessions = Seq.fill(1)(GameSession(genApiKey))
  private val userToGame = TrieMap.empty[User, GameSession]

  override def host: String = if isLocal then "localhost" else "0.0.0.0"

  println(s"Starting server on port $host:$port")
  startMonitoring()
  startClock()
  initialize()

  @staticResources("/assets/:fileName")
  def getClient(fileName: String): String = s"assets/$fileName"

  @get("/")
  def main(): Response[Response.Data] =
    val resourceRoot = getClass.getClassLoader
    StaticResource("index.html", resourceRoot, Seq("Content-Type" -> "text/html; charset=utf-8"))

  @authorized
  @mau.postJson("/login")
  def login(params: LoginParams)(username: String): Response[String] =
    this.synchronized:
      if gameSessions.forall(_.isFull) then
        println(s"$username cannot login because too many users")
        Response("Too many users. Please try again later.", statusCode = 409)
      else userManager.createUser(username, params.avatar) match
        case Some(sessionId) =>
          println(s"$username has joined the game")
          Response("", cookies = Seq(Cookie("sessionId", sessionId)))
        case None =>
          println(s"$username cannot login because already taken")
          Response(s"$username is already taken", statusCode = 401)

  @loggedIn
  @post("/logout")
  def logout()(user: User): Response[String] =
    userToGame.updateWith(user):
      case Some(game) =>
        game.removeUser(user)
        None
      case None => None
    userManager.removeUser(user)
    Response("", cookies = Seq(Cookie("sessionId", "")))

  @playing
  @mau.postJson("/api/submit")
  def submit(params: SubmitParams)(user: User, game: GameSession): CompilationReport =
    game.submit(user, params.code)

  @playing
  @mau.postJson("/api/compilation")
  def compile(params: CompilationParams)(user: User, game: GameSession): CompilationReport =
    game.compile(params.code)

  @playing
  @mau.postJson("/api/generation")
  def generate(params: GenerationParams)(user: User, game: GameSession): Generation =
    game.generate(params.prompt)

  @playing
  @mau.postJson("/api/completion")
  def complete(params: CompletionParams)(user: User, game: GameSession): Seq[Completion] =
    game.complete(params.code, params.position)

  @playing
  @mau.postJson("/api/completion/info")
  def info(params: Completion)(user: User, game: GameSession): Response[String] =
    cask.Response("Not yet implemented", 404)

  @websocket("/play")
  def play(request: Request): WebsocketResult =
    val userOpt =
      for
        cookie <- request.cookies.get("sessionId")
        user <- userManager.getUser(cookie.value)
      yield user

    userOpt match
      case None => Response("", 401)
      case Some(user) =>
        var handler: Option[WsHandler] = None         
        userToGame.updateWith(user):
          case Some(game) =>
            handler = Some(WsHandler(connection => game.addConnection(user, connection)))
            Some(game)
          case None =>
            
            gameSessions.find(game => !game.isFull) match
              case Some(game) => 
                handler = Some(WsHandler(connection => game.addUser(user, connection)))
                Some(game)
              case None => None
        handler.getOrElse(Response("Too many users. Please try again later.", 409))

  private def startClock(): Unit =
    val clock = new Thread:
      override def run(): Unit =
        while true do
          gameSessions.foreach(_.tick(Instant.now()))
          Thread.sleep(100)
    clock.start()
  
  private def startMonitoring(): Unit =
    val monitor = new Thread:
      override def run(): Unit =
        while true do
          for user <- userManager.users do
            val userGame = userToGame.updateWith(user)(_.filter(_.pingOrRemove(user)))
            if userGame.isEmpty then userManager.removeUser(user)
          Thread.sleep(10000)
    monitor.start()

  private class authorized extends RawDecorator:
    def wrapFunction(request: cask.Request, delegate: Delegate) =
      request.headers.get("authorization")
        .flatMap(_.headOption)
        .collect:
          case s"Basic $credentials" => utils.Base64.decode(credentials)
        .collect:
          case s"$username:$password" if isValidPassword(password) && Sanitizer.isValid(username) =>
            delegate(request, Map("username" -> username))
        .getOrElse(router.Result.Success(Response("Invalid username or password", 401)))

  private class playing extends loggedIn:
    override def wrapFunction(request: cask.Request, delegate: Delegate): Result[Response.Raw] =
      super.wrapFunction(
        request,
        (request, map) =>
          val user = map("user").asInstanceOf[User]
          userToGame.get(user) match
            case None => Result.Success(Response("user is not playing", 403))
            case Some(game) => delegate(request, map + ("game" -> game))
      )

  private class loggedIn extends RawDecorator:
    def wrapFunction(request: cask.Request, delegate: Delegate): Result[Response.Raw] =
      request.cookies.get("sessionId").flatMap(c => userManager.getUser(c.value)) match
        case Some(user) => delegate(request, Map("user" -> user))
        case None => Result.Success(Response("", 401))

  private def isValidPassword(password: String): Boolean =
    isLocal || password == globalPassword
