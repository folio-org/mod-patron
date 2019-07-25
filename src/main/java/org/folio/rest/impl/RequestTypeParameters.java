package org.folio.rest.impl;

class RequestTypeParameters {

  private String itemMaterialTypeId;
  private String itemPatronGroupId;
  private String itemLoanTypeId;
  private String itemLocationId;
  private ItemStatus itemStatus;

  String getItemMaterialTypeId() {
    return itemMaterialTypeId;
  }

  void setItemMaterialTypeId(String materialTypeId) {
    this.itemMaterialTypeId = materialTypeId;
  }

  String getPatronGroupId() {
    return itemPatronGroupId;
  }

  void setPatronGroupId(String patronGroupId) {
    this.itemPatronGroupId = patronGroupId;
  }

  String getItemLoanTypeId() {
    return itemLoanTypeId;
  }

  void setItemLoanTypeId(String loanTypeId) {
    this.itemLoanTypeId = loanTypeId;
  }

  String getItemLocationId() {
    return itemLocationId;
  }

  void setItemLocationId(String locationId) {
    this.itemLocationId = locationId;
  }

  void setItemStatus(ItemStatus itemStatus) {
    this.itemStatus = itemStatus;
  }

  ItemStatus getItemStatus() {
    return this.itemStatus;
  }
}
