package uk.gov.gds.common.repository

import uk.gov.gds.common.logging.Logging
import scala.collection.immutable.Stream

trait Cursor[A] {
  def pageOfData: List[A]

  def pages: Int

  def currentPage: Long

  def total: Long

  def gotoNextPage(): Unit

  def hasNextPage: Boolean

  def foreach[B](f: (A) => B): Unit

  def map[B](f: (A) => B): Seq[B]

  def toList: List[A]
  
  def toStream: Stream[A]
}

abstract class CursorBase[A](var pageSize: Int,
                             var currentPage: Long)
  extends Cursor[A] with Logging {

  def pages = math.ceil(total.toDouble / pageSize.toDouble).toInt

  def gotoNextPage() = if (!hasNextPage) throw new EndOfCursorException else currentPage += 1

  def hasNextPage = (currentPage + 1) <= pages

  def foreach[B](f: (A) => B) {
    1.to(pages).foreach {
      _ =>
        pageOfData.foreach(f)
        advanceToNextPage()
    }
  }

  def map[B](f: (A) => B) = 1.to(pages).map {
    _ =>
      val results = pageOfData.map(f)
      advanceToNextPage()
      results
  }.flatten

  def toList: List[A] = map(x => x).toList
  
  def toStream: Stream[A] = stream(this)

  protected def skipSize = ((currentPage - 1) * pageSize).toInt

  protected def logAndTimeQuery[B](logMessage: String, query: => B) = {
    logger.trace(logMessage)
    val startTimeInMillis = System.currentTimeMillis()
    val queryResult = query
    logger.trace(logMessage + " completed in " + (System.currentTimeMillis() - startTimeInMillis) + "ms")
    queryResult
  }

  private def advanceToNextPage() = if (hasNextPage) gotoNextPage()
  
  private def stream[A](cursor: Cursor[A]): Stream[A] =
    cursor.pageOfData.toStream #::: (if (cursor.hasNextPage) {
      cursor.gotoNextPage
      stream(cursor)
    } else {
      Stream.empty
    })
}

class EndOfCursorException extends RuntimeException