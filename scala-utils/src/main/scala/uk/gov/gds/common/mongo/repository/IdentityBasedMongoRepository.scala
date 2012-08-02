package uk.gov.gds.common.mongo.repository

import com.novus.salat.CaseClass
import uk.gov.gds.common.repository.HasIdentity
import com.mongodb.casbah.MongoCollection

abstract class IdentityBasedMongoRepository[A <: CaseClass with HasIdentity](thisCollection: MongoCollection)(implicit m: Manifest[A])
  extends MongoRepositoryBase[A](thisCollection) {

  protected val databaseIdProperty: String

  override def load(id: String) = findOne(where(databaseIdProperty -> id))

  override def load(ids: List[String]) = SimpleMongoCursor(where(databaseIdProperty -> in(ids)))

  def delete(id: String) {
    super.delete(where(databaseIdProperty -> id))
  }

  override def startup() {
    super.startup()
    createIdIndex()
  }

  protected def createIdIndex() {
    addIndex(index(databaseIdProperty -> Ascending))
  }
}