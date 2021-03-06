package uk.gov.gds.common.mongo.repository

import com.novus.salat._
import com.novus.salat.global.NoTypeHints
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.MongoCollection
import uk.gov.gds.common.logging.Logging
import uk.gov.gds.common.pagination.PaginationSupport
import uk.gov.gds.common.repository.{ CursorBase, Repository }
import java.io.OutputStream
import uk.gov.gds.common.repository.Cursor
import com.mongodb.ReadPreference

abstract class MongoRepositoryBase[A <: CaseClass](implicit m: Manifest[A])
  extends Repository[A]
  with Logging
  with SyntacticSugarForMongoQueries
  with PaginationSupport {

  RegisterJodaTimeConversionHelpers()

  protected val collection: MongoCollection
  protected implicit val ctx = NoTypeHints
  protected lazy val emptyQuery = MongoDBObject()

  protected implicit def domainObj2mongoObj(o: A) = grater[A].asDBObject(o)

  protected implicit def domainList2MongoObj(o: List[A]) = o.map(grater[A].asDBObject(_))

  protected implicit def mongoOptObj2DomainObj(o: Option[collection.T]) = o.map(grater[A].asObject(_))

  protected implicit def mongoObj2DomainObj(o: collection.T) = grater[A].asObject(o)

  protected implicit def cursorOfMongoObject2listOfDomainObjects(c: collection.CursorType) = c.map(grater[A].asObject(_)).toList

  protected def createIndexes() {}

  protected def createReferenceData() {}

  def startup() {
    createIndexes()
    createReferenceData()
  }

  def dumpJSON(os: OutputStream) {
    val allCursor = all
    dumpPage(os, allCursor)
    while (allCursor.hasNextPage) {
      allCursor.gotoNextPage
      dumpPage(os, allCursor)
    }
  }

  private def dumpPage(os: OutputStream, page: Cursor[A]) {
    page.pageOfData.foreach { a =>
      os.write(grater[A].toCompactJSON(a).getBytes())
      os.write("\n".getBytes())
    }
  }

  protected def addIndex(index: DBObject,
                         unique: Boolean = Enforced,
                         sparse: Boolean = Sparse,
                         duplicate: Boolean = Keep) = createIndex(index, unique, sparse, duplicate)

  protected def addIndex(index: DBObject,
                         unique: Boolean,
                         sparse: Boolean,
                         duplicate: Boolean,
                         name: String) = createIndex(index, unique, sparse, duplicate, Some(name))

  protected def dropIndex(name: String) {
    logger.info("Dropping index '%s'".format(name))

    try {
      collection.underlying.dropIndex(name)
    } catch {
      case e =>
        logger.error("Could not drop index '%s'".format(name), e)
        throw e
    }
  }

  private def createIndex(index: DBObject,
                          unique: Boolean,
                          sparse: Boolean,
                          duplicate: Boolean,
                          name: Option[String] = None) {
    logger.info("Adding index " + index)

    val requiredOptions = List("unique" -> unique, "background" -> false, "sparse" -> sparse, "dropDups" -> duplicate)
    val additionalOptions = name.map("name" -> _).toList
    val options = requiredOptions ::: additionalOptions

    try {
      collection.underlying.ensureIndex(
        index,
        query(options: _*))
    } catch {
      case e: Exception =>
        logger.error("Could not create index " + index, e)
        throw e
    }
  }

  class SimpleMongoCursor(query: DBObject,
    pageSize: Int,
    currentPage: Long,
    order: Option[MongoDBObject] = None,
    customReadPreference: Option[ReadPreference] = None)
    extends CursorBase[A](pageSize, currentPage) {

    def pageOfData = logAndTimeQuery[List[A]](
      logMessage = "Mongo query: " + query + " with page:" + currentPage + " skip:" + skipSize + " page-size:" + pageSize + " sort order " + order,
      ensureReadPreference(currentPageQuery))

    def total = logAndTimeQuery(
      logMessage = "Mongo count: " + collection.name,
      query = collection.count(query))

    private def currentPageQuery =
      order match {
        case Some(direction) => collection.find(query).sort(direction).skip(skipSize).limit(pageSize)
        case _ => collection.find(query).skip(skipSize).limit(pageSize)
      }

    private def ensureReadPreference(cursor: MongoCursor) = {
      customReadPreference.foreach(cursor.underlying.setReadPreference)
      cursor
    }
  }

  object SimpleMongoCursor {

    def apply(query: DBObject) = buildCursor(query)

    def apply(query: DBObject, order: DBObject) = buildCursor(query = query, order = Some(order))

    def apply(query: DBObject, order: DBObject, pageSize: Int) = buildCursor(query = query, order = Some(order), pageSize = pageSize)

    def apply(query: DBObject, order: DBObject, pageSize: Int, readPreference: ReadPreference) =
      buildCursor(query = query, order = Some(order), pageSize = pageSize, customReadPreference = Some(readPreference))

    def apply(query: DBObject, page: Int, pageSize: Int) = buildCursor(
      query = query,
      currentPage = page,
      pageSize = pageSize)

    def apply(query: DBObject, page: Int, pageSize: Int, order: MongoDBObject) = buildCursor(
      query = query,
      pageSize = pageSize,
      currentPage = page,
      order = Some(order))

    def apply(pageSize: Int = defaultPageSize) = buildCursor(
      query = emptyQuery,
      pageSize = pageSize)

    private def buildCursor(query: DBObject,
      pageSize: Int = defaultPageSize,
      currentPage: Int = 1,
      order: Option[MongoDBObject] = None,
      customReadPreference: Option[ReadPreference] = None) =
      new SimpleMongoCursor(
        query = query,
        pageSize = pageSize,
        currentPage = currentPage,
        order = order,
        customReadPreference = customReadPreference)
  }

}