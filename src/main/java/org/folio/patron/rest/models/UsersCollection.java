package org.folio.patron.rest.models;

import java.util.List;

public class UsersCollection {
  private List<User> users;
  private int totalRecords;

  public List<User> getUsers() {
    return users;
  }

  public void setUsers(List<User> users) {
    this.users = users;
  }

  public int getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(int totalRecords) {
    this.totalRecords = totalRecords;
  }
}
