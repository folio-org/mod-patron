package org.folio.patron.rest.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.patron.rest.models.User;
import org.folio.patron.rest.models.UsersCollection;
import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.ContactInfo;
import org.folio.rest.jaxrs.model.ExternalPatron;
import org.folio.rest.jaxrs.model.ExternalPatronCollection;
import org.folio.rest.jaxrs.model.GeneralInfo;
import org.folio.rest.jaxrs.model.PreferredEmailCommunication;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PatronUtils {
  private PatronUtils() {
  }

  private static final String USER_TYPE = "patron";

  public static ExternalPatron mapToExternalPatron(User user) {
    if (user == null) {
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

    Set<PreferredEmailCommunication> preferredEmailCommunication = new LinkedHashSet<>(user.getPreferredEmailCommunication());
    externalPatron.setPreferredEmailCommunication(preferredEmailCommunication);

    User.Personal.Address userAddress = user.getPersonal().getAddress();
    if (userAddress != null) {
          Address address = new Address();
          address.setCountry(userAddress.getCountryId());
          address.setAddressLine0(userAddress.getAddressLine1());
          address.setAddressLine1(userAddress.getAddressLine2());
          address.setCity(userAddress.getCity());
          address.setProvince(userAddress.getRegion());
          address.setZip(userAddress.getPostalCode());
          externalPatron.setAddress(address);
        }
    return externalPatron;
  }

  public static User mapToUser(ExternalPatron externalPatron, String remotePatronGroupId, String homeAddressTypeId) {
    if (externalPatron == null) {
      return null;
    }

    User user = new User();
    LocalDate currentDate = LocalDate.now();
    LocalDate expirationLocalDate = currentDate.plusYears(2);
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

    if (externalPatron.getAddress() != null) {
      User.Personal.Address userAddress = new User.Personal.Address();
      userAddress.setId(UUID.randomUUID().toString());
      userAddress.setCountryId(externalPatron.getAddress().getCountry());
      userAddress.setAddressLine1(externalPatron.getAddress().getAddressLine0());
      userAddress.setAddressLine2(externalPatron.getAddress().getAddressLine1());
      userAddress.setCity(externalPatron.getAddress().getCity());
      userAddress.setRegion(externalPatron.getAddress().getProvince());
      userAddress.setPostalCode(externalPatron.getAddress().getZip());
      userAddress.setAddressTypeId(homeAddressTypeId);
      userAddress.setPrimaryAddress(false);

      personal.setAddress(userAddress);
    }

    user.setPersonal(personal);
    user.setActive(true);
    user.setEnrollmentDate(new Date());
    user.setType(USER_TYPE);
    return user;
  }

  public static ExternalPatronCollection mapToExternalPatronCollection(String json) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      UsersCollection usersCollection = mapper.readValue(json, UsersCollection.class);


      List<ExternalPatron> externalPatrons = new ArrayList<>();
      for (User user : usersCollection.getUsers()){
        ExternalPatron externalPatron = PatronUtils.mapToExternalPatron(user);
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
