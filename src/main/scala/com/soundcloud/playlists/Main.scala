package com.soundcloud.playlists

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]) =
    PlaylistsServer.stream[IO].compile.drain.as(ExitCode.Success)
}
