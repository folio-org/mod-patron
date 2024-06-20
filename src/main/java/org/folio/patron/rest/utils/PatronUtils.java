package org.folio.patron.rest.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.patron.rest.models.User;
import org.folio.patron.rest.models.UsersCollection;
import org.folio.rest.jaxrs.model.Address0;
import org.folio.rest.jaxrs.model.Address1;
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

    List<User.Personal.Address> userAddresses = user.getPersonal().getAddresses();
    if (userAddresses != null) {
      for (int i = 0; i < userAddresses.size(); i++) {
        User.Personal.Address userAddress = userAddresses.get(i);
        if (i == 0) {
          Address0 address0 = new Address0();
          address0.setCountry(userAddress.getCountryId());
          address0.setAddressLine0(userAddress.getAddressLine1());
          address0.setAddressLine1(userAddress.getAddressLine2());
          address0.setCity(userAddress.getCity());
          address0.setProvince(userAddress.getRegion());
          address0.setZip(userAddress.getPostalCode());
          externalPatron.setAddress0(address0);
        } else if (i == 1) {
          Address1 address1 = new Address1();
          address1.setCountry(userAddress.getCountryId());
          address1.setAddressLine0(userAddress.getAddressLine1());
          address1.setAddressLine1(userAddress.getAddressLine2());
          address1.setCity(userAddress.getCity());
          address1.setProvince(userAddress.getRegion());
          address1.setZip(userAddress.getPostalCode());
          externalPatron.setAddress1(address1);
        }
      }
    }
    return externalPatron;
  }

  public static User mapToUser(ExternalPatron externalPatron, String remotePatronGroupId, String homeAddressTypeId, String workAddressTypeId) {
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
    List<User.Personal.Address> userAddresses = new ArrayList<>();

    if (externalPatron.getAddress0() != null) {
      User.Personal.Address userAddress0 = new User.Personal.Address();
      userAddress0.setId(UUID.randomUUID().toString());
      userAddress0.setCountryId(externalPatron.getAddress0().getCountry());
      userAddress0.setAddressLine1(externalPatron.getAddress0().getAddressLine0());
      userAddress0.setAddressLine2(externalPatron.getAddress0().getAddressLine1());
      userAddress0.setCity(externalPatron.getAddress0().getCity());
      userAddress0.setRegion(externalPatron.getAddress0().getProvince());
      userAddress0.setPostalCode(externalPatron.getAddress0().getZip());
      userAddress0.setAddressTypeId(homeAddressTypeId);
      userAddress0.setPrimaryAddress(true);
      userAddresses.add(userAddress0);
    }

    if (externalPatron.getAddress1() != null) {
      User.Personal.Address userAddress1 = new User.Personal.Address();
      userAddress1.setId(UUID.randomUUID().toString());
      userAddress1.setCountryId(externalPatron.getAddress1().getCountry());
      userAddress1.setAddressLine1(externalPatron.getAddress1().getAddressLine0());
      userAddress1.setAddressLine2(externalPatron.getAddress1().getAddressLine1());
      userAddress1.setCity(externalPatron.getAddress1().getCity());
      userAddress1.setRegion(externalPatron.getAddress1().getProvince());
      userAddress1.setPostalCode(externalPatron.getAddress1().getZip());
      userAddress1.setAddressTypeId(workAddressTypeId);
      userAddress1.setPrimaryAddress(false);
      userAddresses.add(userAddress1);
    }

    personal.setAddresses(userAddresses);
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
      throw new RuntimeException(e);
    }
  }
}
