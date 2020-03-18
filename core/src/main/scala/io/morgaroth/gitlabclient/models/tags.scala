package io.morgaroth.gitlabclient.models

case class ReleaseShort(
                         tag_name: String,
                         description: String,
                       )

case class TagInfo(
                    commit: CommitSimple,
                    release: Option[ReleaseShort],
                    name: String,
                    target: String,
                    message: Option[String],
                    `protected`: Boolean,
                  )