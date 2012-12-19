package uk.gov.gds.common.repository

import org.scalatest.{GivenWhenThen, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import uk.gov.gds.common.mongo.repository.SimpleMongoRepository
import uk.gov.gds.common.mongo.TestMongoDatabase
import org.scalatest.mock.MockitoSugar

case class ModelObject(name: String, value: Int = 1, unmodified: String = "unmodified")

class MongoQueryBuilderTest extends FunSuite with ShouldMatchers with GivenWhenThen with MockitoSugar {

  private val database = TestMongoDatabase(this.getClass)
  private val repository = new ModelObjectRepository


  class ModelObjectRepository extends SimpleMongoRepository[ModelObject] {
    protected val collection = database("updatequerytests")
  }

  test("Should be able to find an object based on a string property") {
    given("A repository which contains two objects")

    repository.safeInsert(ModelObject(name = "foo"))
    repository.safeInsert(ModelObject(name = "bar"))

    when("we attempt to update the item called foo")

    repository.find {
      (query, schema) =>
        query
          .eq(schema.name, "foo")
          .eq(schema.value, 13)
    }
  }


}
