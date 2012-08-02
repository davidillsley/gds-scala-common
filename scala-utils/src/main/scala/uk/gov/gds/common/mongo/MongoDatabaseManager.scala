package uk.gov.gds.common.mongo

import migration._
import repository.{IdentityBasedMongoRepository, MongoRepositoryBase}
import uk.gov.gds.common.logging.Logging
import uk.gov.gds.common.j2ee.ContainerEventListener
import uk.gov.gds.common.config.Config
import com.mongodb.casbah.{MongoDB, MongoConnection}
import com.mongodb.{WriteConcern, ServerAddress}
import com.mongodb.WriteConcern.{NORMAL, SAFE}

abstract class MongoDatabaseManager extends ContainerEventListener with Logging {

  lazy val database: MongoDB = {
    logger.info("Connection to database: " + databaseName)
    mongoConnection(databaseName)
  }
  val changeLogRepository = new ChangeLogRepository(this)

  private lazy val databaseHosts = {
    val databaseHostString = Config("mongo.database.hosts")
    logger.info("Mongo Database Hosts: " + databaseHostString)
    databaseHostString.split(",").toList
  }

  private lazy val mongoConnection = MongoConnection(databaseHosts.map(new ServerAddress(_)))

  protected val repositoriesToInitialiseOnStartup: List[MongoRepositoryBase[_]]

  def databaseChangeScripts: List[ChangeScript] = Nil

  def removeData() {
    repositoriesToInitialiseOnStartup.foreach {
      item => item.deleteAll()
    }
  }

  protected def databaseName = Config("mongo.database.name")

  override def startup() {
    initializeDatabase()
  }

  def apply(collectionName: String) = collection(collectionName)

  def collection(collectionName: String) = {
    val collection = database(collectionName)
    collection.slaveOk()
    collection
  }

  def initializeDatabase(writeConcern: WriteConcern = SAFE) {
    synchronized {
      withWriteConcern(writeConcern) {
        initialiseRepositories()
        applyDatabaseChangeScripts()
      }
    }
  }

  def emptyDatabase(writeConcern: WriteConcern = NORMAL) {
    synchronized {
      withWriteConcern(writeConcern) {
        database.dropDatabase()
      }
    }
  }

  def appliedChangeScripts = changeLogRepository.all

  def changeScriptAuditFor(changeScriptName: String) = changeLogRepository.load(changeScriptName)

  def changeScriptAuditFor(changeScript: ChangeScript) = changeLogRepository.load(changeScript.name)

  private def initialiseRepositories() {
    changeLogRepository.startup()

    repositoriesToInitialiseOnStartup.foreach {
      repository =>
        logger.info("Initialising repository " + repository.getClass.getSimpleName)
        repository.startup()
    }

    logger.info("All repositories initialised")
  }

  def withWriteConcern(writeConcern: WriteConcern)(block: => Unit) = {
    val currentWriteConcern = database.getWriteConcern()

    try {
      database.setWriteConcern(writeConcern)
      block
    }
    finally {
      database.setWriteConcern(currentWriteConcern)
    }
  }

  private def applyDatabaseChangeScripts() {
    databaseChangeScripts.foreach(applyChangeScript(_))
  }

  private def applyChangeScript(changeScript: ChangeScript) {
    changeScriptAuditFor(changeScript) match {
      case Some(audit) if (ChangeScriptStatus.ok.equals(audit.status)) =>
        logger.debug("Change script " + changeScript.name + " has already been applied")

      case _ => commitChangeScript(changeScript)
    }
  }

  private def commitChangeScript(changeScript: ChangeScript) {
    logger.info("Applying change script " + changeScript.name)

    try {
      changeScript.applyToDatabase()
      changeLogRepository.safeInsert(SuccessfulChangeScriptAudit(changeScript))
    }
    catch {
      case e: Exception =>
        changeLogRepository.safeInsert(FailedChangeScriptAudit(changeScript))
        logger.error("Change script failed to apply " + changeScript.shortName, e)

        throw new ChangeScriptFailedException(
          "Change script failed to apply " + changeScript.shortName + " [" + e.getMessage + "]", e)
    }
  }

  class ChangeLogRepository(databaseManager: MongoDatabaseManager)
    extends IdentityBasedMongoRepository[ChangeScriptAudit](databaseManager.apply("changelog"))
    with Logging {

    protected val databaseIdProperty = "name"

    override def deleteAll() {
      logger.warn("Deleting ALL changescripts from repository. I hope you knew what you were doing!")
      super.deleteAll()
    }
  }

}

