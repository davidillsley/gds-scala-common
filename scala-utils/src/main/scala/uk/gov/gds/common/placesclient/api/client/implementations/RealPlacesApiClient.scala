package uk.gov.gds.common.placesclient.api.client.implementations

import uk.gov.gds.common.json.JsonSerializer._
import uk.gov.gds.placesclient.api.client.PlacesHttpClient
import uk.gov.gds.placesclient.model._
import uk.gov.gds.common.placesclient.api.client.PlacesApiClient

object RealPlacesApiClient extends PlacesApiClient {

  def getAddresses(postcode: String, lineOne: Option[String]) = {
    val params = Map("postcode" -> postcode) ++ (lineOne match {
      case Some(thing) => Map("lineOne" -> thing)
      case _ => Map.empty
    })

    val response = PlacesHttpClient.get("/address", params)
    fromJson[List[Address]](response)
  }

  def addressExists(postcode: String, lineOne: Option[String]) = getAddresses(postcode, lineOne).nonEmpty

  def numberAddressesFound(postcode: String, lineOne: Option[String]) = getAddresses(postcode, lineOne).size

  def getLocalAuthority(postcode: String): Option[LocalAuthority] =
    PlacesHttpClient.getOptional("/authority", Map("postcode" -> postcode)).flatMap(fromJson[Option[LocalAuthority]](_))

  def getLocalAuthority(address: Address) =
    PlacesHttpClient.getOptional("/authority", Map("postcode" -> address.postcode)).flatMap(fromJson[Option[LocalAuthority]](_))

  def getLocalAuthorityBySnac(snac: String) =
    PlacesHttpClient.getOptional("/authority/ertp/" + snac).flatMap(fromJson[Option[LocalAuthority]](_))

  def getAuthorityByUrlSlug(urlSlug: String) =
    PlacesHttpClient.getOptional("/authority/" + urlSlug).flatMap(fromJson[Option[Authority]](_))

  def getAuthorityBySnacCode(snacCode: String) =
    PlacesHttpClient.getOptional("/authority/" + snacCode).flatMap(fromJson[Option[Authority]](_))

  def getAuthorityLicenceInformationByAuthorityAndLicence(authorityUrlSlugWithArea: String, licenceUrlSlug: String) =
    PlacesHttpClient.getOptional("/elms-licence/" + authorityUrlSlugWithArea + "/" + licenceUrlSlug).flatMap(fromJson[Option[AuthorityLicenceInformation]](_))

  def getAuthorityLicenceInformationBySnacCodeAndLegalRefNbr(snacCode: String, legalRefNbr: Int) =
    PlacesHttpClient.getOptional("/elms-licence/" + legalRefNbr + "/" + snacCode).flatMap(fromJson[Option[AuthorityLicenceInformation]](_))

  def getLicenceInformationByUrlSlugAndLegalRefNbr(urlSlug: String, legalReferenceNumber: Int) =
    PlacesHttpClient.getOptional("/elms-licence/" + urlSlug + "/" + legalReferenceNumber).flatMap(fromJson[Option[ElmsLicenceInformation]](_))

  def getLicenceInformationByLegalReferenceNumber(legalReferenceNumber: Int) =
    PlacesHttpClient.getOptional("/elms-licence/" + legalReferenceNumber).flatMap(fromJson[Option[ElmsLicenceInformation]](_))

  def getAllAuthorities() =
    PlacesHttpClient.getOptional("/authorities").flatMap(fromJson[Option[List[Authority]]](_))

  def getAuthorityLicenceInteractions(authorityUrlSlug: String) =
    PlacesHttpClient.getOptional("/authority/" + authorityUrlSlug + "/licence-interactions").flatMap(fromJson[Option[List[AuthorityLicenceInteraction]]](_))

  def getAllLicences() =
    PlacesHttpClient.getOptional("/elms-licences").flatMap(fromJson[Option[List[ElmsLicence]]](_))

  def getCompetentAuthoritiesByPostcodeAndLicenceUrlSlug(postcode: String, licenceUrlSlug: String) =
    PlacesHttpClient.getOptional("/competent-authority/" + postcode.replace(" ", "") + "/" + licenceUrlSlug).flatMap(fromJson[Option[List[AuthorityLicenceInformation]]](_))

}

