package org.folio.patron.rest.models;

import java.util.Date;
import java.util.List;
import java.util.Set;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.folio.rest.jaxrs.model.PreferredEmailCommunication;

@Data
public class User {

  private String username;
  private String id;
  private String externalSystemId;
  private String barcode;
  private Boolean active;
  private String type;
  @Valid
  private Set<PreferredEmailCommunication> preferredEmailCommunication;

  @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
  private String patronGroup;

  @Valid
  private Set<String> departments;

  @Valid
  private List<String> proxyFor;

  @Valid
  private Personal personal;

  private Date enrollmentDate;
  private Date expirationDate;
  private Date createdDate;
  private Date updatedDate;

  public String getUsername() {
    return username;
  }

  public Set<PreferredEmailCommunication> getPreferredEmailCommunication() {
    return preferredEmailCommunication;
  }

  public void setPreferredEmailCommunication(Set<PreferredEmailCommunication> preferredEmailCommunication) {
    this.preferredEmailCommunication = preferredEmailCommunication;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getExternalSystemId() {
    return externalSystemId;
  }

  public void setExternalSystemId(String externalSystemId) {
    this.externalSystemId = externalSystemId;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPatronGroup() {
    return patronGroup;
  }

  public void setPatronGroup(String patronGroup) {
    this.patronGroup = patronGroup;
  }

  public Set<String> getDepartments() {
    return departments;
  }

  public void setDepartments(Set<String> departments) {
    this.departments = departments;
  }

  public List<String> getProxyFor() {
    return proxyFor;
  }

  public void setProxyFor(List<String> proxyFor) {
    this.proxyFor = proxyFor;
  }

  public Personal getPersonal() {
    return personal;
  }

  public void setPersonal(Personal personal) {
    this.personal = personal;
  }

  public Date getEnrollmentDate() {
    return enrollmentDate;
  }

  public void setEnrollmentDate(Date enrollmentDate) {
    this.enrollmentDate = enrollmentDate;
  }

  public Date getExpirationDate() {
    return expirationDate;
  }

  public void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Date getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  @Data
  public static class Personal {

    private String lastName;
    private String firstName;
    private String middleName;
    private String preferredFirstName;
    private String email;
    private String phone;
    private String mobilePhone;

    public String getLastName() {
      return lastName;
    }

    public void setLastName(String lastName) {
      this.lastName = lastName;
    }

    public String getFirstName() {
      return firstName;
    }

    public void setFirstName(String firstName) {
      this.firstName = firstName;
    }

    public String getMiddleName() {
      return middleName;
    }

    public void setMiddleName(String middleName) {
      this.middleName = middleName;
    }

    public String getPreferredFirstName() {
      return preferredFirstName;
    }

    public void setPreferredFirstName(String preferredFirstName) {
      this.preferredFirstName = preferredFirstName;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getMobilePhone() {
      return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
      this.mobilePhone = mobilePhone;
    }

    public Date getDateOfBirth() {
      return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
      this.dateOfBirth = dateOfBirth;
    }

    public List<Address> getAddresses() {
      return addresses;
    }

    public void setAddresses(List<Address> addresses) {
      this.addresses = addresses;
    }

    public String getPreferredContactTypeId() {
      return preferredContactTypeId;
    }

    public void setPreferredContactTypeId(String preferredContactTypeId) {
      this.preferredContactTypeId = preferredContactTypeId;
    }

    public String getProfilePictureLink() {
      return profilePictureLink;
    }

    public void setProfilePictureLink(String profilePictureLink) {
      this.profilePictureLink = profilePictureLink;
    }

    private Date dateOfBirth;

    @Valid
    private List<Address> addresses;

    private String preferredContactTypeId;
    private String profilePictureLink;

    @Data
    public static class Address {

      private String id;
      private String countryId;
      private String addressLine1;
      private String addressLine2;
      private String city;
      private String region;
      private String postalCode;

      public String getId() {
        return id;
      }

      public void setId(String id) {
        this.id = id;
      }

      public String getCountryId() {
        return countryId;
      }

      public void setCountryId(String countryId) {
        this.countryId = countryId;
      }

      public String getAddressLine1() {
        return addressLine1;
      }

      public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
      }

      public String getAddressLine2() {
        return addressLine2;
      }

      public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
      }

      public String getCity() {
        return city;
      }

      public void setCity(String city) {
        this.city = city;
      }

      public String getRegion() {
        return region;
      }

      public void setRegion(String region) {
        this.region = region;
      }

      public String getPostalCode() {
        return postalCode;
      }

      public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
      }

      public String getAddressTypeId() {
        return addressTypeId;
      }

      public void setAddressTypeId(String addressTypeId) {
        this.addressTypeId = addressTypeId;
      }

      public Boolean getPrimaryAddress() {
        return primaryAddress;
      }

      public void setPrimaryAddress(Boolean primaryAddress) {
        this.primaryAddress = primaryAddress;
      }

      @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
      private String addressTypeId;
      private Boolean primaryAddress;
    }
  }
}
