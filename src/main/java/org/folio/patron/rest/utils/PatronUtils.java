package org.folio.patron.rest.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.patron.rest.models.User;
import org.folio.patron.rest.models.UsersCollection;
import org.folio.rest.jaxrs.model.AddressInfo;
import org.folio.rest.jaxrs.model.ContactInfo;
import org.folio.rest.jaxrs.model.ExternalPatron;
import org.folio.rest.jaxrs.model.ExternalPatronCollection;
import org.folio.rest.jaxrs.model.GeneralInfo;
import org.folio.rest.jaxrs.model.PreferredEmailCommunication;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PatronUtils {
  private PatronUtils() {
  }
  private static final Logger logger = LogManager.getLogger();
  private static final String USER_TYPE = "patron";
  private static final Integer TWO_YEARS = 2;
  private static final String CONTACT_TYPE_EMAIL_ID = "002";

  public static ExternalPatron mapUserToExternalPatron(User user) {
    logger.info("mapUserToExternalPatron:: Mapping user object to external patron");
    if (user == null) {
      logger.warn("mapUserToExternalPatron:: user is null");
      return null;
    }
    ExternalPatron externalPatron = new ExternalPatron();

    GeneralInfo generalInfo = new GeneralInfo();
    generalInfo.setExternalSystemId(user.getExternalSystemId());
    generalInfo.setFirstName(user.getPersonal().getFirstName());
    generalInfo.setLastName(user.getPersonal().getLastName());
    generalInfo.setMiddleName(user.getPersonal().getMiddleName());
    generalInfo.setPreferredFirstName(user.getPersonal().getPreferredFirstName());
    externalPatron.setGeneralInfo(generalInfo);

    ContactInfo contactInfo = new ContactInfo();
    contactInfo.setEmail(user.getPersonal().getEmail());
    contactInfo.setPhone(user.getPersonal().getPhone());
    contactInfo.setMobilePhone(user.getPersonal().getMobilePhone());
    externalPatron.setContactInfo(contactInfo);

    Set<PreferredEmailCommunication> preferredEmailCommunication =
      new LinkedHashSet<>(user.getPreferredEmailCommunication());
    externalPatron.setPreferredEmailCommunication(preferredEmailCommunication);

    User.Personal.Address userAddress =
      Optional.ofNullable(user.getPersonal())
        .map(User.Personal::getAddresses)
        .filter(addressList->!addressList.isEmpty())
        .map(addressList->addressList.get(0))
        .orElse(null);

    if (userAddress != null) {
          AddressInfo address = new AddressInfo();
          address.setCountry(userAddress.getCountryId());
          address.setAddressLine0(userAddress.getAddressLine1());
          address.setAddressLine1(userAddress.getAddressLine2());
          address.setCity(userAddress.getCity());
          address.setProvince(userAddress.getRegion());
          address.setZip(userAddress.getPostalCode());
          externalPatron.setAddressInfo(address);
        }
    return externalPatron;
  }

  public static User mapExternalPatronToUser(ExternalPatron externalPatron,
                                             String remotePatronGroupId, String homeAddressTypeId) {
    logger.info("mapExternalPatronToUser:: Mapping external Patron to user");
    if (externalPatron == null) {
      logger.warn("mapExternalPatronToUser:: External patron object is null");
      return null;
    }

    User user = new User();
    LocalDate currentDate = LocalDate.now();
    LocalDate expirationLocalDate = currentDate.plusYears(TWO_YEARS);
    Date expirationDate = Date.from(expirationLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    user.setExpirationDate(expirationDate);

    user.setPatronGroup(remotePatronGroupId);
    user.setPreferredEmailCommunication(externalPatron.getPreferredEmailCommunication());
    user.setExternalSystemId(externalPatron.getGeneralInfo().getExternalSystemId());
    User.Personal personal = new User.Personal();
    personal.setEmail(externalPatron.getContactInfo().getEmail());
    personal.setPhone(externalPatron.getContactInfo().getPhone());
    personal.setMobilePhone(externalPatron.getContactInfo().getMobilePhone());
    personal.setFirstName(externalPatron.getGeneralInfo().getFirstName());
    personal.setLastName(externalPatron.getGeneralInfo().getLastName());
    personal.setMiddleName(externalPatron.getGeneralInfo().getMiddleName());
    personal.setPreferredFirstName(externalPatron.getGeneralInfo().getPreferredFirstName());

    if (externalPatron.getAddressInfo() != null) {
      User.Personal.Address userAddress = new User.Personal.Address();
      userAddress.setId(UUID.randomUUID().toString());
      userAddress.setCountryId(externalPatron.getAddressInfo().getCountry());
      userAddress.setAddressLine1(externalPatron.getAddressInfo().getAddressLine0());
      userAddress.setAddressLine2(externalPatron.getAddressInfo().getAddressLine1());
      userAddress.setCity(externalPatron.getAddressInfo().getCity());
      userAddress.setRegion(externalPatron.getAddressInfo().getProvince());
      userAddress.setPostalCode(externalPatron.getAddressInfo().getZip());
      userAddress.setAddressTypeId(homeAddressTypeId);
      userAddress.setPrimaryAddress(true);

      personal.setAddresses(Collections.singletonList(userAddress));
    }

    user.setPersonal(personal);
    user.setActive(true);
    user.setEnrollmentDate(new Date());
    user.setType(USER_TYPE);
    //For contact type email
    user.getPersonal().setPreferredContactTypeId(CONTACT_TYPE_EMAIL_ID);
    return user;
  }

  public static ExternalPatronCollection mapUserCollectionToExternalPatronCollection(String json) {
    logger.info("mapUserCollectionToExternalPatronCollection::" +
      " Mapping user collection to external patron collection");
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      UsersCollection usersCollection = mapper.readValue(json, UsersCollection.class);
      List<ExternalPatron> externalPatrons = new ArrayList<>();
      for (User user : usersCollection.getUsers()){
        ExternalPatron externalPatron = PatronUtils.mapUserToExternalPatron(user);
        externalPatrons.add(externalPatron);
      }

      ExternalPatronCollection collection = new ExternalPatronCollection();
      collection.setExternalPatrons(externalPatrons);
      collection.setTotalRecords(usersCollection.getTotalRecords());
      return collection;
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
