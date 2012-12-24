package uk.gov.gds.common.mongo.repository

import com.novus.salat._
import com.mongodb.casbah.Imports._
import com.mongodb.WriteConcern.{MAJORITY => safe, NORMAL => unsafe}

abstract class SimpleMongoRepository[A <: CaseClass](implicit m: Manifest[A]) extends MongoRepositoryBase[A] {

  def load(id: String) = load(oid(id))

  def load(id: ObjectId) = findOne(where("_id" -> id))

  def load(ids: List[String]) = SimpleMongoCursor(where("_id" -> inOids(ids)))

  def get(id: String): A = get(oid(id))

  def get(id: ObjectId): A = load(id).getOrElse(throw new NoSuchObjectException(id))

  def insertWith(writeConcern: WriteConcern, obj: A) = insert(obj, writeConcern)

  def bulkInsertWith(writeConcern: WriteConcern, obj: List[A]) = bulkInsert(obj, writeConcern)

  def updateWith(writeConcern: WriteConcern, query: DBObject, obj: DBObject, upsert: Boolean, multi: Boolean) =
    collection.update(query, obj, upsert, multi, writeConcern)

  def findOne(query: FindQuery) = whereQueryBuilder(query).findOne

  def findAll(query: FindQuery) = whereQueryBuilder(query).findAll

  def safeInsert(obj: A) = insertWith(safe, obj)

  def unsafeInsert(obj: A) = insertWith(unsafe, obj)

  def safeUpdate(query: DBObject, obj: DBObject, upsert: Boolean = true, multi: Boolean = false) =
    updateWith(safe, query, obj, upsert, multi)

  def unSafeUpdate(query: DBObject, obj: DBObject, upsert: Boolean = true, multi: Boolean = false) =
    updateWith(unsafe, query, obj, upsert, multi)

  def unsafeDelete(id: String) = unsafeDelete(where("_id" -> oid(id)))

  def safeDelete(id: String) = safeDelete(where("_id" -> oid(id)))

  def unsafeDelete(query: DBObject) = collection.remove(query, unsafe)

  def safeDelete(query: DBObject) = collection.remove(query, safe)

  def deleteAll() {
    collection.remove(query())
  }

  def findAndModify(query: DBObject, update: DBObject, returnNew: Boolean = false) =
    collection.findAndModify(query = query, update = update, sort = null, fields = null, remove = false, returnNew = returnNew, upsert = false)

  def all = SimpleMongoCursor()

  def allFields = MongoDBObject.empty

  def addFieldWith(writeConcern: WriteConcern, fieldName: String, defaultValue: String = "") = updateWith(writeConcern,
    allFields, update("$set" -> field(fieldName, defaultValue)), false, true)

  def removeFieldWith(writeConcern: WriteConcern, fieldName: String, defaultValue: String = "") = updateWith(writeConcern,
    allFields, update("$unset" -> field(fieldName, defaultValue)), false, true)

  def field(fieldName: String, defaultValue: String = "") = values(fieldName -> defaultValue)

  def addField(fieldName: String, defaultValue: String = "") =
    safeUpdate(allFields, update("$set" -> field(fieldName, defaultValue)), false, true)

  def removeField(fieldName: String, defaultValue: String = "") = safeUpdate(allFields,
    update("$unset" -> field(fieldName, defaultValue)), false, true)

  def findOne(filter: DBObject) = collection.findOne(filter)

  protected def findAll(filter: DBObject): List[A] = collection.find(filter)

  @inline private final def whereQueryBuilder(f: FindQuery) = {
    val queryBuilder = GdsFindQueryBuilder(ModelProxyFactory.proxy[A])
    f(queryBuilder, queryBuilder.schema)
    queryBuilder
  }

  @inline private final def update(query: DBObject, obj: DBObject, upsert: Boolean = true, multi: Boolean = false, writeConcern: WriteConcern) =
    collection.update(query, obj, upsert, multi, writeConcern)

  @inline private final def insert(obj: A, writeConcern: WriteConcern) = {
    val query = domainObj2mongoObj(obj)
    collection.insert(query, writeConcern)
    grater[A].asObject(query)
  }

  @inline private final def bulkInsert(obj: List[A], writeConcern: WriteConcern) = {
    val query = domainList2MongoObj(obj)
    collection.insert(query, writeConcern)
    query.map(grater[A].asObject(_))
  }
}
