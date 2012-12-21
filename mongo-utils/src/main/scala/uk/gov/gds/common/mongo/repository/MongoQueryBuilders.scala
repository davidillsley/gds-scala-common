package uk.gov.gds.common.mongo.repository

import com.novus.salat.CaseClass

trait MongoQueryExecutor[A] {
  def findOne: Option[A]

  def findAll: List[A]

  def getOne: A = findOne.get
}

case class GdsFindQueryBuilder[A <: CaseClass](schema: A) extends MongoQueryExecutor[A] {

  def foo[B, B1](methodCall: B, value: B1)(implicit e: B =:= B1) = {
    // // //
    this
  }

  def findOne: Option[A] = None

  def findAll: List[A] = List.empty
}
