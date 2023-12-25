package smithy4s.zio

import cats.effect.SyncIO
import org.typelevel.vault.Key
import smithy4s.Endpoint
import smithy4s.http.PathParams
import zio.{Task, ZIO}
import zio.http.*

package object http {
  type HttpRoutes = Routes[Any, Throwable]
  type ClientEndpointMiddleware = Endpoint.Middleware[Client]
  type ServerEndpointMiddleware = Endpoint.Middleware[HttpRoutes]
  type SimpleHandler = Request => Task[Response]

  private val pathParamsKey: String =
    Key.newKey[SyncIO, PathParams].unsafeRunSync().hashCode().toString

  private def serializePathParams(pathParams: PathParams): String = {
    pathParams.map { case (key, value) => s"$key=$value" }.mkString("&")
  }

  private def deserializePathParams(pathParamsString: String): PathParams = {
    pathParamsString
      .split("&")
      .filterNot(_.isEmpty)
      .map { param =>
        val Array(key, value) = param.split("=")
        key -> value
      }
      .toMap
  }
  def tagRequest(req: Request, pathParams: PathParams): Request = {
    val serializedPathParams = serializePathParams(pathParams)
    req.addHeader(Header.Custom(pathParamsKey, serializedPathParams))
  }
  def lookupPathParams(req: Request): (Request, Option[PathParams]) = {
    val pathParamsString = req.headers.get(pathParamsKey)
    (
      req.removeHeader(pathParamsKey),
      pathParamsString.map(deserializePathParams)
    )
  }

  implicit class AppOps[A, E](eff: ZIO[A, Option[E], Response]) {
    def orNotFound: ZIO[A, E, Response] = eff.unsome.map {
      case None        => Response.status(Status.NotFound)
      case Some(value) => value
    }
  }
}
