package org.folio.rest.impl;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import org.junit.jupiter.api.Test;

class RequestTypeItemStatusWhiteListTests {

  @Test
  void canCreateHoldRequestWhenItemStatusCheckedOut() {
    assertTrue(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.CHECKED_OUT, RequestType.HOLD));
  }

  @Test
  void canCreateRecallRequestWhenItemStatusCheckedOut() {
    assertTrue(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.CHECKED_OUT, RequestType.RECALL));
  }

  @Test
  void cannotCreatePagedRequestWhenItemStatusCheckedOut() {
    assertFalse(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.CHECKED_OUT, RequestType.PAGE));
  }

  @Test
  void cannotCreateNoneRequestWhenItemStatusIsAnything() {
    assertFalse(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.CHECKED_OUT, RequestType.NONE));
  }

  @Test
  void canCreateHoldRequestWhenItemStatusOnOrder() {
    assertTrue(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.ON_ORDER, RequestType.HOLD));
  }

  @Test
  void canCreateRecallRequestWhenItemStatusOnOrder() {
    assertTrue(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.ON_ORDER, RequestType.RECALL));
  }

  @Test
  void cannotCreatePagedRequestWhenItemStatusOnOrder() {
    assertFalse(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.ON_ORDER, RequestType.PAGE));
  }

  @Test
  void canCreateHoldRequestWhenItemStatusInProcess() {
    assertTrue(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.IN_PROCESS, RequestType.HOLD));
  }

  @Test
  void canCreateRecallRequestWhenItemStatusInProcess() {
    assertTrue(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.IN_PROCESS, RequestType.RECALL));
  }

  @Test
  void cannotCreatePagedRequestWhenItemStatusInProcess() {
    assertFalse(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.IN_PROCESS, RequestType.PAGE));
  }

  @Test
  void canCreateRecallRequestWhenItemStatusPaged() {
    assertTrue(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.PAGED, RequestType.RECALL));
  }

  @Test
  void cannotCreatePagedRequestWhenItemStatusIsNone() {
    assertFalse(RequestTypeItemStatusWhiteList.canCreateRequestForItem(ItemStatus.NONE, RequestType.PAGE));
  }
}
