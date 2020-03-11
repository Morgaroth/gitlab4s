package io.morgaroth.gitlabclient.models

import java.time.ZonedDateTime


case class CommitSimple(
                         id: String,
                         short_id: String,
                         title: String,
                         author_name: String,
                         author_email: String,
                         authored_date: ZonedDateTime,
                         committer_name: String,
                         committer_email: String,
                         committed_date: ZonedDateTime,
                         created_at: ZonedDateTime,
                         message: String,
                         parent_ids: Vector[String],
                       )

case class CommitStats(
                        additions: Int,
                        deletions: Int,
                        total: Int,
                      )

case class LastPipelineInfo(
                             id: BigInt,
                             ref: String,
                             sha: String,
                             status: String,
                           )

case class Commit(
                   id: String,
                   short_id: String,
                   title: String,
                   author_name: String,
                   author_email: String,
                   authored_date: ZonedDateTime,
                   committer_name: String,
                   committer_email: String,
                   committed_date: ZonedDateTime,
                   created_at: ZonedDateTime,
                   message: String,
                   parent_ids: Vector[String],
                   web_url: String,
                   stats: CommitStats,
                   last_pipeline: LastPipelineInfo,
                   status: String,
                 )

case class CommitDiff(
                       diff: String,
                       new_path: String,
                       old_path: String,
                       a_mode: String,
                       b_mode: String,
                       new_file: Boolean,
                       renamed_file: Boolean,
                       deleted_file: Boolean
                     )