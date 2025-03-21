package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.integration.http.Response;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.rest.jaxrs.model.StagingUser;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StagingUserRepository {
  private static final Logger logger = LogManager.getLogger();
  private final VertxOkapiHttpClient client;
  private static final String STAGING_USERS = "/staging-users";

  public StagingUserRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<Response> createStagingUser(StagingUser stagingUser, Map<String, String> okapiHeaders) {
    logger.info("createStagingUser::Creating staging-user: {}", stagingUser);
    return client.post(STAGING_USERS, JsonObject.mapFrom(stagingUser), okapiHeaders);
  }

  public CompletableFuture<Response> updateStagingUser(String externalSystemId ,
                                                       StagingUser stagingUser,
                                                       Map<String, String> okapiHeaders) {
    logger.info("updateStagingUser::Updating staging-user: {}", stagingUser);
    return client.put(STAGING_USERS +"/" + externalSystemId, JsonObject.mapFrom(stagingUser), okapiHeaders);
  }
}
