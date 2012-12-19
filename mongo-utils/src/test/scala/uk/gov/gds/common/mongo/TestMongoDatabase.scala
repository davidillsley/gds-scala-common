package uk.gov.gds.common.mongo

class TestMongoDatabase(override val databaseName: String) extends MongoDatabaseManager {
  protected val repositoriesToInitialiseOnStartup = Nil
}

object TestMongoDatabase {

  def apply[A](a: A)(implicit m: Manifest[A]) = new TestMongoDatabase(uniqName(m.erasure.asInstanceOf[Class[A]]))

  private def uniqName[A](clazz: Class[A]) = clazz.getSimpleName + "_" + System.nanoTime()
}