package io.morgaroth.gitlabclient

sealed trait Paging

case object AllPages extends Paging

case class PageCount(pagesCount: Int, pageSize: Int) extends Paging

case class EntitiesCount(expectedEntitiesCount: Int) extends Paging


sealed trait SortDirection

case object Desc extends SortDirection {
  override def toString: String = "desc"
}

case object Asc extends SortDirection {
  override def toString: String = "asc"
}

case class Sorting[T <: SortingFamily](field: T, direction: SortDirection = Desc)

trait SortingFamily {
  def property: String
}

sealed trait MergeRequestNotesSort extends SortingFamily

sealed trait MergeRequestsSort extends SortingFamily

case object CreatedBy extends MergeRequestNotesSort with MergeRequestsSort {
  override val property: String = "created_by"
}

case object UpdatedBy extends MergeRequestNotesSort with MergeRequestsSort {
  override val property: String = "updated_at"
}