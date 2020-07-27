package io.morgaroth.gitlabclient

import cats.data.EitherT
import io.morgaroth.gitlabclient.models._
import io.morgaroth.gitlabclient.query.ParamQuery._

trait EventsAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  // @see: https://docs.gitlab.com/ee/api/events.html#list-currently-authenticated-users-events
  def getEvents(
                 since: UtcDate = null,
                 until: UtcDate = null,
                 targetType: TargetType = null,
                 action: ActionType = null,
                 paging: Paging = AllPages,
                 sort: Sorting[EventsSort] = null,
               ): EitherT[F, GitlabError, Vector[EventInfo]] = {
    val params = Vector(
      wrap(since).map(_.toDateStr).map("after".eqParam(_)),
      wrap(until).map(_.toDateStr).map("before".eqParam(_)),
      wrap(targetType).map(_.name).map("target_type".eqParam(_)),
      wrap(action).map(_.name).map("action".eqParam(_)),
      Vector("scope".eqParam("all")),
      wrap(sort).flatMap(_.toParams),
    ).flatten
    val req = reqGen.get(API + s"/events", params: _*)
    getAllPaginatedResponse[EventInfo](req, "events", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/events.html#get-user-contribution-events
  def getUserContributionEvents(
                                 userId: BigInt,
                                 since: UtcDate = null,
                                 until: UtcDate = null,
                                 targetType: TargetType = null,
                                 action: ActionType = null,
                                 paging: Paging = AllPages,
                                 sort: Sorting[EventsSort] = null,
                               ): EitherT[F, GitlabError, Vector[EventInfo]] = {
    val params = Vector(
      wrap(since).map(_.toDateStr).map("after".eqParam(_)),
      wrap(until).map(_.toDateStr).map("before".eqParam(_)),
      wrap(targetType).map(_.name).map("target_type".eqParam(_)),
      wrap(action).map(_.name).map("action".eqParam(_)),
      wrap(sort).flatMap(_.toParams),
    ).flatten
    val req = reqGen.get(API + s"/users/$userId/events", params: _*)
    getAllPaginatedResponse[EventInfo](req, "events", paging)
  }

}
