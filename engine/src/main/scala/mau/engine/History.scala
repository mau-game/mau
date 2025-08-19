package mau.engine
import mau.game.EventProjection
import mau.round.Player

opaque type History = Seq[Event]

object History:
  def empty: History = Seq.empty
  extension (history: History)
    def add(event: Event): History = event +: history
    def project(player: Player): Seq[EventProjection] =
      history.map(_.project(player))