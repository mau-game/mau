package mau

import com.raquo.laminar.api.L.*
import mau.round.Player

import scala.scalajs.js.timers.setTimeout

def victoryScreen(winner: Player, isYou: Boolean): Modifier[HtmlElement] =
	child <-- EventStream.fromValue(winner).withTiming(6000).map:
		case Some(winner) =>
			div(
				className := Seq("victory-page", s"player-${winner.id}-victory"),
				className.toggle("fade-out") <-- EventStream.delay(5000).mapTo(true),
				div(
					className := "rising-sun",
					div(
						className := "spinning-rays",
						(0 to 9).map(i => div(className := s"pair-of-rays r$i"))
					),
					div(
						className := "sun",
						img(
						className := "winner-profile-pic",
						src := Assets.Avatars(winner.user.avatar),
						alt := "Winner Profile Picture"
						)
					)
				),
				div(
					className := "victory-header",
					if isYou then h1("CONGRA\u00ADTULATIONS!")
					else Seq(h1(winner.user.name), h2("is victorious"))
				),
				div(
					className := "victory-message",
					if isYou then h2("You are the winner of this round!")
					else h2("Take a moment to congratulate the winner!"),
				)
			)
		case None => emptyNode
