package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.HttpStatus.HTTP_UNPROCESSABLE_ENTITY;
import static org.folio.patron.rest.models.ExternalPatronErrorCode.EMAIL_ALREADY_EXIST;
import static org.folio.patron.rest.models.ExternalPatronErrorCode.INVALID_PATRON_GROUP;
import static org.folio.patron.rest.models.ExternalPatronErrorCode.MULTIPLE_USER_WITH_EMAIL;
import static org.folio.patron.rest.models.ExternalPatronErrorCode.PATRON_GROUP_NOT_APPLICABLE;
import static org.folio.patron.rest.models.ExternalPatronErrorCode.USER_ACCOUNT_INACTIVE;
import static org.folio.patron.rest.models.ExternalPatronErrorCode.USER_ALREADY_EXIST;
import static org.folio.patron.rest.models.ExternalPatronErrorCode.USER_NOT_FOUND;
import static org.folio.patron.rest.utils.PatronUtils.mapUserCollectionToExternalPatronCollection;
import static org.folio.rest.impl.Constants.JSON_FIELD_CONTRIBUTORS;
import static org.folio.rest.impl.Constants.JSON_FIELD_CONTRIBUTOR_NAMES;
import static org.folio.rest.impl.Constants.JSON_FIELD_HOLDINGS_RECORD_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_INSTANCE;
import static org.folio.rest.impl.Constants.JSON_FIELD_INSTANCE_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_ITEM;
import static org.folio.rest.impl.Constants.JSON_FIELD_ITEM_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_NAME;
import static org.folio.rest.impl.Constants.JSON_FIELD_PATRON_COMMENTS;
import static org.folio.rest.impl.Constants.JSON_FIELD_PICKUP_SERVICE_POINT_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUESTER_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_DATE;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_EXPIRATION_DATE;
import static org.folio.rest.impl.Constants.JSON_FIELD_TITLE;
import static org.folio.rest.impl.Constants.JSON_FIELD_TOTAL_RECORDS;
import static org.folio.rest.impl.Constants.JSON_FIELD_USER_ID;
import static org.folio.rest.impl.HoldHelpers.constructNewHoldWithCancellationFields;
import static org.folio.rest.impl.HoldHelpers.createCancelRequest;
import static org.folio.rest.impl.HoldHelpers.getHold;
import static org.folio.rest.impl.UrlPath.CIRCULATION_BFF_ALLOWED_SERVICE_POINTS_URL_PATH;
import static org.folio.rest.impl.UrlPath.CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS_URL_PATH;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountHoldCancelByIdAndHoldIdResponse.respond200WithApplicationJson;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond201WithApplicationJson;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond401WithTextPlain;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond403WithTextPlain;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond404WithTextPlain;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond422WithApplicationJson;
import static org.folio.rest.jaxrs.resource.Patron.PostPatronAccountItemHoldByIdAndItemIdResponse.respond500WithTextPlain;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
  private static final Logger logger = LogManager.getLogger();
  private static final String HOME_ADDRESS_TYPE = "home";
  private static final String TOTAL_RECORDS = "totalRecords";
  private static final String QUERY = "query";
  private static final String CIRCULATION_REQUESTS = "/circulation/requests/%s";
  private static final String ACTIVE = "active";
  private static final String PATRON_GROUP = "patronGroup";
  private static final String ADDRESS_TYPES = "addressTypes";
  private static final String USER_GROUPS = "usergroups";
  private static final String REMOTE_GROUP = "Remote Non-circulating";
  private static final String HOME_FIELD = "home";
  private static final String ADDRESS_TYPE_FIELD = "addressType";
  private static final String ID_FILED = "id";
  private static final String USERS_FILED = "users";
  private static final String BAD_REQUEST_CODE = "BAD_REQUEST";
  private static final String PROCESS_SINGLE_USER = "processSingleUser:: {}";
  private static final String VALUE_KEY = "value";

  @Override
  public void postPatronAccount(ExternalPatron entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info("postPatronAccount:: Trying to create external patron");
    final var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());
    final var userRepository = new UserRepository(httpClient);
    final String patronEmail = entity.getContactInfo().getEmail();

    getUserByEmail(patronEmail, okapiHeaders, userRepository)
      .thenCompose(userResponse -> handleUserResponse(userResponse, entity, okapiHeaders, userRepository))
      .thenAccept(response -> asyncResultHandler.handle(Future.succeededFuture(response)))
      .exceptionally(throwable -> {
        logger.error("postPatronAccount:: Failed to create external patron", throwable);
        asyncResultHandler.handle(Future.succeededFuture(PostPatronAccountResponse.respond500WithTextPlain(throwable.getCause().getMessage())));
        return null;
      });
  }

  @Override
  public void getPatronRegistrationStatusByEmailId(String email, Map<String, String> okapiHeaders,
                                                          Handler<AsyncResult<Response>> asyncResultHandler,
                                                          Context vertxContext) {
    logger.debug("getPatronAccountRegistrationStatusByEmailId:: Fetching patron details with emailId {}", email);
    final var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());
    final var userRepository = new UserRepository(httpClient);

    getUserByEmailWithPatronType(email, okapiHeaders, userRepository)
      .thenAccept(userResponse -> handleGetUserResponse(email, userResponse, asyncResultHandler))
      .exceptionally(throwable -> {
        logger.error("getPatronAccountRegistrationStatusByEmailId:: Failed to get patron details by email {}",
          email, throwable);
        asyncResultHandler.handle(Future.succeededFuture(GetPatronRegistrationStatusByEmailIdResponse
          .respond500WithTextPlain(throwable.getCause().getMessage())));
        return null;
      });
  }

  @Override
  public void putPatronAccountByEmailByEmailId(String emailId, ExternalPatron entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    final var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());
    final var userRepository = new UserRepository(httpClient);

    getUserByEmail(emailId, okapiHeaders, userRepository)
      .thenCompose(userResponse -> handleUserUpdateResponse(emailId, userResponse, entity, okapiHeaders, userRepository))
      .thenAccept(response -> asyncResultHandler.handle(Future.succeededFuture(response)))
      .exceptionally(throwable -> {
        logger.error("putPatronAccountByEmailByEmailId:: Failed to update external patron by email", throwable);
        asyncResultHandler.handle(handleError(throwable));
        return null;
      });
  }

  @Override
  public void getPatronAccount(boolean expired, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    final var httpClient = HttpClientFactory.getHttpClient(vertxContext.owner());
    final var userRepository = new UserRepository(httpClient);

    if (!expired) {
      JsonObject emptyResponse = new JsonObject()
        .put(USERS_FILED, new JsonArray())
        .put(TOTAL_RECORDS, 0);
      asyncResultHandler.handle(Future.succeededFuture(
        GetPatronAccountResponse.respond200WithApplicationJson(mapUserCollectionToExternalPatronCollection(emptyResponse.encode()))
      ));
      return;
    }

    var now = Instant.now();
    var yesterday = now.minus(1, ChronoUnit.DAYS).atZone(ZoneId.systemDefault()).toLocalDate();
    var yesterdayStr = yesterday.toString();

    userRepository.getUsersByExpDate(yesterdayStr, okapiHeaders)
      .thenAccept(usersResponse ->
        asyncResultHandler.handle(Future.succeededFuture(
          GetPatronAccountResponse.respond200WithApplicationJson(mapUserCollectionToExternalPatronCollection(usersResponse.encode()))
        ))
      )
      .exceptionally(throwable -> {
        logger.error("getPatronAccount:: Failed to get external patrons", throwable);
        asyncResultHandler.handle(Future.succeededFuture(
          GetPatronAccountResponse.respond500WithTextPlain(throwable.getCause().getMessage())));
        return null;
      });
  }

  private CompletableFuture<Response> handleUserUpdateResponse(String emailId, JsonObject userResponse, ExternalPatron entity, Map<String, String> okapiHeaders, UserRepository userRepository) {
    final int totalRecords = userResponse.getInteger(TOTAL_RECORDS);
    if (totalRecords > 1) {
      return CompletableFuture.completedFuture(
        PutPatronAccountByEmailByEmailIdResponse.respond422WithApplicationJson(createError(MULTIPLE_USER_WITH_EMAIL.name(), String.valueOf(HTTP_UNPROCESSABLE_ENTITY)))
      );
    } else if (totalRecords == 1) {
      final JsonObject userJson = userResponse.getJsonArray(USERS_FILED).getJsonObject(0);
      final String userId = userJson.getString(ID_FILED);
      final String patronGroup = userJson.getString(PATRON_GROUP);

      return getRemotePatronGroupId(userRepository, okapiHeaders)
        .thenCompose(remotePatronGroupId ->
          getAddressTypes(userRepository, okapiHeaders)
            .thenCompose(addressTypes -> {
              if (Objects.equals(patronGroup, remotePatronGroupId)) {
              final String homeAddressTypeId = addressTypes.getString(HOME_ADDRESS_TYPE);
                final String entityEmail = entity.getContactInfo().getEmail();
                if (!emailId.equals(entityEmail)) {
                  return getUserByEmail(entityEmail, okapiHeaders, userRepository)
                    .thenCompose(userEmailResponse -> {
                      final int records = userEmailResponse.getInteger(TOTAL_RECORDS);
                      if (records > 0) {
                        logger.error("putPatronAccountByEmailByEmailId:: {}", EMAIL_ALREADY_EXIST.value());
                        return CompletableFuture.completedFuture(
                          PutPatronAccountByEmailByEmailIdResponse.respond422WithApplicationJson(createError(EMAIL_ALREADY_EXIST.name(), String.valueOf(HTTP_UNPROCESSABLE_ENTITY))));
                      } else {
                        return updateUser(userId, entity, okapiHeaders, userRepository, remotePatronGroupId, homeAddressTypeId);
                      }
                    });
                } else {
                  return updateUser(userId, entity, okapiHeaders, userRepository, remotePatronGroupId, homeAddressTypeId);
                }
              } else {
                return CompletableFuture.completedFuture(
                  PutPatronAccountByEmailByEmailIdResponse.respond422WithApplicationJson(createError(PATRON_GROUP_NOT_APPLICABLE.name(), String.valueOf(HTTP_UNPROCESSABLE_ENTITY)))
                );
              }
            })
        );
    } else {
      return CompletableFuture.completedFuture(
        PutPatronAccountByEmailByEmailIdResponse.respond404WithTextPlain(USER_NOT_FOUND.name())
      );
    }
  }

  private void handleGetUserResponse(String email, JsonObject userResponse, Handler<AsyncResult<Response>> asyncResultHandler) {
    final int totalRecords = userResponse.getJsonArray(USERS_FILED).size();

    if (totalRecords > 1) {
      logger.warn("handleGetUserResponse:: Multiple user record found for email {}", email);
      asyncResultHandler.handle(Future.succeededFuture(GetPatronRegistrationStatusByEmailIdResponse.
        respond400WithApplicationJson(createError(MULTIPLE_USER_WITH_EMAIL.value(), MULTIPLE_USER_WITH_EMAIL.name()))));
    } else if (totalRecords == 1) {
      var userJson = userResponse.getJsonArray(USERS_FILED).getJsonObject(0);
      var user = convertJsonToUser(userJson);
      if (user != null && user.getActive()) {
        asyncResultHandler.handle(Future.succeededFuture(GetPatronRegistrationStatusByEmailIdResponse
          .respond200WithApplicationJson(user)));
      } else {
        logger.warn("handleGetUserResponse:: Inactive user record found for email {}", email);
        asyncResultHandler.handle(Future.succeededFuture(GetPatronRegistrationStatusByEmailIdResponse
          .respond404WithApplicationJson(createError(USER_ACCOUNT_INACTIVE.value(), USER_ACCOUNT_INACTIVE.name()))));
      }
    } else {
      logger.warn("handleGetUserResponse:: user record not found for email {}", email);
      asyncResultHandler.handle(Future.succeededFuture(GetPatronRegistrationStatusByEmailIdResponse
        .respond404WithApplicationJson(createError(USER_NOT_FOUND.value(), USER_NOT_FOUND.name()))));
    }
  }

  private org.folio.rest.jaxrs.model.User convertJsonToUser(JsonObject userJson) {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      return mapper.readValue(userJson.encode(), org.folio.rest.jaxrs.model.User.class);
    } catch (IOException e) {
      return null;
    }
  }

  private CompletableFuture<JsonObject> getUserByEmail(String email, Map<String, String> okapiHeaders, UserRepository userRepository) {
    logger.info("getUserByEmail:: Trying to get user by email");
    return userRepository.getUserByCql("(personal.email==" + email + ")", okapiHeaders);
  }

  private CompletableFuture<JsonObject> getUserByEmailWithPatronType(String email, Map<String, String> okapiHeaders, UserRepository userRepository) {
    logger.info("getUserByEmailWithPatronType:: Trying to get user by email {}", email);
    return userRepository.getUserByCql("(personal.email==" + email + " and type==patron)", okapiHeaders);
  }

  private CompletableFuture<Response> handleUserResponse(JsonObject userResponse, ExternalPatron entity, Map<String, String> okapiHeaders, UserRepository userRepository) {
    final int totalRecords = userResponse.getInteger(TOTAL_RECORDS);

    if (totalRecords > 1) {
      logger.error("handleUserResponse:: More than 1 record found");
      return CompletableFuture.completedFuture(
        PostPatronAccountResponse.respond422WithApplicationJson(createError(MULTIPLE_USER_WITH_EMAIL.name(), String.valueOf(HTTP_UNPROCESSABLE_ENTITY)))
      );
    } else if (totalRecords == 1) {
      logger.info("handleUserResponse:: 1 record found");
      final JsonObject userJson = userResponse.getJsonArray(USERS_FILED).getJsonObject(0);
      return processSingleUser(userJson, userRepository, okapiHeaders);
    } else {
      logger.info("handleUserResponse:: No record found , creating user");
      return getRemotePatronGroupId(userRepository, okapiHeaders)
        .thenCompose(remotePatronGroupId -> getAddressTypes(userRepository, okapiHeaders)
          .thenCompose(addressTypes -> {
            final String homeAddressTypeId = addressTypes.getString(HOME_ADDRESS_TYPE);
            return createUser(entity, okapiHeaders, userRepository, remotePatronGroupId, homeAddressTypeId);
          })
        );
    }
  }

  private CompletableFuture<Response> processSingleUser(JsonObject userJson, UserRepository userRepository, Map<String, String> okapiHeaders) {
    final boolean isActive = userJson.getBoolean(ACTIVE, false);
    final String patronGroup = userJson.getString(PATRON_GROUP, "");

    logger.info("processSingleUser:: Processing user with patron group: {} and active status: {}", patronGroup, isActive);

    return getRemotePatronGroupId(userRepository, okapiHeaders).thenApply(remotePatronGroupId -> {
      logger.info("processSingleUser:: Retrieved remote patron group ID: {}", remotePatronGroupId);

      if (!isActive) {
        logger.warn(PROCESS_SINGLE_USER, USER_ACCOUNT_INACTIVE.value());
        return PostPatronAccountResponse.respond422WithApplicationJson(createError(USER_ACCOUNT_INACTIVE.name(), String.valueOf(HTTP_UNPROCESSABLE_ENTITY)));
      } else if (remotePatronGroupId.equals(patronGroup)) {
        logger.warn(PROCESS_SINGLE_USER, USER_ALREADY_EXIST.value());
        return PostPatronAccountResponse.respond422WithApplicationJson(createError(USER_ALREADY_EXIST.name(), String.valueOf(HTTP_UNPROCESSABLE_ENTITY)));
      } else {
        logger.warn(PROCESS_SINGLE_USER, INVALID_PATRON_GROUP.value());
        return PostPatronAccountResponse.respond422WithApplicationJson(createError(INVALID_PATRON_GROUP.name(), String.valueOf(HTTP_UNPROCESSABLE_ENTITY)));
      }
    });
  }

  private CompletableFuture<String> getRemotePatronGroupId(UserRepository userRepository, Map<String, String> okapiHeaders) {
    logger.info("getRemotePatronGroupId::Attempting to retrieve remote patron group ID");

    return userRepository.getGroupByGroupName(REMOTE_GROUP, okapiHeaders)
      .thenApply(responseJson -> {
        int totalRecords = responseJson.getInteger(TOTAL_RECORDS);
        logger.info("getRemotePatronGroupId::Received response with total records: {}", totalRecords);

        if (totalRecords > 0) {
          String groupId = responseJson.getJsonArray(USER_GROUPS).getJsonObject(0).getString(ID_FILED);
          logger.info("getRemotePatronGroupId::Found remote patron group ID: {}", groupId);
          return groupId;
        } else {
          String errorMessage = "Remote patron group not found";
          logger.error("getRemotePatronGroupId::{}", errorMessage);
          throw new IllegalArgumentException(new HttpException(500, errorMessage));
        }
      })
      .exceptionally(throwable -> {
        logger.error("getRemotePatronGroupId::Exception occurred while retrieving remote patron group ID", throwable);
        throw new CompletionException(throwable);
      });
  }

  private CompletableFuture<JsonObject> getAddressTypes(UserRepository userRepository, Map<String, String> okapiHeaders) {
    logger.info("getAddressTypes::Trying to get addressTypes");
    return userRepository.getAddressByType(okapiHeaders)
      .thenApply(responseJson -> {
        final JsonObject addressTypes = new JsonObject();
        responseJson.getJsonArray(ADDRESS_TYPES).forEach(item -> {
          final JsonObject addressType = (JsonObject) item;
          String address = addressType.getString(ADDRESS_TYPE_FIELD);
          if (HOME_FIELD.equalsIgnoreCase(address)) {
            addressTypes.put(HOME_FIELD, addressType.getString(ID_FILED));
            logger.info("getAddressTypes::Found home address type: {}", addressType.getString(ID_FILED));
          }
        });
        if (!addressTypes.containsKey(HOME_FIELD)) {
          logger.warn("getAddressTypes::Home address type not found");
        }
        return addressTypes;
      });
  }

  private CompletableFuture<Response> createUser(ExternalPatron entity, Map<String, String> okapiHeaders, UserRepository userRepository, String remotePatronGroupId, String homeAddressTypeId) {
    logger.info("createUser::Trying to create user");
    final User user = PatronUtils.mapExternalPatronToUser(entity, remotePatronGroupId, homeAddressTypeId);
    return userRepository.createUser(user, okapiHeaders)
      .thenApply(createdUserJson ->
        PostPatronAccountResponse.respond201WithApplicationJson(entity)
      );
  }

  private CompletableFuture<Response> updateUser(String id, ExternalPatron entity, Map<String, String> okapiHeaders, UserRepository userRepository, String remotePatronGroupId, String homeAddressTypeId) {
    logger.info("updateUser::Trying to update user");
    final User user = PatronUtils.mapExternalPatronToUser(entity, remotePatronGroupId, homeAddressTypeId);
    user.setId(id);
    return userRepository.updateUser(id, user, okapiHeaders)
      .thenApply(updatedUserJson ->
        PutPatronAccountByEmailByEmailIdResponse.respond204()
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
    logger.debug("getPatronAccountById:: Trying to get PatronAccount with parameters:  id: {}, includeLoans: {}, " +
      "includeCharges: {}, includeHolds: {}", id, includeLoans, includeCharges, includeHolds);
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
            logger.error("getPatronAccountById:: Exception in first thenCompose block while fetching PatronAccount ", e);
            throw new CompletionException(e);
          }
        })
        .thenAccept(account -> {
          asyncResultHandler.handle(succeededFuture(GetPatronAccountByIdResponse.respond200WithApplicationJson(account)));
        })
        .exceptionally(throwable -> {
          logger.error("getPatronAccountById:: Exception in exceptionally block for fetching PatronAccount " +
            "while handling result ", throwable);
          asyncResultHandler.handle(handleError(throwable));
          return null;
        });
    } catch (Exception e) {
      logger.error("getPatronAccountById:: Exception in outer try-catch block while initiating the process during " +
        "fetching PatronAccount", e);
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
            .getString(VALUE_KEY);
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
                  new Error()
                    .withMessage("Cannot find a valid request type for this item")
                    .withCode("CANNOT_FIND_VALID_REQUEST_FOR_ITEM")
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

    new EcsTlrSettingsService()
      .isEcsTlrFeatureEnabled(httpClient, okapiHeaders)
      .thenApply(this::getAllowedServicePointsUrl)
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
    Future<javax.ws.rs.core.Response> result;

    final Throwable t = throwable.getCause();
    if (t instanceof ValidationException validationException) {
      return succeededFuture(GetPatronAccountByIdResponse.respond422WithApplicationJson(
        validationException.getErrors()));
    }
    if (t instanceof HttpException httpException) {
      final int code = httpException.getCode();
      final String message = httpException.getMessage();
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
          result = succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond422WithApplicationJson(
            new Errors().withErrors(List.of(new Error()
              .withMessage(message)
              .withCode(BAD_REQUEST_CODE)))));
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
    if (t instanceof ValidationException validationException) {
      return succeededFuture(GetPatronAccountByIdResponse.respond422WithApplicationJson(
        validationException.getErrors()));
    }
    if (t instanceof HttpException) {
      final int code = ((HttpException) t).getCode();
      final String message = t.getMessage();
      switch (code) {
      case 400:
        // This means that we screwed up something in the request to another
        // module. This API only takes a UUID, so a client side 400 is not
        // possible here, only server side, which the client won't be able to
        // do anything about.
        result = succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond422WithApplicationJson(
          new Errors().withErrors(List.of(new Error()
            .withMessage(message)
            .withCode(BAD_REQUEST_CODE)))));
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

  private Errors createError(String message, String code) {
    return new Errors().withErrors(List.of(new Error().withMessage(message).withCode(code)));
  }

  private Future<javax.ws.rs.core.Response> handleInstanceHoldPOSTError(Throwable throwable) {
    final Future<javax.ws.rs.core.Response> result;

    final Throwable t = throwable.getCause();
    if (t instanceof HttpException) {
      final int code = ((HttpException) t).getCode();
      final String message = t.getMessage();
      switch (code) {
      case 400:
        // This means that we screwed up something in the request to another
        // module. This API only takes a UUID, so a client side 400 is not
        // possible here, only server side, which the client won't be able to
        // do anything about.
        result = succeededFuture(PostPatronAccountInstanceHoldByIdAndInstanceIdResponse.respond422WithApplicationJson(
          new Errors().withErrors(List.of(new Error()
            .withMessage(message)
            .withCode(BAD_REQUEST_CODE)))));
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

  private String getAllowedServicePointsUrl(Boolean isEcsTlrFeatureEnabled) {
    return BooleanUtils.isTrue(isEcsTlrFeatureEnabled)
      ? CIRCULATION_BFF_ALLOWED_SERVICE_POINTS_URL_PATH
      : CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS_URL_PATH;
  }
}
