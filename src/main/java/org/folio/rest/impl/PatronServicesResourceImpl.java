package org.folio.rest.impl;

import static org.folio.rest.impl.HoldHelpers.addCancellationFieldsToRequest;
import static org.folio.rest.impl.HoldHelpers.constructNewHoldWithCancellationFields;
import static org.folio.rest.impl.HoldHelpers.getHold;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.HttpException;
import org.folio.patron.rest.exceptions.ModuleGeneratedHttpException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Charge;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Hold;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TotalCharges;
import org.folio.rest.jaxrs.resource.Patron;
import org.folio.util.StringUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PatronServicesResourceImpl implements Patron {

  @Validate
  @Override
  public void getPatronAccountById(String id, boolean includeLoans,
      boolean includeCharges, boolean includeHolds,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {

    try {
      // Look up the user to ensure that the user exists and is enabled
        LookupsUtils.getUser(id, okapiHeaders)
        .thenAccept(this::verifyUserEnabled)
        .thenCompose(v -> {
          try {
            final Account account = new Account();

            account.setTotalCharges(new TotalCharges().withAmount(0.0).withIsoCurrencyCode("USD"));
            account.setTotalChargesCount(0);

            final CompletableFuture<Account> cf1 = getLoans(id, includeLoans, okapiHeaders)
                .thenApply(body -> addLoans(account, body, includeLoans));

            final CompletableFuture<Account> cf2 = getRequests(id, includeHolds, okapiHeaders)
                .thenApply(body -> addHolds(account, body, includeHolds));

            final CompletableFuture<Account> cf3 = getAccounts(id, okapiHeaders)
                .thenApply(body -> addCharges(account, body, includeCharges))
                .thenCompose(charges -> {
                  if (includeCharges) {
                    List<CompletableFuture<Account>> cfs = new ArrayList<>();

                    for (Charge charge: account.getCharges()) {
                      if (charge.getItem() != null) {
                        cfs.add(lookupItem(charge, account, okapiHeaders));
                      }
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
        })
        .exceptionally(throwable -> {
          asyncResultHandler.handle(handleError(throwable));
          return null;
        });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(GetPatronAccountByIdResponse.respond500WithTextPlain(e.getMessage())));
    }
  }

  private CompletableFuture<JsonObject> getAccounts(String id, Map<String, String> okapiHeaders) {
    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    final var queryParameters = Map.of(
      "limit", String.valueOf(getLimit(true)),
      "query", String.format("(userId==%s and status.name==Open)", id));

    return client.get("/accounts", queryParameters, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  private CompletableFuture<JsonObject> getRequests(String id, boolean includeHolds,
    Map<String, String> okapiHeaders) {

    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    final var queryParameters = Map.of(
      "limit", String.valueOf(getLimit(includeHolds)),
      "query", String.format("(requesterId==%s and status==Open*)", id));

    return client.get("/circulation/requests", queryParameters, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  private CompletableFuture<JsonObject> getLoans(String id, boolean includeLoans,
    Map<String, String> okapiHeaders) {

    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    final var queryParameters = Map.of(
      "limit", String.valueOf(getLimit(includeLoans)),
      "query", String.format("(userId==%s and status.name==Open)", id));

    return client.get("/circulation/loans", queryParameters, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  @Validate
  @Override
  public void postPatronAccountItemRenewByIdAndItemId(String id, String itemId,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {

    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    final JsonObject renewalJSON = new JsonObject()
        .put(Constants.JSON_FIELD_ITEM_ID, itemId)
        .put(Constants.JSON_FIELD_USER_ID, id);

    try {
      client.post("/circulation/renew-by-id", renewalJSON, okapiHeaders)
          .thenApply(LookupsUtils::verifyAndExtractBody)
          .thenAccept(body -> {
            final Item item = getItem(itemId, body.getJsonObject(Constants.JSON_FIELD_ITEM));
            final Loan hold = getLoan(body, item);
            asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond201WithApplicationJson(hold)));
          })
          .exceptionally(throwable -> {
            asyncResultHandler.handle(handleRenewPOSTError(throwable));
            return null;
          });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond500WithTextPlain(e.getMessage())));
    }
  }

  @Validate
  @Override
  public void postPatronAccountItemHoldByIdAndItemId(String id, String itemId,
      Hold entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {

    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    RequestObjectFactory requestFactory = new RequestObjectFactory(okapiHeaders);

    requestFactory.createRequestByItem(id, itemId, entity)
      .thenCompose(holdJSON -> {
        try {
          if (holdJSON == null) {
            final Errors errors = new Errors()
                          .withErrors(Collections.singletonList(
                              new Error().withMessage("Cannot find a valid request type for this item")
                                         .withParameters(Collections.singletonList(
                                            new Parameter().withKey("itemId")
                                                           .withValue(itemId)
                                         ))
                          ));

            asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond422WithApplicationJson(errors)));
            return null;
          }

          return client.post("/circulation/requests", holdJSON, okapiHeaders)
            .thenApply(LookupsUtils::verifyAndExtractBody)
            .thenAccept(body -> {
              final Item item = getItem(itemId, body.getJsonObject(Constants.JSON_FIELD_ITEM));
              final Hold hold = getHold(body, item);
              asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond201WithApplicationJson(hold)));
            })
            .exceptionally(throwable -> {
              asyncResultHandler.handle(handleItemHoldPOSTError(throwable));
              return null;
            });
        } catch (Exception e) {
          asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond500WithTextPlain(e.getMessage())));
          return null;
        }
      });
  }

  @Validate
  @Override
  public void postPatronAccountHoldCancelByIdAndHoldId(String id, String holdId, Hold entity, Map<String, String> okapiHeaders, Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {
    final Hold[] holds = new Hold[1];

    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    try {
      client.get("/circulation/requests/" + holdId, Map.of(), okapiHeaders)
        .thenApply(LookupsUtils::verifyAndExtractBody)
        .thenApply( body -> {
          JsonObject itemJson = body.getJsonObject(Constants.JSON_FIELD_ITEM);
          final Item item = getItem(body.getString(Constants.JSON_FIELD_ITEM_ID), itemJson);
          final Hold hold = getHold(body, item);
          holds[0] = constructNewHoldWithCancellationFields(hold, entity);
          return addCancellationFieldsToRequest(body, entity);
        })
        .thenCompose( anUpdatedRequest -> {
          try {
            return client.put("/circulation/requests/" + holdId, anUpdatedRequest, okapiHeaders);
          } catch (Exception e) {
              asyncResultHandler.handle(handleHoldCancelPOSTError(e));
              return null;
            }
        })
        .thenApply(LookupsUtils::verifyAndExtractBody)
        .thenAccept(
            body -> asyncResultHandler.handle(Future.succeededFuture(
              PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond200WithApplicationJson(holds[0]))))
        .exceptionally(throwable -> {
            asyncResultHandler.handle(handleHoldCancelPOSTError(throwable));
            return null;
        });
    } catch (Exception e) {
      asyncResultHandler.handle(handleHoldCancelPOSTError(e));
    }
  }

  @Validate
  @Override
  public void postPatronAccountInstanceHoldByIdAndInstanceId(String id,
      String instanceId, Hold entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler,
      Context vertxContext) {

    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    final JsonObject holdJSON = new JsonObject()
        .put(Constants.JSON_FIELD_INSTANCE_ID, instanceId)
        .put("requesterId", id)
        .put(Constants.JSON_FIELD_REQUEST_DATE, new DateTime(entity.getRequestDate(), DateTimeZone.UTC).toString())
        .put(Constants.JSON_FIELD_PICKUP_SERVICE_POINT_ID, entity.getPickupLocationId())
        .put(Constants.JSON_FIELD_PATRON_COMMENTS, entity.getPatronComments());

    if (entity.getExpirationDate() != null) {
      holdJSON.put(Constants.JSON_FIELD_REQUEST_EXPIRATION_DATE,
          new DateTime(entity.getExpirationDate(), DateTimeZone.UTC).toString());
    }

    try {
      client.post("/circulation/requests/instances", holdJSON, okapiHeaders)
          .thenApply(LookupsUtils::verifyAndExtractBody)
          .thenAccept(body -> {
            final Item item = getItem(body.getString(Constants.JSON_FIELD_ITEM_ID),
                body.getJsonObject(Constants.JSON_FIELD_ITEM));
            final Hold hold = getHold(body, item);
            asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond201WithApplicationJson(hold)));
          })
          .exceptionally(throwable -> {
            asyncResultHandler.handle(handleInstanceHoldPOSTError(throwable));
            return null;
          });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond500WithTextPlain(e.getMessage())));
    }
  }

  private void verifyUserEnabled(JsonObject body) {
    final boolean active = body.getBoolean("active");

    if (!active) {
      throw new CompletionException(new ModuleGeneratedHttpException(400, "User is not active"));
    }
  }

  private Account addLoans(Account account, JsonObject body, boolean includeLoans) {
    final int totalLoans = body.getInteger(Constants.JSON_FIELD_TOTAL_RECORDS, Integer.valueOf(0)).intValue();
    final List<Loan> loans = new ArrayList<>();

    account.setTotalLoans(totalLoans);
    account.setLoans(loans);

    if (totalLoans > 0 && includeLoans) {
      final JsonArray loansArray = body.getJsonArray("loans");
      for (Object o : loansArray) {
        if (o instanceof JsonObject) {
          JsonObject loanObject = (JsonObject) o;
          final Item item = getItem(loanObject.getString(Constants.JSON_FIELD_ITEM_ID),
                                    loanObject.getJsonObject(Constants.JSON_FIELD_ITEM));
          final Loan loan = getLoan(loanObject, item);
          loans.add(loan);
        }
      }
    }

    return account;
  }

  private Item getItem(String itemId, JsonObject itemJson) {
    final JsonArray contributors = itemJson.getJsonArray(Constants.JSON_FIELD_CONTRIBUTORS, new JsonArray());
    final StringBuilder sb = new StringBuilder();

    for (Object o : contributors) {
      if (o instanceof JsonObject) {
        if (sb.length() != 0) {
          sb.append("; ");
        }
        sb.append(((JsonObject) o).getString(Constants.JSON_FIELD_NAME));
      }
    }

    return new Item()
        .withAuthor(sb.length() == 0 ? null : sb.toString())
        .withInstanceId(itemJson.getString(Constants.JSON_FIELD_INSTANCE_ID))
        .withItemId(itemId)
        .withTitle(itemJson.getString(Constants.JSON_FIELD_TITLE));
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
    final int totalHolds = body.getInteger(Constants.JSON_FIELD_TOTAL_RECORDS, Integer.valueOf(0)).intValue();
    final List<Hold> holds = new ArrayList<>();

    account.setTotalHolds(totalHolds);
    account.setHolds(holds);

    if (totalHolds > 0 && includeHolds) {
      final JsonArray holdsJson = body.getJsonArray("requests");
      for (Object o : holdsJson) {
        if (o instanceof JsonObject) {
          JsonObject holdJson = (JsonObject) o;
          final Item item = getItem(holdJson.getString(Constants.JSON_FIELD_ITEM_ID), holdJson.getJsonObject(Constants.JSON_FIELD_ITEM));
          final Hold hold = getHold(holdJson, item);
          holds.add(hold);
        }
      }
    }

    return account;
  }

  private Account addCharges(Account account, JsonObject body, boolean includeCharges) {
    final int totalCharges = body.getInteger(Constants.JSON_FIELD_TOTAL_RECORDS, Integer.valueOf(0)).intValue();
    final List<Charge> charges = new ArrayList<>();
    account.setTotalChargesCount(totalCharges);
    account.setCharges(charges);

    double amount = 0.0;

    if (totalCharges > 0) {
      final JsonArray accountsJson = body.getJsonArray("accounts");
      for (Object o : accountsJson) {
        if (o instanceof JsonObject) {
          final JsonObject accountJson = (JsonObject) o;
          Charge charge = getCharge(accountJson);
          if (accountJson.getString(Constants.JSON_FIELD_ITEM_ID) != null) {
            final Item item = new Item().withItemId(accountJson.getString(Constants.JSON_FIELD_ITEM_ID));
            charge.setItem(item);
          }
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

  private Charge getCharge(JsonObject chargeJson) {
    return new Charge()
        .withAccrualDate(new DateTime(chargeJson.getJsonObject("metadata").getString("createdDate"), DateTimeZone.UTC).toDate())
        .withChargeAmount(new TotalCharges().withAmount(chargeJson.getDouble("remaining")).withIsoCurrencyCode("USD"))
        .withState(chargeJson.getJsonObject("paymentStatus",
            new JsonObject().put(Constants.JSON_FIELD_NAME,  "Unknown"))
                  .getString(Constants.JSON_FIELD_NAME))
        .withReason(chargeJson.getString("feeFineType"));
  }

  private CompletableFuture<Account> lookupItem(Charge charge, Account account, Map<String, String> okapiHeaders) {
    return getItem(charge, okapiHeaders)
        .thenCompose(item -> getInstance(item, okapiHeaders))
        .thenApply(instance -> getItem(charge, instance))
        .thenApply(item -> updateItem(charge, item, account));
  }

  private CompletableFuture<JsonObject> getItem(Charge charge, Map<String, String> okapiHeaders) {
    return LookupsUtils.getItem(charge.getItem().getItemId(), okapiHeaders);
  }

  private CompletableFuture<JsonObject> getInstance(
    JsonObject item, Map<String, String> okapiHeaders) {

    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    try {
      String cql = "holdingsRecords.id==" +
          StringUtil.cqlEncode(item.getString(Constants.JSON_FIELD_HOLDINGS_RECORD_ID));

      return client.get("/inventory/instances", Map.of("query", cql), okapiHeaders)
          .thenApply(LookupsUtils::verifyAndExtractBody)
          .thenApply(instances -> instances.getJsonArray("instances").getJsonObject(0));
    } catch (Exception e) {
      throw new CompletionException(e);
    }
  }

  private Item getItem(Charge charge, JsonObject instance) {
    final String itemId = charge.getItem().getItemId();
    final JsonObject composite = new JsonObject()
        .put(Constants.JSON_FIELD_CONTRIBUTORS, instance.getJsonArray(Constants.JSON_FIELD_CONTRIBUTORS))
        .put(Constants.JSON_FIELD_INSTANCE_ID, instance.getString("id"))
        .put(Constants.JSON_FIELD_TITLE, instance.getString(Constants.JSON_FIELD_TITLE));

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
      case 422:
        final Errors errors = Json.decodeValue(message, Errors.class);
        result = Future.succeededFuture(PostPatronAccountItemHoldByIdAndItemIdResponse.respond422WithApplicationJson(errors));
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
      case 422:
        final Errors errors = Json.decodeValue(message, Errors.class);
        result = Future.succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond422WithApplicationJson(errors));
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
        final Errors errors = Json.decodeValue(message, Errors.class);
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

  private Future<javax.ws.rs.core.Response> handleHoldCancelPOSTError(Throwable throwable) {
    final Future<javax.ws.rs.core.Response> result;

    final Throwable t = throwable.getCause();
    if (t instanceof HttpException) {
      final int code = ((HttpException) t).getCode();
      final String message = ((HttpException) t).getMessage();
      switch (code) {
      case 400:
        result = Future.succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond400WithTextPlain(message));
        break;
      case 401:
        result = Future.succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond401WithTextPlain(message));
        break;
      case 403:
        result = Future.succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond403WithTextPlain(message));
        break;
      case 404:
        result = Future.succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond404WithTextPlain(message));
        break;
      case 422:
        final Errors errors = Json.decodeValue(message, Errors.class);
        result = Future.succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond422WithApplicationJson(errors));
        break;
      default:
        result = Future.succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond500WithTextPlain(message));
      }
    } else {
      result = Future.succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond500WithTextPlain(throwable.getMessage()));
    }

    return result;
  }
}
