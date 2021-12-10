package io.gitlab.mateuszjaje.gitlabclient

trait GitlabError

case class GitlabRequestingError(description: String, requestId: String, cause: Throwable) extends GitlabError

case class GitlabHttpError(statusCode: Int, description: String, requestId: String, requestType: String, errorBody: Option[String])
    extends GitlabError

case class GitlabMarshallingError(description: String, requestId: String, cause: Throwable) extends GitlabError

case class GitlabUnmarshallingError(description: String, requestId: String, cause: Throwable) extends GitlabError
