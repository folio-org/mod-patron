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
import org.folio.rest.jaxrs.model.Hold;
import org.folio.rest.jaxrs.model.Hold.FulfillmentPreference;
import org.folio.rest.jaxrs.model.Hold.Status;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.TotalCharges;
import org.folio.rest.jaxrs.resource.PatronServicesResource;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;
import org.joda.time.DateTime;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PatronServicesResourceImpl implements PatronServicesResource {
  @Validate
  @Override
  public void getPatronAccountById(String id, boolean includeLoans,
      boolean includeCharges, boolean includeHolds,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext)
      throws Exception {
    final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
    try {
      // Look up the user to ensure that the user exists and is enabled
      httpClient.request("/users/" + id, okapiHeaders)
        .thenAccept(this::verifyUserEnabled)
        .thenCompose(v -> {
          try {
            final Account account = new Account();

            account.setTotalCharges(new TotalCharges().withAmount(0.0).withIsoCurrencyCode("USD"));
            account.setTotalChargesCount(0);

            final CompletableFuture<Account> cf1 = httpClient.request("/circulation/loans?limit=" + getLimit(includeLoans) + "&query=%28userId%3D%3D" + id + "%20and%20status.name%3D%3DOpen%29", okapiHeaders)
                .thenApply(response -> {
                  verifyExists(response);
                  return addLoans(account, response, includeLoans);
                });

            final CompletableFuture<Account> cf2 = httpClient.request("/circulation/requests?limit=" + getLimit(includeHolds) + "&query=%28requesterId%3D%3D" + id + "%20and%20requestType%3D%3DHold%20and%20status%3D%3DOpen%2A%29", okapiHeaders)
                .thenApply(response -> {
                  verifyExists(response);
                  return addHolds(account, response, includeHolds);
                });

            final CompletableFuture<Account> cf3 = httpClient.request("/accounts?limit=" + getLimit(true) + "&query=%28userId%3D%3D" + id + "%20and%20status.name%3D%3DOpen%29", okapiHeaders)
                .thenApply(response -> {
                  verifyExists(response);
                  return addCharges(account, response, includeCharges);
                }).thenCompose(charges -> {
                  if (includeCharges) {
                    List<CompletableFuture<Account>> cfs = new ArrayList<>();
                    for (Charge charge: account.getCharges()) {
                      cfs.add(lookupChargeItem(httpClient, charge, account, okapiHeaders));
                      cfs.add(lookupFeeFine(httpClient, charge, account, okapiHeaders));
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
          asyncResultHandler.handle(Future.succeededFuture(GetPatronAccountByIdResponse.withJsonOK(account)));
          httpClient.closeClient();
        })
        .exceptionally(throwable -> {
          asyncResultHandler.handle(handleError(throwable));
          httpClient.closeClient();
          return null;
        });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(GetPatronAccountByIdResponse.withPlainInternalServerError(e.getMessage())));
      httpClient.closeClient();
    }
  }

  @Validate
  @Override
  public void postPatronAccountByIdItemByItemIdRenew(String itemId, String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext)
      throws Exception {
    // TODO Implement when https://issues.folio.org/browse/CIRC-100 is complete
    asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountByIdItemByItemIdRenewResponse.withNotImplemented()));
  }

  @Validate
  @Override
  public void postPatronAccountByIdItemByItemIdHold(String itemId, String id,
      Hold entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext)
      throws Exception {
    final JsonObject holdJSON = new JsonObject()
        .put("itemId", itemId)
        .put("requesterId", id)
        .put("requestType", "Hold")
        .put("requestDate", new DateTime(entity.getRequestId()).toString())
        .put("fulfilmentPreference", entity.getFulfillmentPreference().toString())
        .put("requestExpirationDate", entity.getExpirationDate());

    final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
    try {
      httpClient.request(HttpMethod.POST, Buffer.buffer(holdJSON.toString()), "/circulation/requests", okapiHeaders)
          .thenAccept(response -> {
            verifyExists(response);
            JsonObject body = response.getBody();
            final Item item = getItem(itemId, body.getJsonObject("item"));
            final Hold hold = getHold(body, item);
            asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountByIdItemByItemIdHoldResponse.withJsonCreated(hold)));
            httpClient.closeClient();
          })
          .exceptionally(throwable -> {
            asyncResultHandler.handle(handleHoldPOSTError(throwable));
            httpClient.closeClient();
            return null;
          });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountByIdItemByItemIdHoldResponse.withPlainInternalServerError(e.getMessage())));
      httpClient.closeClient();
    }
  }

  @Validate
  @Override
  public void putPatronAccountByIdItemByItemIdHoldByHoldId(String holdId,
      String itemId, String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext)
      throws Exception {
    asyncResultHandler.handle(Future.succeededFuture(PutPatronAccountByIdItemByItemIdHoldByHoldIdResponse.withNotImplemented()));
  }

  @Validate
  @Override
  public void deletePatronAccountByIdItemByItemIdHoldByHoldId(String holdId,
      String itemId, String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext)
      throws Exception {
    // TODO: validation that the hold is for the specified user and the
    // specified item.
    final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
    try {
      httpClient.request(HttpMethod.DELETE, "/circulation/requests/" + holdId, okapiHeaders)
          .thenAccept(response -> {
            verifyExists(response);
            asyncResultHandler.handle(Future.succeededFuture(DeletePatronAccountByIdItemByItemIdHoldByHoldIdResponse.withNoContent()));
          })
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
  public void postPatronAccountByIdInstanceByInstanceIdHold(String instanceId,
      String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler,
      Context vertxContext) throws Exception {
    // TODO Implement once FOLIO can perform hold management on instances
    asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountByIdInstanceByInstanceIdHoldResponse.withNotImplemented()));
  }

  @Validate
  @Override
  public void putPatronAccountByIdInstanceByInstanceIdHoldByHoldId(
      String holdId, String instanceId, String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler,
      Context vertxContext) throws Exception {
    // TODO Implement once FOLIO can perform hold management on instances
    asyncResultHandler.handle(Future.succeededFuture(PutPatronAccountByIdInstanceByInstanceIdHoldByHoldIdResponse.withNotImplemented()));
  }

  @Validate
  @Override
  public void deletePatronAccountByIdInstanceByInstanceIdHoldByHoldId(
      String holdId, String instanceId, String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler,
      Context vertxContext) throws Exception {
    // TODO Implement once FOLIO can perform hold management on instances
    asyncResultHandler.handle(Future.succeededFuture(DeletePatronAccountByIdInstanceByInstanceIdHoldByHoldIdResponse.withNotImplemented()));
  }

  private HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    final String okapiURL;
    if (okapiHeaders.containsKey("X-Okapi-Url")) {
      okapiURL = okapiHeaders.get("X-Okapi-Url");
    } else {
      okapiURL = System.getProperty("okapi.url");
    }
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    return HttpClientFactory.getHttpClient(okapiURL, tenantId);
  }

  private void verifyExists(Response response) {
    if (!Response.isSuccess(response.getCode())) {
      throw new CompletionException(new HttpException(response.getCode(), response.getError().toString()));
    }
  }

  private void verifyUserEnabled(Response response) {
    verifyExists(response);

    final JsonObject responseBody = response.getBody();

    boolean active = responseBody.getBoolean("active");
    if (!active) {
      throw new CompletionException(new ModuleGeneratedHttpException(400, "User is not active"));
    }
  }

  private Account addLoans(Account account, Response response, boolean includeLoans) {
    final int totalLoans = response.getBody().getInteger("totalRecords", Integer.valueOf(0)).intValue();
    final List<Loan> loans = new ArrayList<>();

    account.setTotalLoans(totalLoans);
    account.setLoans(loans);

    if (totalLoans > 0 && includeLoans) {
      final JsonArray loansArray = response.getBody().getJsonArray("loans");
      for (Object o : loansArray) {
        if (o instanceof JsonObject) {
          JsonObject loanObject = (JsonObject) o;
          final Item item = getItem(loanObject.getString("itemId"), loanObject.getJsonObject("item"));
          final Loan loan = getLoan(loanObject, item);
          loans.add(loan);
        }
      }
    }

    return account;
  }

  private Item getItem(String itemId, JsonObject itemJson) {
    final JsonArray contributors = itemJson.getJsonArray("contributors", new JsonArray());
    final StringBuilder sb = new StringBuilder();

    for (Object o : contributors) {
      if (o instanceof JsonObject) {
        if (sb.length() != 0) {
          sb.append("; ");
        }
        sb.append(((JsonObject) o).getString("name"));
      }
    }

    return new Item()
        .withAuthor(sb.length() == 0 ? null : sb.toString())
        .withInstanceId(itemJson.getString("instanceId"))
        .withItemId(itemId)
        .withTitle(itemJson.getString("title"));
  }

  private Loan getLoan(JsonObject loan, Item item) {
    final Date dueDate = new DateTime(loan.getString("dueDate")).toDate();
    return new Loan()
        .withId(loan.getString("id"))
        .withItem(item)
        // This should be more sophisticated, or actually reported by
        // the circulation module. What is "overdue" can vary as some
        // libraries have a grace period, don't count holidays, etc.
        .withOverdue(new Date().after(dueDate))
        .withDueDate(dueDate)
        .withLoanDate(new DateTime(loan.getString("loanDate")).toDate());
  }

  private Account addHolds(Account account, Response response, boolean includeHolds) {
    final int totalHolds = response.getBody().getInteger("totalRecords", Integer.valueOf(0)).intValue();
    final List<Hold> holds = new ArrayList<>();

    account.setTotalHolds(totalHolds);
    account.setHolds(holds);

    if (totalHolds > 0 && includeHolds) {
      final JsonArray holdsJson = response.getBody().getJsonArray("requests");
      for (Object o : holdsJson) {
        if (o instanceof JsonObject) {
          JsonObject holdJson = (JsonObject) o;
          final Item item = getItem(holdJson.getString("itemId"), holdJson.getJsonObject("item"));
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
        .withExpirationDate(holdJson.getString("requestExpirationDate"))
        .withRequestId(holdJson.getString("id"))
        .withFulfillmentPreference(FulfillmentPreference.fromValue(holdJson.getString("fulfilmentPreference")))
        .withStatus(Status.fromValue(holdJson.getString("status")));
  }

  private Account addCharges(Account account, Response response, boolean includeCharges) {
    final int totalCharges = response.getBody().getInteger("totalRecords", Integer.valueOf(0)).intValue();
    final List<Charge> charges = new ArrayList<>();

    account.setTotalChargesCount(totalCharges);
    account.setCharges(charges);

    double amount = 0.0;

    if (totalCharges > 0) {
      final JsonArray accountsJson = response.getBody().getJsonArray("accounts");
      for (Object o : accountsJson) {
        if (o instanceof JsonObject) {
          JsonObject accountJson = (JsonObject) o;
          final Item item = new Item().withItemId(accountJson.getString("itemId"));
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
        .withAccrualDate(new DateTime(chargeJson.getString("dateCreated")).toDate())
        .withChargeAmount(new TotalCharges().withAmount(chargeJson.getDouble("remaining")).withIsoCurrencyCode("USD"))
        .withState(chargeJson.getJsonObject("paymentStatus", new JsonObject().put("name",  "Unknown")).getString("name"))
        .withFeeFineId(chargeJson.getString("feeFineId"));
  }

  private CompletableFuture<Account> lookupChargeItem(HttpClientInterface httpClient, Charge charge, Account account, Map<String, String> okapiHeaders) {
    return getChargeItem(charge, httpClient, okapiHeaders)
        .thenApply(this::verifyAndExtractBody)
        .thenCompose(chargeItem ->
          itemLookup(chargeItem, charge, account, httpClient, okapiHeaders)
        );
  }

  private CompletableFuture<Account> itemLookup(JsonObject chargeItem, Charge charge,
      Account account, HttpClientInterface httpClient,
      Map<String, String> okapiHeaders) {
    try {
      final String barcode = chargeItem.getString("barcode");

      return httpClient.request("/inventory/items?query=barcode%3D%3D" + barcode, okapiHeaders)
          .thenCompose(invResponse -> {
            if (Response.isSuccess(invResponse.getCode())) {
              if (invResponse.getBody().getInteger("totalRecords").intValue() > 0) {
                final JsonObject item = invResponse.getBody().getJsonArray("items").getJsonObject(0);
                return getHoldingsRecord(item, httpClient, okapiHeaders)
                  .thenApply(this::verifyAndExtractBody)
                  .thenCompose(holdingsRecord -> getInstance(holdingsRecord, httpClient, okapiHeaders))
                  .thenApply(this::verifyAndExtractBody)
                  .thenApply(instance -> getItem(item, instance))
                  .thenApply(itemObject -> updateChargeItem(charge, itemObject, account));
              } else {
                charge.getItem().withTitle(chargeItem.getString("instance"));
                return CompletableFuture.completedFuture(account);
              }
            } else {
              charge.getItem().withTitle(chargeItem.getString("instance"));
              return CompletableFuture.completedFuture(account);
            }
          });
    } catch (Exception e) {
      throw new CompletionException(e);
    }
  }

  private CompletableFuture<Response> getChargeItem(Charge charge,
      HttpClientInterface httpClient, Map<String, String> okapiHeaders) {
    try {
      return httpClient.request("/chargeitem/" + charge.getItem().getItemId(), okapiHeaders);
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
      return httpClient.request("/inventory/instances/" + holdingsRecord.getString("instanceId"), okapiHeaders);
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

  private Item getItem(JsonObject item, JsonObject instance) {
    final String itemId = item.getString("id");
    final JsonObject composite = new JsonObject()
        .put("contributors", instance.getJsonArray("contributors"))
        .put("instanceId", instance.getString("id"))
        .put("title", instance.getString("title"));

    return getItem(itemId, composite);
  }

  private Account updateChargeItem(Charge charge, Item item, Account account) {
    charge.withItem(item);

    return account;
  }

  private CompletableFuture<Account> lookupFeeFine(HttpClientInterface httpClient,
      Charge charge, Account account, Map<String, String> okapiHeaders) {
    return getFeeFine(charge, httpClient, okapiHeaders)
        .thenApply(this::verifyAndExtractBody)
        .thenApply(feefine -> updateChargeWithFeeFine(feefine, charge, account));
  }

  private CompletableFuture<Response> getFeeFine(Charge charge,
      HttpClientInterface httpClient, Map<String, String> okapiHeaders) {
    try {
      return httpClient.request("/feefines/" + charge.getFeeFineId(), okapiHeaders);
    } catch (Exception e) {
      throw new CompletionException(e);
    }
  }

  private Account updateChargeWithFeeFine(JsonObject feeFine, Charge charge, Account account) {
    charge.withReason(feeFine.getString("feeFineType"));

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
          result = Future.succeededFuture(GetPatronAccountByIdResponse.withPlainBadRequest(message));
        } else {
          result = Future.succeededFuture(GetPatronAccountByIdResponse.withPlainInternalServerError(message));
        }
        break;
      case 401:
        result = Future.succeededFuture(GetPatronAccountByIdResponse.withPlainUnauthorized(message));
        break;
      case 403:
        result = Future.succeededFuture(GetPatronAccountByIdResponse.withPlainForbidden(message));
        break;
      case 404:
        result = Future.succeededFuture(GetPatronAccountByIdResponse.withPlainNotFound(message));
        break;
      default:
        result = Future.succeededFuture(GetPatronAccountByIdResponse.withPlainInternalServerError(message));
      }
    } else {
      result = Future.succeededFuture(GetPatronAccountByIdResponse.withPlainInternalServerError(throwable.getMessage()));
    }

    return result;
  }

  private Future<javax.ws.rs.core.Response> handleHoldPOSTError(Throwable throwable) {
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
        result = Future.succeededFuture(PostPatronAccountByIdItemByItemIdHoldResponse.withPlainInternalServerError(message));
        break;
      case 401:
        result = Future.succeededFuture(PostPatronAccountByIdItemByItemIdHoldResponse.withPlainUnauthorized(message));
        break;
      case 403:
        result = Future.succeededFuture(PostPatronAccountByIdItemByItemIdHoldResponse.withPlainForbidden(message));
        break;
      case 404:
        result = Future.succeededFuture(PostPatronAccountByIdItemByItemIdHoldResponse.withPlainNotFound(message));
        break;
      default:
        result = Future.succeededFuture(PostPatronAccountByIdItemByItemIdHoldResponse.withPlainInternalServerError(message));
      }
    } else {
      result = Future.succeededFuture(PostPatronAccountByIdItemByItemIdHoldResponse.withPlainInternalServerError(throwable.getMessage()));
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
        result = Future.succeededFuture(DeletePatronAccountByIdItemByItemIdHoldByHoldIdResponse.withPlainInternalServerError(message));
        break;
      case 401:
        result = Future.succeededFuture(DeletePatronAccountByIdItemByItemIdHoldByHoldIdResponse.withPlainUnauthorized(message));
        break;
      case 403:
        result = Future.succeededFuture(DeletePatronAccountByIdItemByItemIdHoldByHoldIdResponse.withPlainForbidden(message));
        break;
      case 404:
        result = Future.succeededFuture(DeletePatronAccountByIdItemByItemIdHoldByHoldIdResponse.withPlainNotFound(message));
        break;
      default:
        result = Future.succeededFuture(DeletePatronAccountByIdItemByItemIdHoldByHoldIdResponse.withPlainInternalServerError(message));
      }
    } else {
      result = Future.succeededFuture(DeletePatronAccountByIdItemByItemIdHoldByHoldIdResponse.withPlainInternalServerError(throwable.getMessage()));
    }

    return result;
  }
}
