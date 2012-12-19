package uk.gov.gds.common.mongo.repository

import com.novus.salat.CaseClass

class QueryPart

class GdsMongoQuery

case class GdsFindQueryBuilder[A <: CaseClass](proxy: A) {

  @inline final def eq[B](methodCall: B, value: B) = {
    this
  }

}
