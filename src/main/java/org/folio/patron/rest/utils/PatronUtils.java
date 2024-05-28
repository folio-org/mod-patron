package org.folio.patron.rest.utils;

import org.folio.patron.rest.models.User;
import org.folio.rest.jaxrs.model.ExternalPatron;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PatronUtils {
  private PatronUtils() {
  }

  private static final String USER_TYPE = "patron";

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
}
