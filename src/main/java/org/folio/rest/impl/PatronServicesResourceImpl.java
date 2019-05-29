package org.folio.rest.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.folio.patron.rest.exceptions.HttpException;
import org.folio.patron.rest.exceptions.ModuleGeneratedHttpException;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Charge;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Hold;
import org.folio.rest.jaxrs.model.Hold.Status;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.TotalCharges;
import org.folio.rest.jaxrs.resource.Patron;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PatronServicesResourceImpl implements Patron {
  private static final String JSON_FIELD_PICKUP_SERVICE_POINT_ID = "pickupServicePointId";
  private static final String JSON_FIELD_REQUEST_DATE = "requestDate";
  private static final String JSON_FIELD_NAME = "name";
  private static final String JSON_FIELD_ITEM = "item";
  private static final String JSON_FIELD_TOTAL_RECORDS = "totalRecords";
  private static final String JSON_FIELD_CONTRIBUTORS = "contributors";
  private static final String JSON_FIELD_TITLE = "title";
  private static final String JSON_FIELD_INSTANCE_ID = "instanceId";
  private static final String JSON_FIELD_USER_ID = "userId";
  private static final String JSON_FIELD_ITEM_ID = "itemId";
  private static final String JSON_FIELD_REQUEST_EXPIRATION_DATE = "requestExpirationDate";
  private static final String JSON_VALUE_HOLD_SHELF = "Hold Shelf";

  @Validate
  @Override
  public void getPatronAccountById(String id, boolean includeLoans,
      boolean includeCharges, boolean includeHolds,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {
    final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
    try {
      // Look up the user to ensure that the user exists and is enabled
      httpClient.request("/users/" + id, okapiHeaders)
        .thenApply(this::verifyAndExtractBody)
        .thenAccept(this::verifyUserEnabled)
        .thenCompose(v -> {
          try {
            final Account account = new Account();

            account.setTotalCharges(new TotalCharges().withAmount(0.0).withIsoCurrencyCode("USD"));
            account.setTotalChargesCount(0);

            final CompletableFuture<Account> cf1 = httpClient.request("/circulation/loans?limit=" + getLimit(includeLoans) + "&query=%28userId%3D%3D" + id + "%20and%20status.name%3D%3DOpen%29", okapiHeaders)
                .thenApply(this::verifyAndExtractBody)
                .thenApply(body -> addLoans(account, body, includeLoans));

            final CompletableFuture<Account> cf2 = httpClient.request("/circulation/requests?limit=" + getLimit(includeHolds) + "&query=%28requesterId%3D%3D" + id + "%20and%20requestType%3D%3DHold%20and%20status%3D%3DOpen%2A%29", okapiHeaders)
                .thenApply(this::verifyAndExtractBody)
                .thenApply(body -> addHolds(account, body, includeHolds));

            final CompletableFuture<Account> cf3 = httpClient.request("/accounts?limit=" + getLimit(true) + "&query=%28userId%3D%3D" + id + "%20and%20status.name%3D%3DOpen%29", okapiHeaders)
                .thenApply(this::verifyAndExtractBody)
                .thenApply(body -> addCharges(account, body, includeCharges))
                .thenCompose(charges -> {
                  if (includeCharges) {
                    List<CompletableFuture<Account>> cfs = new ArrayList<>();
                    for (Charge charge: account.getCharges()) {
                      cfs.add(lookupItem(httpClient, charge, account, okapiHeaders));
                    }
                    return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]))
                        .thenApply(done -> account);
                  }
                  return CompletableFuture.completedFuture(account);
                });

            return CompletableFuture.allOf(cf1, cf2, cf3)
                .thenApply(result -> account);
          } catch (Exception e) {
            throw new CompletionException(e);
          }
        })
        .thenAccept(account -> {
          asyncResultHandler.handle(Future.succeededFuture(GetPatronAccountByIdResponse.respond200WithApplicationJson(account)));
          httpClient.closeClient();
        })
        .exceptionally(throwable -> {
          asyncResultHandler.handle(handleError(throwable));
          httpClient.closeClient();
          return null;
        });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(GetPatronAccountByIdResponse.respond500WithTextPlain(e.getMessage())));
      httpClient.closeClient();
    }
  }

  @Validate
  @Override
  public void postPatronAccountItemRenewByIdAndItemId(String id, String itemId,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {
    final JsonObject renewalJSON = new JsonObject()
        .put(JSON_FIELD_ITEM_ID, itemId)
        .put(JSON_FIELD_USER_ID, id);

    final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
    try {
      httpClient.request(HttpMethod.POST, Buffer.buffer(renewalJSON.toString()), "/circulation/renew-by-id", okapiHeaders)
          .thenApply(this::verifyAndExtractBody)
          .thenAccept(body -> {
            final Item item = getItem(itemId, body.getJsonObject(JSON_FIELD_ITEM));
            final Loan hold = getLoan(body, item);
            asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond201WithApplicationJson(hold)));
            httpClient.closeClient();
          })
          .exceptionally(throwable -> {
            asyncResultHandler.handle(handleRenewPOSTError(throwable));
            httpClient.closeClient();
            return null;
          });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond500WithTextPlain(e.getMessage())));
      httpClient.closeClient();
    }
  }

  @Validate
  @Override
  public void postPatronAccountItemHoldByIdAndItemId(String id, String itemId,
      Hold entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {
    final JsonObject holdJSON = new JsonObject()
        .put(JSON_FIELD_ITEM_ID, itemId)
        .put("requesterId", id)
        .put("requestType", "Hold")
        .put(JSON_FIELD_REQUEST_DATE, new DateTime(entity.getRequestDate(), DateTimeZone.UTC).toString())
        .put("fulfilmentPreference", JSON_VALUE_HOLD_SHELF)
        .put(JSON_FIELD_PICKUP_SERVICE_POINT_ID, entity.getPickupLocationId());

    if (entity.getExpirationDate() != null) {
      holdJSON.put(JSON_FIELD_REQUEST_EXPIRATION_DATE,
          new DateTime(entity.getExpirationDate(), DateTimeZone.UTC).toString());
    }

    final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
    try {
      httpClient.request(HttpMethod.POST, Buffer.buffer(holdJSON.toString()), "/circulation/requests", okapiHeaders)
          .thenApply(this::verifyAndExtractBody)
          .thenAccept(body -> {
            final Item item = getItem(itemId, body.getJsonObject(JSON_FIELD_ITEM));
            final Hold hold = getHold(body, item);
            asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond201WithApplicationJson(hold)));
            httpClient.closeClient();
          })
          .exceptionally(throwable -> {
            asyncResultHandler.handle(handleItemHoldPOSTError(throwable));
            httpClient.closeClient();
            return null;
          });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond500WithTextPlain(e.getMessage())));
      httpClient.closeClient();
    }
  }

  @Validate
  @Override
  public void putPatronAccountItemHoldByIdAndItemIdAndHoldId(String id,
      String itemId, String holdId, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(PutPatronAccountItemHoldByIdAndItemIdAndHoldIdResponse.respond501()));
  }

  @Validate
  @Override
  public void deletePatronAccountItemHoldByIdAndItemIdAndHoldId(String id,
      String itemId, String holdId, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {
    // Consider validation to verify the hold is for the specified user and the specified item.
    final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
    try {
      httpClient.request(HttpMethod.DELETE, "/circulation/requests/" + holdId, okapiHeaders)
          .thenApply(this::verifyAndExtractBody)
          .thenAccept(body -> asyncResultHandler.handle(Future.succeededFuture(DeletePatronAccountItemHoldByIdAndItemIdAndHoldIdResponse.respond204())))
          .exceptionally(throwable -> {
            asyncResultHandler.handle(handleHoldDELETEError(throwable));
            httpClient.closeClient();
            return null;
          });
    } catch (Exception e) {
      asyncResultHandler.handle(handleHoldDELETEError(e));
      httpClient.closeClient();
    }
  }

  @Validate
  @Override
  public void postPatronAccountInstanceHoldByIdAndInstanceId(String id,
      String instanceId, Hold entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler,
      Context vertxContext) {
    final JsonObject holdJSON = new JsonObject()
        .put(JSON_FIELD_INSTANCE_ID, instanceId)
        .put("requesterId", id)
        .put(JSON_FIELD_REQUEST_DATE, new DateTime(entity.getRequestDate(), DateTimeZone.UTC).toString())
        .put(JSON_FIELD_PICKUP_SERVICE_POINT_ID, entity.getPickupLocationId());

    if (entity.getExpirationDate() != null) {
      holdJSON.put(JSON_FIELD_REQUEST_EXPIRATION_DATE,
          new DateTime(entity.getExpirationDate(), DateTimeZone.UTC).toString());
    }

    final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
    try {
      httpClient.request(HttpMethod.POST, Buffer.buffer(holdJSON.toString()),
          "/circulation/requests/instances", okapiHeaders)
          .thenApply(this::verifyAndExtractBody)
          .thenAccept(body -> {
            final Item item = getItem(body.getString(JSON_FIELD_ITEM_ID),
                body.getJsonObject(JSON_FIELD_ITEM));
            final Hold hold = getHold(body, item);
            asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond201WithApplicationJson(hold)));
            httpClient.closeClient();
          })
          .exceptionally(throwable -> {
            asyncResultHandler.handle(handleInstanceHoldPOSTError(throwable));
            httpClient.closeClient();
            return null;
          });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond500WithTextPlain(e.getMessage())));
      httpClient.closeClient();
    }
  }

  private HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault("X-Okapi-Url", "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    return HttpClientFactory.getHttpClient(okapiURL, tenantId);
  }

  private void verifyUserEnabled(JsonObject body) {
    final boolean active = body.getBoolean("active");

    if (!active) {
      throw new CompletionException(new ModuleGeneratedHttpException(400, "User is not active"));
    }
  }

  private Account addLoans(Account account, JsonObject body, boolean includeLoans) {
    final int totalLoans = body.getInteger(JSON_FIELD_TOTAL_RECORDS, Integer.valueOf(0)).intValue();
    final List<Loan> loans = new ArrayList<>();

    account.setTotalLoans(totalLoans);
    account.setLoans(loans);

    if (totalLoans > 0 && includeLoans) {
      final JsonArray loansArray = body.getJsonArray("loans");
      for (Object o : loansArray) {
        if (o instanceof JsonObject) {
          JsonObject loanObject = (JsonObject) o;
          final Item item = getItem(loanObject.getString(JSON_FIELD_ITEM_ID), loanObject.getJsonObject(JSON_FIELD_ITEM));
          final Loan loan = getLoan(loanObject, item);
          loans.add(loan);
        }
      }
    }

    return account;
  }

  private Item getItem(String itemId, JsonObject itemJson) {
    final JsonArray contributors = itemJson.getJsonArray(JSON_FIELD_CONTRIBUTORS, new JsonArray());
    final StringBuilder sb = new StringBuilder();

    for (Object o : contributors) {
      if (o instanceof JsonObject) {
        if (sb.length() != 0) {
          sb.append("; ");
        }
        sb.append(((JsonObject) o).getString(JSON_FIELD_NAME));
      }
    }

    return new Item()
        .withAuthor(sb.length() == 0 ? null : sb.toString())
        .withInstanceId(itemJson.getString(JSON_FIELD_INSTANCE_ID))
        .withItemId(itemId)
        .withTitle(itemJson.getString(JSON_FIELD_TITLE));
  }

  private Loan getLoan(JsonObject loan, Item item) {
    final String dueDateString = loan.getString("dueDate");
    final boolean overdue;
    final Date dueDate;

    if (dueDateString == null) {
      dueDate = null;
      overdue = false;
    } else {
      // This should be more sophisticated, or actually reported by
      // the circulation module. What is "overdue" can vary as some
      // libraries have a grace period, don't count holidays, etc.
      final DateTime dueDateTime = new DateTime(dueDateString, DateTimeZone.UTC); 
      dueDate = dueDateTime.toDate();
      overdue = dueDateTime.isBeforeNow();
    }

    return new Loan()
        .withId(loan.getString("id"))
        .withItem(item)
        .withOverdue(overdue)
        .withDueDate(dueDate)
        .withLoanDate(new DateTime(loan.getString("loanDate"), DateTimeZone.UTC).toDate());
  }

  private Account addHolds(Account account, JsonObject body, boolean includeHolds) {
    final int totalHolds = body.getInteger(JSON_FIELD_TOTAL_RECORDS, Integer.valueOf(0)).intValue();
    final List<Hold> holds = new ArrayList<>();

    account.setTotalHolds(totalHolds);
    account.setHolds(holds);

    if (totalHolds > 0 && includeHolds) {
      final JsonArray holdsJson = body.getJsonArray("requests");
      for (Object o : holdsJson) {
        if (o instanceof JsonObject) {
          JsonObject holdJson = (JsonObject) o;
          final Item item = getItem(holdJson.getString(JSON_FIELD_ITEM_ID), holdJson.getJsonObject(JSON_FIELD_ITEM));
          final Hold hold = getHold(holdJson, item);
          holds.add(hold);
        }
      }
    }

    return account;
  }

  private Hold getHold(JsonObject holdJson, Item item) {
    return new Hold()
        .withItem(item)
        .withExpirationDate(holdJson.getString(JSON_FIELD_REQUEST_EXPIRATION_DATE) == null ? null : new DateTime(holdJson.getString(JSON_FIELD_REQUEST_EXPIRATION_DATE), DateTimeZone.UTC).toDate())
        .withRequestId(holdJson.getString("id"))
        .withPickupLocationId(holdJson.getString(JSON_FIELD_PICKUP_SERVICE_POINT_ID))
        .withRequestDate(new DateTime(holdJson.getString(JSON_FIELD_REQUEST_DATE), DateTimeZone.UTC).toDate())
        .withStatus(Status.fromValue(holdJson.getString("status")));
  }

  private Account addCharges(Account account, JsonObject body, boolean includeCharges) {
    final int totalCharges = body.getInteger(JSON_FIELD_TOTAL_RECORDS, Integer.valueOf(0)).intValue();
    final List<Charge> charges = new ArrayList<>();

    account.setTotalChargesCount(totalCharges);
    account.setCharges(charges);

    double amount = 0.0;

    if (totalCharges > 0) {
      final JsonArray accountsJson = body.getJsonArray("accounts");
      for (Object o : accountsJson) {
        if (o instanceof JsonObject) {
          final JsonObject accountJson = (JsonObject) o;
          final Item item = new Item().withItemId(accountJson.getString(JSON_FIELD_ITEM_ID));
          final Charge charge = getCharge(accountJson, item);
          amount += charge.getChargeAmount().getAmount().doubleValue();
          if (includeCharges) {
            charges.add(charge);
          }
        }
      }
    }

    account.setTotalCharges(new TotalCharges()
        .withAmount(amount)
        .withIsoCurrencyCode("USD"));

    return account;
  }

  private Charge getCharge(JsonObject chargeJson, Item item) {
    return new Charge()
        .withItem(item)
        .withAccrualDate(new DateTime(chargeJson.getString("dateCreated"), DateTimeZone.UTC).toDate())
        .withChargeAmount(new TotalCharges().withAmount(chargeJson.getDouble("remaining")).withIsoCurrencyCode("USD"))
        .withState(chargeJson.getJsonObject("paymentStatus", new JsonObject().put(JSON_FIELD_NAME,  "Unknown")).getString(JSON_FIELD_NAME))
        .withReason(chargeJson.getString("feeFineType"));
  }

  private CompletableFuture<Account> lookupItem(HttpClientInterface httpClient, Charge charge, Account account, Map<String, String> okapiHeaders) {
    return getItem(charge, httpClient, okapiHeaders)
        .thenApply(this::verifyAndExtractBody)
        .thenCompose(item ->getHoldingsRecord(item, httpClient, okapiHeaders))
        .thenApply(this::verifyAndExtractBody)
        .thenCompose(holding -> getInstance(holding, httpClient, okapiHeaders))
        .thenApply(this::verifyAndExtractBody)
        .thenApply(instance -> getItem(charge, instance))
        .thenApply(item -> updateItem(charge, item, account));
  }

  private CompletableFuture<Response> getItem(Charge charge,
      HttpClientInterface httpClient, Map<String, String> okapiHeaders) {
    try {
      return httpClient.request("/inventory/items/" + charge.getItem().getItemId(), okapiHeaders);
    } catch (Exception e) {
      throw new CompletionException(e);
    }
  }

  private CompletableFuture<Response> getHoldingsRecord(JsonObject item,
      HttpClientInterface httpClient, Map<String, String> okapiHeaders) {
    try {
      return httpClient.request("/holdings-storage/holdings/" + item.getString("holdingsRecordId"), okapiHeaders);
    } catch (Exception e) {
      throw new CompletionException(e);
    }
  }

  private CompletableFuture<Response> getInstance(JsonObject holdingsRecord,
      HttpClientInterface httpClient, Map<String, String> okapiHeaders) {
    try {
      return httpClient.request("/inventory/instances/" + holdingsRecord.getString(JSON_FIELD_INSTANCE_ID), okapiHeaders);
    } catch (Exception e) {
      throw new CompletionException(e);
    }
  }

  private JsonObject verifyAndExtractBody(Response response) {
    if (!Response.isSuccess(response.getCode())) {
      throw new CompletionException(new HttpException(response.getCode(), response.getError().toString()));
    }

    return response.getBody();
  }

  private Item getItem(Charge charge, JsonObject instance) {
    final String itemId = charge.getItem().getItemId();
    final JsonObject composite = new JsonObject()
        .put(JSON_FIELD_CONTRIBUTORS, instance.getJsonArray(JSON_FIELD_CONTRIBUTORS))
        .put(JSON_FIELD_INSTANCE_ID, instance.getString("id"))
        .put(JSON_FIELD_TITLE, instance.getString(JSON_FIELD_TITLE));

    return getItem(itemId, composite);
  }

  private Account updateItem(Charge charge, Item item, Account account) {
    charge.withItem(item);

    return account;
  }

  private int getLimit(boolean includeItem) {
    final int limit;

    if (includeItem) {
      limit = Integer.MAX_VALUE;
    } else {
      limit = 1; // until RMB-96 is implemented, then 0
    }

    return limit;
  }

  private Future<javax.ws.rs.core.Response> handleError(Throwable throwable) {
    final Future<javax.ws.rs.core.Response> result;

    final Throwable t = throwable.getCause();
    if (t instanceof HttpException) {
      final int code = ((HttpException) t).getCode();
      final String message = ((HttpException) t).getMessage();
      switch (code) {
      case 400:
        // This means that we screwed up something in the request to another
        // module. This API only takes a UUID, so a client side 400 is not
        // possible here, only server side, which the client won't be able to
        // do anything about. If the error is module generated, we can do
        // something about it or at least tell the user something.
        if (t instanceof ModuleGeneratedHttpException) {
          result = Future.succeededFuture(GetPatronAccountByIdResponse.respond400WithTextPlain(message));
        } else {
          result = Future.succeededFuture(GetPatronAccountByIdResponse.respond500WithTextPlain(message));
        }
        break;
      case 401:
        result = Future.succeededFuture(GetPatronAccountByIdResponse.respond401WithTextPlain(message));
        break;
      case 403:
        result = Future.succeededFuture(GetPatronAccountByIdResponse.respond403WithTextPlain(message));
        break;
      case 404:
        result = Future.succeededFuture(GetPatronAccountByIdResponse.respond404WithTextPlain(message));
        break;
      default:
        result = Future.succeededFuture(GetPatronAccountByIdResponse.respond500WithTextPlain(message));
      }
    } else {
      result = Future.succeededFuture(GetPatronAccountByIdResponse.respond500WithTextPlain(throwable.getMessage()));
    }

    return result;
  }

  private Future<javax.ws.rs.core.Response> handleItemHoldPOSTError(Throwable throwable) {
    final Future<javax.ws.rs.core.Response> result;

    final Throwable t = throwable.getCause();
    if (t instanceof HttpException) {
      final int code = ((HttpException) t).getCode();
      final String message = ((HttpException) t).getMessage();
      switch (code) {
      case 400:
        // This means that we screwed up something in the request to another
        // module. This API only takes a UUID, so a client side 400 is not
        // possible here, only server side, which the client won't be able to
        // do anything about.
        result = Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond500WithTextPlain(message));
        break;
      case 401:
        result = Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond401WithTextPlain(message));
        break;
      case 403:
        result = Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond403WithTextPlain(message));
        break;
      case 404:
        result = Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond404WithTextPlain(message));
        break;
      default:
        result = Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond500WithTextPlain(message));
      }
    } else {
      result = Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond500WithTextPlain(throwable.getMessage()));
    }

    return result;
  }

  private Future<javax.ws.rs.core.Response> handleInstanceHoldPOSTError(Throwable throwable) {
    final Future<javax.ws.rs.core.Response> result;

    final Throwable t = throwable.getCause();
    if (t instanceof HttpException) {
      final int code = ((HttpException) t).getCode();
      final String message = ((HttpException) t).getMessage();
      switch (code) {
      case 400:
        // This means that we screwed up something in the request to another
        // module. This API only takes a UUID, so a client side 400 is not
        // possible here, only server side, which the client won't be able to
        // do anything about.
        result = Future.succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond500WithTextPlain(message));
        break;
      case 401:
        result = Future.succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond401WithTextPlain(message));
        break;
      case 403:
        result = Future.succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond403WithTextPlain(message));
        break;
      case 404:
        result = Future.succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond404WithTextPlain(message));
        break;
      default:
        result = Future.succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond500WithTextPlain(message));
      }
    } else {
      result = Future.succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond500WithTextPlain(throwable.getMessage()));
    }

    return result;
  }

  private Future<javax.ws.rs.core.Response> handleRenewPOSTError(Throwable throwable) {
    final Future<javax.ws.rs.core.Response> result;

    final Throwable t = throwable.getCause();
    if (t instanceof HttpException) {
      final int code = ((HttpException) t).getCode();
      final String message = ((HttpException) t).getMessage();
      switch (code) {
      case 400:
        // This means that we screwed up something in the request to another
        // module. This API takes UUIDs, so a client side 400 is not
        // possible here, only server side, which the client won't be able to
        // do anything about.
        result = Future.succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond500WithTextPlain(message));
        break;
      case 401:
        result = Future.succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond401WithTextPlain(message));
        break;
      case 403:
        result = Future.succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond403WithTextPlain(message));
        break;
      case 404:
        result = Future.succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond404WithTextPlain(message));
        break;
      case 422:
        final JsonObject response = new JsonObject(message);
        final Errors errors = Json.decodeValue(response.getString("errorMessage"), Errors.class);
        result = Future.succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond422WithApplicationJson(errors));
        break;
      default:
        result = Future.succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond500WithTextPlain(message));
      }
    } else {
      result = Future.succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond500WithTextPlain(throwable.getMessage()));
    }

    return result;
  }

  private Future<javax.ws.rs.core.Response> handleHoldDELETEError(Throwable throwable) {
    final Future<javax.ws.rs.core.Response> result;

    final Throwable t = throwable.getCause();
    if (t instanceof HttpException) {
      final int code = ((HttpException) t).getCode();
      final String message = ((HttpException) t).getMessage();
      switch (code) {
      case 400:
        // This means that we screwed up something in the request to another
        // module. This API only takes a UUID, so a client side 400 is not
        // possible here, only server side, which the client won't be able to
        // do anything about.
        result = Future.succeededFuture(DeletePatronAccountItemHoldByIdAndItemIdAndHoldIdResponse.respond500WithTextPlain(message));
        break;
      case 401:
        result = Future.succeededFuture(DeletePatronAccountItemHoldByIdAndItemIdAndHoldIdResponse.respond401WithTextPlain(message));
        break;
      case 403:
        result = Future.succeededFuture(DeletePatronAccountItemHoldByIdAndItemIdAndHoldIdResponse.respond403WithTextPlain(message));
        break;
      case 404:
        result = Future.succeededFuture(DeletePatronAccountItemHoldByIdAndItemIdAndHoldIdResponse.respond404WithTextPlain(message));
        break;
      default:
        result = Future.succeededFuture(DeletePatronAccountItemHoldByIdAndItemIdAndHoldIdResponse.respond500WithTextPlain(message));
      }
    } else {
      result = Future.succeededFuture(DeletePatronAccountItemHoldByIdAndItemIdAndHoldIdResponse.respond500WithTextPlain(throwable.getMessage()));
    }

    return result;
  }
}
