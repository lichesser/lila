package lila.app
package mashup

import chess.{ EloCalculator, Color }
import lila.bookmark.BookmarkApi
import lila.forum.PostApi
import lila.game.{ GameRepo, Game }
import lila.relation.RelationApi
import lila.user.{ User, UserRepo, Context, EloChart, Confrontation }

case class UserInfo(
    user: User,
    rank: Option[(Int, Int)],
    nbPlaying: Int,
    confrontation: Option[Confrontation],
    nbBookmark: Int,
    eloChart: Option[EloChart],
    nbFollowing: Int,
    nbFollowers: Int,
    nbPosts: Int) {

  def nbRated = user.count.rated

  def nbWithMe = confrontation ?? (_.games)

  def percentRated: Int = math.round(nbRated / user.count.game.toFloat * 100)
}

object UserInfo {

  def apply(
    countUsers: () ⇒ Fu[Int],
    bookmarkApi: BookmarkApi,
    eloCalculator: EloCalculator,
    relationApi: RelationApi,
    postApi: PostApi,
    getRank: String ⇒ Fu[Option[Int]])(user: User, ctx: Context): Fu[UserInfo] =
    (getRank(user.id) flatMap {
      _ ?? { rank ⇒ countUsers() map { nb ⇒ (rank -> nb).some } }
    }) zip
      ((ctx is user) ?? {
        GameRepo count (_ notFinished user.id) map (_.some)
      }) zip
      (ctx.me.filter(user!=) ?? { me ⇒
        GameRepo.confrontation(me, user) map (_.some filterNot (_.empty))
      }) zip
      (bookmarkApi countByUser user) zip
      EloChart(user) zip
      relationApi.nbFollowing(user.id) zip
      relationApi.nbFollowers(user.id) zip
      postApi.nbByUser(user.id) map {
        case (((((((rank, nbPlaying), confrontation), nbBookmark), eloChart), nbFollowing), nbFollowers), nbPosts) ⇒ new UserInfo(
          user = user,
          rank = rank,
          nbPlaying = ~nbPlaying,
          confrontation = confrontation,
          nbBookmark = nbBookmark,
          eloChart = eloChart,
          nbFollowing = nbFollowing,
          nbFollowers = nbFollowers,
          nbPosts = nbPosts)
      }
}
