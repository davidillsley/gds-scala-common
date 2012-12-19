package uk.gov.gds.common.repository

import com.mongodb.casbah.Imports._
import uk.gov.gds.common.mongo.repository.GdsFindQueryBuilder
import com.novus.salat.CaseClass

trait Repository[A <: CaseClass] {

  type FindQuery = (GdsFindQueryBuilder[A], A) => Unit

  def safeInsert(obj: A): A

  def unsafeInsert(obj: A): A

  def findOne(filter: DBObject): Option[A]

  def load(id: String): Option[A]

  def load(ids: List[String]): Cursor[A]

  def safeDelete(id: String): WriteResult

  def unsafeDelete(id: String): WriteResult

  def safeDelete(query: DBObject): WriteResult

  def unsafeDelete(query: DBObject): WriteResult

  def deleteAll(): Unit

  def findAndModify(query: DBObject, update: DBObject, returnNew: Boolean = false): Option[A]

  def all: Cursor[A]

  def get(id: String): A

  def get(id: ObjectId): A

  def findOne(query: FindQuery): Option[A]

  def findAll(query: FindQuery): List[A]
}

