package lila.user

import scala.concurrent.duration._
import scala.collection.mutable

import spray.caching.{ LruCache, Cache }

import play.api.libs.concurrent.Execution.Implicits._

final class Cached(userRepo: UserRepo, ttl: Duration) {

  def username(id: String): Option[String] =
    usernameCache.fromFuture(id.toLowerCase)(userRepo usernameById id).await

  def usernameOrAnonymous(id: String): String = username(id) | Users.anonymous

  def usernameOrAnonymous(id: Option[String]): String = (id flatMap username) | Users.anonymous

  def countEnabled: Fu[Int] = countEnabledCache.fromFuture(true)(userRepo.countEnabled)

  // id => username
  private val usernameCache: Cache[Option[String]] = LruCache(maxCapacity = 99999)

  private val countEnabledCache: Cache[Int] = LruCache(timeToLive = ttl)
}
