package mau

import com.raquo.laminar.api.L.*
import io.circe.*
import io.circe.syntax.*
import mau.edition.*
import mau.game.LoginParams

object Endpoints:
  def login(username: String, password: String, avatar: String): EventStream[Either[String, Unit]] =
    val token = utils.Base64.encode(s"$username:$password") 
    FetchStream.raw
      .post(
        "/login",
        opt =>
          val params = io.circe.Printer.noSpaces.print(LoginParams(avatar).asJson)
          opt.headers("authorization" -> s"Basic $token")
          opt.body(params)
      )
      .flatMapSwitch: response =>
        if response.status == 200 then EventStream.fromValue(Right(()))
        else EventStream.fromJsPromise(response.text()).map(Left(_))

  val logout: EventStream[Unit] =
    FetchStream.post("/logout", identity).mapTo(())

  def compile(code: String): EventStream[CompilationReport] =
    val params = Printer.noSpaces.print(CompilationParams(code).asJson)
    FetchStream
      .post("/api/compilation", options => options.body(params))
      .map(parser.decode[CompilationReport])
      .collect { case Right(report) => report }

  def submit(code: String): EventStream[CompilationReport] =
    val params = Printer.noSpaces.print(CompilationParams(code).asJson)
    FetchStream
      .post("/api/submit", options => options.body(params))
      .map(parser.decode[CompilationReport])
      .collect { case Right(report) if !report.isEmpty => report }

  def generate(prompt: String, abortStream: EventStream[Any]): EventStream[Option[Generation]] =
    val params = Printer.noSpaces.print(GenerationParams(prompt).asJson)
    FetchStream
      .post(
        "/api/generation", 
        options =>
          options.abortStream(abortStream)
          options.body(params)
      )
      .map(parser.decode[Generation](_).toOption)