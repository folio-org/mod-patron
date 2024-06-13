package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.patron.rest.utils.PatronUtils.mapToExternalPatron;
import static org.folio.rest.impl.Constants.*;
import static org.folio.rest.impl.HoldHelpers.constructNewHoldWithCancellationFields;
import static org.folio.rest.impl.HoldHelpers.createCancelRequest;
import static org.folio.rest.impl.HoldHelpers.getHold;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond200WithApplicationJson;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond201WithApplicationJson;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond401WithTextPlain;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond403WithTextPlain;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond404WithTextPlain;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond422WithApplicationJson;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond500WithTextPlain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.folio.integration.http.HttpClientFactory;
import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.HttpException;
import org.folio.patron.rest.exceptions.ModuleGeneratedHttpException;
import org.folio.patron.rest.exceptions.ValidationException;
import org.folio.patron.rest.models.User;
import org.folio.patron.rest.utils.PatronUtils;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.AllowedServicePoint;
import org.folio.rest.jaxrs.model.AllowedServicePoints;
import org.folio.rest.jaxrs.model.Charge;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.ExternalPatron;
import org.folio.rest.jaxrs.model.Hold;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TotalCharges;
import org.folio.rest.jaxrs.resource.Patron;
import org.folio.util.StringUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.common.collect.Maps;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PatronServicesResourceImpl implements Patron {
  private static final String HOME_ADDRESS_TYPE = "home";
  private static final String WORK_ADDRESS_TYPE = "work";
  private static final String TOTAL_RECORDS = "totalRecords";
  private static final String QUERY = "query";
  private static final String CIRCULATION_REQUESTS = "/circulation/requests/%s";
  private static final String CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS =
    "/circulation/requests/allowed-service-points";
  private static final String ACTIVE = "active";
  private static final String PATRON_GROUP = "patronGroup";
  private static final String ADDRESS_TYPES = "addressTypes";
  private static final String USER_GROUPS = "usergroups";
  private static final String REMOTE_GROUP = "Remote Non-circulating";
  private static final String HOME = "home";
  private static final String WORK = "work";
  private static final String ADDRESS_TYPE = "addressType";
  private static final String ID = "id";
  private static final String USERS = "users";

  @Override
  public void postPatronAccount(ExternalPatron entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());
    final var userRepository = new UserRepository(httpClient);
    String patronEmail = entity.getContactInfo().getEmail();
    getUserByEmail(patronEmail, okapiHeaders, userRepository)
      .thenCompose(userResponse -> handleUserResponse(userResponse, entity, okapiHeaders, userRepository))
      .thenAccept(response -> asyncResultHandler.handle(Future.succeededFuture(response)))
      .exceptionally(throwable -> {
        asyncResultHandler.handle(handleError(throwable));
        return null;
      });
  }

  @Override
  public void getPatronAccountByEmailByEmailId(String email, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());
    final var userRepository = new UserRepository(httpClient);

    getUserByEmail(email, okapiHeaders, userRepository)
      .thenAccept(userResponse -> handleGetUserResponse(userResponse, asyncResultHandler))
      .exceptionally(throwable -> {
        asyncResultHandler.handle(handleError(throwable));
        return null;
      });
  }

@Override
public void putPatronAccountByEmailByEmailId(String emailId, ExternalPatron entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
  var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());
  final var userRepository = new UserRepository(httpClient);

  getUserByEmail(emailId, okapiHeaders, userRepository)
    .thenCompose(userResponse -> handleUserUpdateResponse(userResponse, entity, okapiHeaders, userRepository))
    .thenAccept(response -> asyncResultHandler.handle(Future.succeededFuture(response)))
    .exceptionally(throwable -> {
      asyncResultHandler.handle(handleError(throwable));
      return null;
    });
}
  private CompletableFuture<Response> handleUserUpdateResponse(JsonObject userResponse, ExternalPatron entity, Map<String, String> okapiHeaders, UserRepository userRepository) {
    int totalRecords = userResponse.getInteger(TOTAL_RECORDS);
    if (totalRecords > 1) {
      return CompletableFuture.completedFuture(
        PutPatronAccountByEmailByEmailIdResponse.respond400WithTextPlain(MULTIPLE_USER_ERROR)
      );
    } else if (totalRecords == 1) {
      JsonObject userJson = userResponse.getJsonArray(USERS).getJsonObject(0);
      String userId = userJson.getString(ID);
      String patronGroup = userJson.getString(PATRON_GROUP);

      return getRemotePatronGroupId(userRepository, okapiHeaders)
        .thenCompose(remotePatronGroupId ->
          getAddressTypes(userRepository, okapiHeaders)
            .thenCompose(addressTypes -> {
              if (Objects.equals(patronGroup, remotePatronGroupId)) {
              String homeAddressTypeId = addressTypes.getString(HOME_ADDRESS_TYPE);
              String workAddressTypeId = addressTypes.getString(WORK_ADDRESS_TYPE);
                return updateUser(userId, entity, okapiHeaders, userRepository, remotePatronGroupId, homeAddressTypeId, workAddressTypeId);
              } else {
                return CompletableFuture.completedFuture(
                  PutPatronAccountByEmailByEmailIdResponse.respond500WithTextPlain("Required Patron group not applicable for user")
                );
              }
            })
        );
    } else {
      return CompletableFuture.completedFuture(
        PutPatronAccountByEmailByEmailIdResponse.respond404WithTextPlain("user does not exist")
      );
    }
  }

  private void handleGetUserResponse(JsonObject userResponse, Handler<AsyncResult<Response>> asyncResultHandler) {
    int totalRecords = userResponse.getInteger(TOTAL_RECORDS);

    if (totalRecords > 1) {
      asyncResultHandler.handle(Future.succeededFuture(GetPatronAccountByEmailByEmailIdResponse.respond400WithTextPlain(MULTIPLE_USER_ERROR)));
    } else if (totalRecords == 1) {
      JsonObject userJson = userResponse.getJsonArray(USERS).getJsonObject(0);
      User user = convertJsonToUser(userJson);
      ExternalPatron externalPatron = mapToExternalPatron(user);
      asyncResultHandler.handle(Future.succeededFuture(GetPatronAccountByEmailByEmailIdResponse.respond200WithApplicationJson(externalPatron)));
    } else {
      asyncResultHandler.handle(Future.succeededFuture(GetPatronAccountByEmailByEmailIdResponse.respond404WithTextPlain("User not found")));
    }
  }

  private User convertJsonToUser(JsonObject userJson) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      return mapper.readValue(userJson.encode(), User.class);
    } catch (IOException e) {
      return null;
    }
  }

  private CompletableFuture<JsonObject> getUserByEmail(String email, Map<String, String> okapiHeaders, UserRepository userRepository) {
    return userRepository.getUserByEmail(email, okapiHeaders);
  }

  private CompletableFuture<Response> handleUserResponse(JsonObject userResponse, ExternalPatron entity, Map<String, String> okapiHeaders, UserRepository userRepository) {
    int totalRecords = userResponse.getInteger(TOTAL_RECORDS);

    if (totalRecords > 1) {
      return CompletableFuture.completedFuture(
        PostPatronAccountResponse.respond400WithTextPlain(MULTIPLE_USER_ERROR)
      );
    } else if (totalRecords == 1) {
      JsonObject userJson = userResponse.getJsonArray(USERS).getJsonObject(0);
      return processSingleUser(userJson, userRepository, okapiHeaders);
    } else {
      return getRemotePatronGroupId(userRepository, okapiHeaders)
        .thenCompose(remotePatronGroupId -> getAddressTypes(userRepository, okapiHeaders)
          .thenCompose(addressTypes -> {
            String homeAddressTypeId = addressTypes.getString(HOME_ADDRESS_TYPE);
            String workAddressTypeId = addressTypes.getString(WORK_ADDRESS_TYPE);
            return createUser(entity, okapiHeaders, userRepository, remotePatronGroupId, homeAddressTypeId, workAddressTypeId);
          })
        );
    }
  }

  private CompletableFuture<Response> processSingleUser(JsonObject userJson, UserRepository userRepository, Map<String, String> okapiHeaders) {
    boolean isActive = userJson.getBoolean(ACTIVE, false);
    String patronGroup = userJson.getString(PATRON_GROUP, "");

    return getRemotePatronGroupId(userRepository, okapiHeaders).thenApply(remotePatronGroupId -> {
      if (!isActive) {
        return PostPatronAccountResponse.respond422WithTextPlain("User account is not active");
      } else if (remotePatronGroupId.equals(patronGroup)) {
        return PostPatronAccountResponse.respond422WithTextPlain("User already exists");
      } else {
        return PostPatronAccountResponse.respond422WithTextPlain("User does not belong to the required patron group");
      }
    });
  }

  private CompletableFuture<String> getRemotePatronGroupId(UserRepository userRepository, Map<String, String> okapiHeaders) {
    return userRepository.getGroupByGroupName(REMOTE_GROUP, okapiHeaders)
      .thenApply(responseJson -> {
        if (responseJson.getInteger(TOTAL_RECORDS) > 0) {
          return responseJson.getJsonArray(USER_GROUPS).getJsonObject(0).getString(ID);
        } else {
          throw new IllegalArgumentException(new HttpException(500, "Remote patron group not found"));
        }
      });
  }

  private CompletableFuture<JsonObject> getAddressTypes(UserRepository userRepository, Map<String, String> okapiHeaders) {
    return userRepository.getAddressByType(okapiHeaders)
      .thenApply(responseJson -> {
        JsonObject addressTypes = new JsonObject();
        responseJson.getJsonArray(ADDRESS_TYPES).forEach(item -> {
          JsonObject addressType = (JsonObject) item;
          if (HOME.equalsIgnoreCase(addressType.getString(ADDRESS_TYPE))) {
            addressTypes.put(HOME, addressType.getString(ID));
          } else if (WORK.equalsIgnoreCase(addressType.getString(ADDRESS_TYPE))) {
            addressTypes.put(WORK, addressType.getString(ID));
          }
        });
        return addressTypes;
      });
  }

  private CompletableFuture<Response> createUser(ExternalPatron entity, Map<String, String> okapiHeaders, UserRepository userRepository, String remotePatronGroupId, String homeAddressTypeId, String workAddressTypeId) {
    User user = PatronUtils.mapToUser(entity, remotePatronGroupId, homeAddressTypeId, workAddressTypeId);
    return userRepository.createUser(user, okapiHeaders)
      .thenApply(createdUserJson ->
        PostPatronAccountResponse.respond201WithApplicationJson(entity)
      );
  }

  private CompletableFuture<Response> updateUser(String id, ExternalPatron entity, Map<String, String> okapiHeaders, UserRepository userRepository, String remotePatronGroupId, String homeAddressTypeId, String workAddressTypeId) {
    User user = PatronUtils.mapToUser(entity, remotePatronGroupId, homeAddressTypeId, workAddressTypeId);
    user.setId(id);
    return userRepository.updateUser(id, user, okapiHeaders)
      .thenApply(updatedUserJson ->
        PutPatronAccountByEmailByEmailIdResponse.respond204WithApplicationJson(entity)
      );
  }

  @Validate
  @Override
  public void getPatronAccountById(String id, boolean includeLoans,
      boolean includeCharges, boolean includeHolds,
      String sortBy,
      int offset,
      int limit,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {

    var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());

    final var userRepository = new UserRepository(httpClient);

    try {
      // Look up the user to ensure that the user exists and is enabled
      userRepository.getUser(id, okapiHeaders)
        .thenCompose(notUsed -> {
          return getCurrencyCode(okapiHeaders, httpClient);
        })
        .thenCompose(code -> {
          try {
            final Account account = new Account();

            account.setTotalCharges(new TotalCharges().withAmount(0.0).withIsoCurrencyCode(code));
            account.setTotalChargesCount(0);

            final CompletableFuture<Account> cf1 = getLoans(id, sortBy, limit, offset, includeLoans, okapiHeaders,
              httpClient)
                .thenApply(body -> addLoans(account, body, includeLoans));

            final CompletableFuture<Account> cf2 = getRequests(id, sortBy, limit, offset, includeHolds, okapiHeaders,
              httpClient)
                .thenApply(body -> addHolds(account, body, includeHolds));

            final CompletableFuture<Account> cf3 = getAccounts(id, sortBy, limit, offset, okapiHeaders, httpClient)
                .thenApply(body -> addCharges(account, body, includeCharges, code))
                .thenCompose(charges -> {
                  if (includeCharges) {
                    List<CompletableFuture<Account>> cfs = new ArrayList<>();

                    for (Charge charge: account.getCharges()) {
                      if (charge.getItem() != null) {
                        cfs.add(lookupItem(charge, account, okapiHeaders,
                          httpClient));
                      }
                    }
                    return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]))
                        .thenApply(done -> account);
                  }
                  return completedFuture(account);
                });

            return CompletableFuture.allOf(cf1, cf2, cf3)
                .thenApply(result -> account);
          } catch (Exception e) {
            throw new CompletionException(e);
          }
        })
        .thenAccept(account -> {
          asyncResultHandler.handle(succeededFuture(GetPatronAccountByIdResponse.respond200WithApplicationJson(account)));
        })
        .exceptionally(throwable -> {
          asyncResultHandler.handle(handleError(throwable));
          return null;
        });
    } catch (Exception e) {
      asyncResultHandler.handle(succeededFuture(GetPatronAccountByIdResponse.respond500WithTextPlain(e.getMessage())));
    }
  }

  private CompletableFuture<String> getCurrencyCode(Map<String, String> okapiHeaders, VertxOkapiHttpClient httpClient) {
    String path = "/configurations/entries";
    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QUERY, "(module==ORG and configName==localeSettings)");
    return httpClient.get(path, queryParameters, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(response -> {
        String currencyType = "USD";
        Integer numOfResults = Integer.parseInt(response.getString(TOTAL_RECORDS));
        if (numOfResults == 1) {
          String value = response
            .getJsonArray("configs")
            .getJsonObject(0)
            .getString("value");
          JsonObject settings = new JsonObject(value);
          if (settings.containsKey("currency")) {
            currencyType = settings.getString("currency");
          }
        }
        return currencyType;
      });
  }

  private CompletableFuture<JsonObject> getAccounts(String id,
    String sortBy, int limit, int offset,
    Map<String, String> okapiHeaders, VertxOkapiHttpClient httpClient) {

    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.putAll(getLimitAndOffsetParams(limit, offset, true));
    queryParameters.put(QUERY, buildQueryWithUserId(id, sortBy));

    return httpClient.get("/accounts", queryParameters, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }

  private CompletableFuture<JsonObject> getRequests(String id,
    String sortBy, int limit, int offset,
    boolean includeHolds, Map<String, String> okapiHeaders, VertxOkapiHttpClient httpClient) {

    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.putAll(getLimitAndOffsetParams(limit, offset, includeHolds));
    queryParameters.put(QUERY, buildQueryWithRequesterId(id, sortBy));

    return httpClient.get("/circulation/requests", queryParameters, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }

  private CompletableFuture<JsonObject> getLoans(String id,
    String sortBy, int limit, int offset,
    boolean includeLoans, Map<String, String> okapiHeaders, VertxOkapiHttpClient httpClient) {

    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.putAll(getLimitAndOffsetParams(limit, offset, includeLoans));
    queryParameters.put(QUERY, buildQueryWithUserId(id, sortBy));

    return httpClient.get("/circulation/loans", queryParameters, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }

  @Validate
  @Override
  public void postPatronAccountItemRenewByIdAndItemId(String id, String itemId,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {

    var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());

    final JsonObject renewalJSON = new JsonObject()
        .put(JSON_FIELD_ITEM_ID, itemId)
        .put(JSON_FIELD_USER_ID, id);

    try {
      httpClient.post("/circulation/renew-by-id", renewalJSON, okapiHeaders)
          .thenApply(ResponseInterpreter::verifyAndExtractBody)
          .thenAccept(body -> {
            final Item item = getItem(itemId, body.getJsonObject(JSON_FIELD_ITEM));
            final Loan hold = getLoan(body, item);
            asyncResultHandler.handle(succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond201WithApplicationJson(hold)));
          })
          .exceptionally(throwable -> {
            asyncResultHandler.handle(handleRenewPOSTError(throwable));
            return null;
          });
    } catch (Exception e) {
      asyncResultHandler.handle(succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond500WithTextPlain(e.getMessage())));
    }
  }

  @Validate
  @Override
  public void postPatronAccountItemHoldByIdAndItemId(String id, String itemId,
      Hold entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {

    var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());

    RequestObjectFactory requestFactory = new RequestObjectFactory(httpClient, okapiHeaders);

    requestFactory.createRequestByItem(id, itemId, entity)
      .whenComplete((holdJSON, throwable) -> {
        if (throwable != null) {
          Throwable cause = throwable.getCause();
          if (cause instanceof ValidationException) {
            asyncResultHandler.handle(succeededFuture(respond422WithApplicationJson(
              ((ValidationException) cause).getErrors())));
          } else {
            asyncResultHandler.handle(succeededFuture(respond500WithTextPlain(cause
              .getMessage())));
          }
        } else {
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

              asyncResultHandler.handle(succeededFuture(respond422WithApplicationJson(errors)));
            }

            httpClient.post("/circulation/requests", holdJSON, okapiHeaders)
              .thenApply(ResponseInterpreter::verifyAndExtractBody)
              .thenAccept(body -> {
                final Item item = getItem(body);
                final Hold hold = getHold(body, item);
                asyncResultHandler.handle(succeededFuture(respond201WithApplicationJson(hold)));
              })
              .exceptionally(e -> {

                asyncResultHandler.handle(handleItemHoldPOSTError(e));
                return null;
              });
          } catch (Exception e) {
            asyncResultHandler.handle(succeededFuture(respond500WithTextPlain(e.getMessage())));
          }
        }
      });
  }

  @Validate
  @Override
  public void postPatronAccountHoldCancelByIdAndHoldId(String id, String holdId, Hold entity, Map<String, String> okapiHeaders, Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) {
    var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());

    final Hold[] holds = new Hold[1];

    completedFuture(format(CIRCULATION_REQUESTS, holdId))
      .thenCompose(path -> httpClient.get(path, Map.of(), okapiHeaders))
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(body -> {
        holds[0] = constructNewHoldWithCancellationFields(getHold(body, getItem(body)), entity);
        return body;
      })
      .thenCompose(updatedRequest -> httpClient.put(format(CIRCULATION_REQUESTS, holdId),
        createCancelRequest(updatedRequest, entity), okapiHeaders))
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .whenComplete((body, throwable) -> {
        if (throwable != null) {
          asyncResultHandler.handle(handleHoldCancelPOSTError(throwable));
        } else {
          asyncResultHandler.handle(succeededFuture(respond200WithApplicationJson(holds[0])));
        }
      });
  }

  @Validate
  @Override
  public void postPatronAccountInstanceHoldByIdAndInstanceId(String id,
      String instanceId, Hold entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler,
      Context vertxContext) {
    var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());

    final JsonObject holdJSON = new JsonObject()
        .put(JSON_FIELD_INSTANCE_ID, instanceId)
        .put(JSON_FIELD_REQUESTER_ID, id)
        .put(JSON_FIELD_REQUEST_DATE, new DateTime(entity.getRequestDate(), DateTimeZone.UTC).toString())
        .put(JSON_FIELD_PICKUP_SERVICE_POINT_ID, entity.getPickupLocationId())
        .put(JSON_FIELD_PATRON_COMMENTS, entity.getPatronComments());

    if (entity.getExpirationDate() != null) {
      holdJSON.put(JSON_FIELD_REQUEST_EXPIRATION_DATE,
          new DateTime(entity.getExpirationDate(), DateTimeZone.UTC).toString());
    }

    try {
      httpClient.post("/circulation/requests/instances", holdJSON, okapiHeaders)
          .thenApply(ResponseInterpreter::verifyAndExtractBody)
          .thenAccept(body -> {
            final Item item = getItem(body);
            final Hold hold = getHold(body, item);
            asyncResultHandler.handle(succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond201WithApplicationJson(hold)));
          })
          .exceptionally(throwable -> {
            asyncResultHandler.handle(handleInstanceHoldPOSTError(throwable));
            return null;
          });
    } catch (Exception e) {
      asyncResultHandler.handle(succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond500WithTextPlain(e.getMessage())));
    }
  }

  @Override
  public void getPatronAccountInstanceAllowedServicePointsByIdAndInstanceId(String requesterId,
    String instanceId, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());
    var queryParameters = Map.of("operation", "create",
      "requesterId", requesterId, "instanceId", instanceId);

    completedFuture(CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS)
      .thenCompose(path -> httpClient.get(path, queryParameters, okapiHeaders))
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(this::getAllowedServicePoints)
      .whenComplete((allowedServicePoints, throwable) -> {
        if (throwable != null) {
          asyncResultHandler.handle(handleAllowedServicePointsGetError(throwable));
        } else {
          asyncResultHandler.handle(succeededFuture(
            GetPatronAccountInstanceAllowedServicePointsByIdAndInstanceIdResponse
              .respond200WithApplicationJson(allowedServicePoints)));
        }
      });
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
          final Item item = getItem(loanObject.getString(JSON_FIELD_ITEM_ID),
                                    loanObject.getJsonObject(JSON_FIELD_ITEM));
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

    if (contributors != null) {
      for (Object o : contributors) {
        if (o instanceof JsonObject) {
          if (sb.length() != 0) {
            sb.append("; ");
          }
          sb.append(((JsonObject) o).getString(JSON_FIELD_NAME));
        }
      }
    }

    return new Item()
        .withAuthor(sb.length() == 0 ? null : sb.toString())
        .withInstanceId(itemJson.getString(JSON_FIELD_INSTANCE_ID))
        .withItemId(itemId)
        .withTitle(itemJson.getString(JSON_FIELD_TITLE));
  }

  private Item getItem(JsonObject body) {
    JsonObject itemJson = body.getJsonObject(JSON_FIELD_ITEM);
    if (itemJson == null) {
      itemJson = new JsonObject();
    }
    JsonObject instanceJson = body.getJsonObject(JSON_FIELD_INSTANCE);
    itemJson.put(JSON_FIELD_INSTANCE_ID, body.getString(JSON_FIELD_INSTANCE_ID));
    itemJson.put(JSON_FIELD_TITLE, instanceJson.getString(JSON_FIELD_TITLE));
    itemJson.put(JSON_FIELD_CONTRIBUTORS, instanceJson.getJsonArray(JSON_FIELD_CONTRIBUTOR_NAMES));
    return getItem(body.getString(JSON_FIELD_ITEM_ID), itemJson);
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
          final Item item = getItem(holdJson);
          final Hold hold = getHold(holdJson, item);
          holds.add(hold);
        }
      }
    }

    return account;
  }

  private Account addCharges(Account account, JsonObject body, boolean includeCharges, String currencyCode) {
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
          Charge charge = getCharge(accountJson, currencyCode);
          if (accountJson.getString(JSON_FIELD_ITEM_ID) != null) {
            final Item item = new Item().withItemId(accountJson.getString(JSON_FIELD_ITEM_ID));
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
        .withIsoCurrencyCode(currencyCode));

    return account;
  }

  private Charge getCharge(JsonObject chargeJson, String currencyCode) {
    return new Charge()
        .withAccrualDate(new DateTime(chargeJson.getJsonObject("metadata").getString("createdDate"), DateTimeZone.UTC).toDate())
        .withChargeAmount(new TotalCharges().withAmount(chargeJson.getDouble("remaining")).withIsoCurrencyCode(currencyCode))
        .withState(chargeJson.getJsonObject("paymentStatus",
            new JsonObject().put(JSON_FIELD_NAME,  "Unknown"))
                  .getString(JSON_FIELD_NAME))
        .withReason(chargeJson.getString("feeFineType"));
  }

  private CompletableFuture<Account> lookupItem(Charge charge, Account account,
    Map<String, String> okapiHeaders, VertxOkapiHttpClient httpClient) {

    final var itemRepository = new ItemRepository(httpClient);

    return itemRepository.getItem(charge.getItem().getItemId(), okapiHeaders)
        .thenCompose(item -> getInstance(item, okapiHeaders, httpClient))
        .thenApply(instance -> getItem(charge, instance))
        .thenApply(item -> updateItem(charge, item, account));
  }

  private CompletableFuture<JsonObject> getInstance(
    JsonObject item, Map<String, String> okapiHeaders,
    VertxOkapiHttpClient httpClient) {

    try {
      String cql = "holdingsRecords.id==" +
          StringUtil.cqlEncode(item.getString(JSON_FIELD_HOLDINGS_RECORD_ID));

      return httpClient.get("/inventory/instances", Map.of(QUERY, cql), okapiHeaders)
          .thenApply(ResponseInterpreter::verifyAndExtractBody)
          .thenApply(instances -> instances.getJsonArray("instances").getJsonObject(0));
    } catch (Exception e) {
      throw new CompletionException(e);
    }
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

  private Map<String, String> getLimitAndOffsetParams(int limit, int offset, boolean includeItem) {
    if (!includeItem || limit == 0) {
      return Map.of(
        "limit", "0");
    }
    if (limit > 0) {
      return Map.of(
        "limit", String.valueOf(limit),
        "offset", String.valueOf(offset));
    }
    return Map.of(
      "limit", String.valueOf(Integer.MAX_VALUE));
  }

  private AllowedServicePoints getAllowedServicePoints(JsonObject body) {
    Set<AllowedServicePoint> allowedSpSet = Stream.of(body.getJsonArray("Page"),
        body.getJsonArray("Hold"), body.getJsonArray("Recall"))
      .filter(Objects::nonNull)
      .flatMap(JsonArray::stream)
      .map(JsonObject.class::cast)
      .map(sp -> new AllowedServicePoint()
        .withId(sp.getString("id"))
        .withName(sp.getString("name")))
      .filter(distinctBy(AllowedServicePoint::getId))
      .collect(Collectors.toSet());

    return new AllowedServicePoints().withAllowedServicePoints(allowedSpSet);
  }

  public static <T> Predicate<T> distinctBy(Function<? super T, ?> keyFunction) {
    Set<Object> result = ConcurrentHashMap.newKeySet();
    return o -> result.add(keyFunction.apply(o));
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
          result = succeededFuture(GetPatronAccountByIdResponse.respond400WithTextPlain(message));
        } else {
          result = succeededFuture(GetPatronAccountByIdResponse.respond500WithTextPlain(message));
        }
        break;
      case 401:
        result = succeededFuture(GetPatronAccountByIdResponse.respond401WithTextPlain(message));
        break;
      case 403:
        result = succeededFuture(GetPatronAccountByIdResponse.respond403WithTextPlain(message));
        break;
      case 404:
        result = succeededFuture(GetPatronAccountByIdResponse.respond404WithTextPlain(message));
        break;
      default:
        result = succeededFuture(GetPatronAccountByIdResponse.respond500WithTextPlain(message));
      }
    } else {
      result = succeededFuture(GetPatronAccountByIdResponse.respond500WithTextPlain(throwable.getMessage()));
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
        result = succeededFuture(respond500WithTextPlain(message));
        break;
      case 401:
        result = succeededFuture(respond401WithTextPlain(message));
        break;
      case 403:
        result = succeededFuture(respond403WithTextPlain(message));
        break;
      case 404:
        result = succeededFuture(respond404WithTextPlain(message));
        break;
      case 422:
        final Errors errors = Json.decodeValue(message, Errors.class);
        result = succeededFuture(respond422WithApplicationJson(errors));
        break;
      default:
        result = succeededFuture(respond500WithTextPlain(message));
      }
    } else {
      result = succeededFuture(respond500WithTextPlain(throwable.getMessage()));
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
        result = succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond500WithTextPlain(message));
        break;
      case 401:
        result = succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond401WithTextPlain(message));
        break;
      case 403:
        result = succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond403WithTextPlain(message));
        break;
      case 404:
        result = succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond404WithTextPlain(message));
        break;
      case 422:
        final Errors errors = Json.decodeValue(message, Errors.class);
        result = succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond422WithApplicationJson(errors));
        break;
      default:
        result = succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond500WithTextPlain(message));
      }
    } else {
      result = succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond500WithTextPlain(throwable.getMessage()));
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
        result = succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond500WithTextPlain(message));
        break;
      case 401:
        result = succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond401WithTextPlain(message));
        break;
      case 403:
        result = succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond403WithTextPlain(message));
        break;
      case 404:
        result = succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond404WithTextPlain(message));
        break;
      case 422:
        final Errors errors = Json.decodeValue(message, Errors.class);
        result = succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond422WithApplicationJson(errors));
        break;
      default:
        result = succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond500WithTextPlain(message));
      }
    } else {
      result = succeededFuture(PostPatronAccountItemRenewByIdAndItemIdResponse.respond500WithTextPlain(throwable.getMessage()));
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
        result = succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond400WithTextPlain(message));
        break;
      case 401:
        result = succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond401WithTextPlain(message));
        break;
      case 403:
        result = succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond403WithTextPlain(message));
        break;
      case 404:
        result = succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond404WithTextPlain(message));
        break;
      case 422:
        final Errors errors = Json.decodeValue(message, Errors.class);
        result = succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond422WithApplicationJson(errors));
        break;
      default:
        result = succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond500WithTextPlain(message));
      }
    } else {
      result = succeededFuture(PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond500WithTextPlain(throwable.getMessage()));
    }

    return result;
  }

  private Future<javax.ws.rs.core.Response> handleAllowedServicePointsGetError(Throwable throwable) {
    final Future<javax.ws.rs.core.Response> result;

    final Throwable t = throwable.getCause();
    if (t instanceof HttpException httpexception) {
      final int code = httpexception.getCode();
      final String message = t.getMessage();
      if (code == 422) {
        final Errors errors = Json.decodeValue(message, Errors.class);
        result = succeededFuture(
          GetPatronAccountInstanceAllowedServicePointsByIdAndInstanceIdResponse
            .respond422WithApplicationJson(errors));
      } else {
        result = succeededFuture(
          GetPatronAccountInstanceAllowedServicePointsByIdAndInstanceIdResponse
            .respond500WithTextPlain(message));
      }
    } else {
      result = succeededFuture(GetPatronAccountInstanceAllowedServicePointsByIdAndInstanceIdResponse
        .respond500WithTextPlain(throwable.getMessage()));
    }

    return result;
  }

  private String buildQueryWithUserId(String userId, String sortBy) {
    if(StringUtils.isNoneBlank(sortBy)) {
      return format("(userId==%s and status.name==Open) sortBy %s", userId, sortBy);
    }
    return format("(userId==%s and status.name==Open)", userId);
  }

  private String buildQueryWithRequesterId(String requesterId, String sortBy) {
    if(StringUtils.isNoneBlank(sortBy)) {
      return format("(requesterId==%s and status==Open*) sortBy %s", requesterId, sortBy);
    }
    return format("(requesterId==%s and status==Open*)", requesterId);
  }

}
