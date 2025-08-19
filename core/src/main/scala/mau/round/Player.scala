package mau.round

import io.circe.Codec

final case class Player(user: User, id: Int) derives Codec:
  override def toString = user.name
