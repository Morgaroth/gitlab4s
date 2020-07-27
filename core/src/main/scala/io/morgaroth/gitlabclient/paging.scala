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

sealed trait TagsSort extends SortingFamily

object TagsSort {

  case object Updated extends TagsSort {
    override val property: String = "updated"
  }

  case object Name extends TagsSort {
    override val property: String = "name"
  }

}

sealed class DeploymentsSort(value: String) extends SortingFamily {
  override def property = value
}

object DeploymentsSort {

  case object IId extends DeploymentsSort("updated")

  case object Id extends DeploymentsSort("id")

  case object CreatedAt extends DeploymentsSort("created_at")

  case object UpdatedAt extends DeploymentsSort("updated_at")

  case object Ref extends DeploymentsSort("ref")

}

sealed trait ProjectsSort extends SortingFamily

object ProjectsSort {

  case object Id extends ProjectsSort {
    override val property: String = "id"
  }

  case object Name extends ProjectsSort {
    override val property: String = "name"
  }

  case object CreatedAt extends ProjectsSort {
    override val property: String = "created_at"
  }

  case object UpdatedAt extends ProjectsSort {
    override val property: String = "updated_at"
  }

  case object LastActivityAt extends ProjectsSort {
    override val property: String = "last_activity_at"
  }

  case object Path extends ProjectsSort {
    override val property: String = "path"
  }

}
sealed trait EventsSort extends SortingFamily

object EventsSort {

  case object CreatedAt extends EventsSort {
    override val property: String = "created_at"
  }
}