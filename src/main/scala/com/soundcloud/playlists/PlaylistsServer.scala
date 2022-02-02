package com.soundcloud.playlists

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.comcast.ip4s._
import com.soundcloud.playlists.database.Migrations
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import com.soundcloud.playlists.database.PlaylistRepository
object PlaylistsServer {

  def stream[F[_]: Async]: Stream[F, Nothing] = {

    val httpApp = (
      PlaylistsRoutes.playlistRoutes[F](new PlaylistRepository[F]())
    ).orNotFound

    val finalHttpApp = Logger.httpApp(true, true)(httpApp)

    for {
      _ <- Migrations.migrate[F]()
      exitCode <- Stream.resource(
        EmberServerBuilder
          .default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build >>
          Resource.eval(Async[F].never)
      )
    } yield exitCode
  }.drain
}
