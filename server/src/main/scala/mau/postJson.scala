package mau

import cask.*
import io.circe.*
import io.circe.syntax.*

import cask.endpoints.JsonData
import cask.router.ArgReader
import cask.endpoints.ParamReader
import cask.router.Result
import cask.model.Response.DataCompanion
import java.io.OutputStream

sealed trait JsReader[T] extends ArgReader[Json, T, cask.model.Request]
object JsReader:
  implicit def defaultJsReader[T: Decoder]: JsReader[T] =
    new JsReader[T]:
      def arity = 1
      def read(ctx: cask.model.Request, label: String, input: Json): T =
        Decoder[T].decodeJson(input) match
          case Left(error) => throw error
          case Right(res) => res
        

  implicit def paramReader[T: ParamReader](using reader: ParamReader[T]): JsReader[T] =
    new JsReader[T]:
      override def arity = 0
      override def unknownQueryParams: Boolean = reader.unknownQueryParams
      override def remainingPathSegments: Boolean = reader.remainingPathSegments
      override def read(ctx: cask.model.Request, label: String, v: Json) = reader.read(ctx, label, ())
end JsReader

trait JsonData extends Response.Data
object JsonData extends DataCompanion[JsonData]:
  implicit class JsonDataImpl[T: Encoder](t: T) extends JsonData:
    def headers = Seq("Content-Type" -> "application/json")
    def write(out: OutputStream) =
      out.write(Printer.noSpaces.print(t.asJson).getBytes)
      out.flush()

class postJson(val path: String, subpath: Boolean = false) extends HttpEndpoint[Response[JsonData], Json]:
  val methods = Seq("post")
  type InputParser[T] = JsReader[T]
  def wrapFunction(ctx: Request, delegate: Delegate): Result[Response.Raw] =
    val bytes = ctx.exchange.getInputStream.readAllBytes
    parser.parse(new String(bytes)) match
      case Left(err) =>
        val response = cask.model.Response(err.toString, statusCode = 400)
        Result.Success(response.map(Response.Data.WritableData(_)))
      case Right(input) => delegate(ctx, Map("params" -> input))
  def wrapPathSegment(s: String): Json =
    parser.parse(s) match
      case Left(err) => throw err
      case Right(json) => json