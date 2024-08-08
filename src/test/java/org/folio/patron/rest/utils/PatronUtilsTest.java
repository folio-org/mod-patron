package org.folio.patron.rest.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.patron.rest.models.User;
import org.folio.patron.rest.models.UsersCollection;
import org.folio.rest.jaxrs.model.AddressInfo;
import org.folio.rest.jaxrs.model.ContactInfo;
import org.folio.rest.jaxrs.model.ExternalPatron;
import org.folio.rest.jaxrs.model.ExternalPatronCollection;
import org.folio.rest.jaxrs.model.GeneralInfo;
import org.folio.rest.jaxrs.model.PreferredEmailCommunication;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class PatronUtilsTest {

  @Test
  void testMapUserToExternalPatron_NullUser() {
    assertNull(PatronUtils.mapUserToExternalPatron(null));
  }

  @Test
  void testMapUserToExternalPatron_ValidUser() {
    // Create User object
    User user = getUser();

    ExternalPatron result = PatronUtils.mapUserToExternalPatron(user);

    assertNotNull(result);
    assertEquals("externalId", result.getGeneralInfo().getExternalSystemId());
    assertEquals("John", result.getGeneralInfo().getFirstName());
    assertEquals("Doe", result.getGeneralInfo().getLastName());
    assertEquals("M", result.getGeneralInfo().getMiddleName());
    assertEquals("Johnny", result.getGeneralInfo().getPreferredFirstName());
    assertEquals("john.doe@example.com", result.getContactInfo().getEmail());
    assertEquals("1234567890", result.getContactInfo().getPhone());
    assertEquals("0987654321", result.getContactInfo().getMobilePhone());
    assertEquals("US", result.getAddressInfo().getCountry());
    assertEquals("123 Main St", result.getAddressInfo().getAddressLine0());
    assertEquals("Apt 4B", result.getAddressInfo().getAddressLine1());
    assertEquals("Springfield", result.getAddressInfo().getCity());
    assertEquals("IL", result.getAddressInfo().getProvince());
    assertEquals("62701", result.getAddressInfo().getZip());
    assertTrue(result.getPreferredEmailCommunication().isEmpty());
  }

  private static User getUser() {
    User user = new User();
    user.setExternalSystemId("externalId");

    User.Personal personal = new User.Personal();
    personal.setFirstName("John");
    personal.setLastName("Doe");
    personal.setMiddleName("M");
    personal.setPreferredFirstName("Johnny");
    personal.setEmail("john.doe@example.com");
    personal.setPhone("1234567890");
    personal.setMobilePhone("0987654321");

    User.Personal.Address address = new User.Personal.Address();
    address.setCountryId("US");
    address.setAddressLine1("123 Main St");
    address.setAddressLine2("Apt 4B");
    address.setCity("Springfield");
    address.setRegion("IL");
    address.setPostalCode("62701");

    personal.setAddresses(Collections.singletonList(address));
    user.setPersonal(personal);

    Set<PreferredEmailCommunication> preferredEmailCommunications = new LinkedHashSet<>();
    user.setPreferredEmailCommunication(preferredEmailCommunications);
    return user;
  }

  @Test
  void testMapExternalPatronToUser_NullExternalPatron() {
    assertNull(PatronUtils.mapExternalPatronToUser(null, "groupId", "addressTypeId"));
  }

  @Test
  void testMapExternalPatronToUser_ValidExternalPatron() {
    // Create ExternalPatron object
    ExternalPatron externalPatron = new ExternalPatron();

    GeneralInfo generalInfo = new GeneralInfo();
    generalInfo.setExternalSystemId("externalId");
    generalInfo.setFirstName("John");
    generalInfo.setLastName("Doe");
    generalInfo.setMiddleName("M");
    generalInfo.setPreferredFirstName("Johnny");
    externalPatron.setGeneralInfo(generalInfo);

    ContactInfo contactInfo = new ContactInfo();
    contactInfo.setEmail("john.doe@example.com");
    contactInfo.setPhone("1234567890");
    contactInfo.setMobilePhone("0987654321");
    externalPatron.setContactInfo(contactInfo);

    AddressInfo addressInfo = new AddressInfo();
    addressInfo.setCountry("US");
    addressInfo.setAddressLine0("123 Main St");
    addressInfo.setAddressLine1("Apt 4B");
    addressInfo.setCity("Springfield");
    addressInfo.setProvince("IL");
    addressInfo.setZip("62701");
    externalPatron.setAddressInfo(addressInfo);

    Set<PreferredEmailCommunication> preferredEmailCommunications = new LinkedHashSet<>();
    externalPatron.setPreferredEmailCommunication(preferredEmailCommunications);

    User result = PatronUtils.mapExternalPatronToUser(externalPatron, "groupId", "addressTypeId");

    assertNotNull(result);
    assertEquals("groupId", result.getPatronGroup());
    assertEquals("externalId", result.getExternalSystemId());
    assertEquals("John", result.getPersonal().getFirstName());
    assertEquals("Doe", result.getPersonal().getLastName());
    assertEquals("M", result.getPersonal().getMiddleName());
    assertEquals("Johnny", result.getPersonal().getPreferredFirstName());
    assertEquals("john.doe@example.com", result.getPersonal().getEmail());
    assertEquals("1234567890", result.getPersonal().getPhone());
    assertEquals("0987654321", result.getPersonal().getMobilePhone());

    User.Personal.Address userAddress = result.getPersonal().getAddresses().get(0);
    assertEquals("US", userAddress.getCountryId());
    assertEquals("123 Main St", userAddress.getAddressLine1());
    assertEquals("Apt 4B", userAddress.getAddressLine2());
    assertEquals("Springfield", userAddress.getCity());
    assertEquals("IL", userAddress.getRegion());
    assertEquals("62701", userAddress.getPostalCode());
    assertEquals("addressTypeId", userAddress.getAddressTypeId());
    assertTrue(userAddress.getPrimaryAddress());
  }

  @Test
  void testMapUserCollectionToExternalPatronCollection_InvalidJson() {
    assertThrows(IllegalArgumentException.class, () -> {
      PatronUtils.mapUserCollectionToExternalPatronCollection("invalidJson");
    });
  }

  @Test
  void testMapUserCollectionToExternalPatronCollection_ValidJson() throws JsonProcessingException {
    // Create UsersCollection and ExternalPatron objects
    UsersCollection usersCollection = new UsersCollection();
    List<User> userList = new ArrayList<>();
    User user = getUser();
    userList.add(user);
    usersCollection.setUsers(userList);
    usersCollection.setTotalRecords(1);


    ExternalPatron externalPatron = PatronUtils.mapUserToExternalPatron(user);
    ExternalPatronCollection expectedCollection = new ExternalPatronCollection();
    expectedCollection.setExternalPatrons(Collections.singletonList(externalPatron));
    expectedCollection.setTotalRecords(1);

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(usersCollection);

    ExternalPatronCollection result = PatronUtils.mapUserCollectionToExternalPatronCollection(json);

    assertNotNull(result);
    assertEquals(expectedCollection.getTotalRecords(), result.getTotalRecords());
    assertEquals(expectedCollection.getExternalPatrons().size(), result.getExternalPatrons().size());
    assertEquals(expectedCollection.getExternalPatrons().get(0).getGeneralInfo().getExternalSystemId(),
      result.getExternalPatrons().get(0).getGeneralInfo().getExternalSystemId());
  }
}
