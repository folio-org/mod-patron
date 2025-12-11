package org.folio.patron.rest.exceptions;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;
import javax.ws.rs.core.Response;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Parameter;

/**
 * Exception thrown when patron settings operations fail.
 */
public class PatronSettingsException extends RuntimeException {

  private final int statusCode;
  private final transient Error error;

  /**
   * Creates an exception for a setting not found by ID.
   *
   * @param id the ID of the setting that was not found
   * @return a PatronSettingsException with NOT_FOUND status
   */
  public static PatronSettingsException notFoundById(String id) {
    var err = new Error()
      .withMessage("Setting entity not found by id: " + id)
      .withCode("not_found_error")
      .withParameters(List.of(new Parameter().withKey("id").withValue(id)));
    return new PatronSettingsException(err, Response.Status.NOT_FOUND);
  }

  /**
   * Creates an exception for a setting not found by key.
   *
   * @param key the key of the setting that was not found
   * @return a PatronSettingsException with NOT_FOUND status
   */
  public static PatronSettingsException notFoundByKey(String key) {
    var err = new Error()
      .withMessage("Setting entity not found by key: " + key)
      .withCode("not_found_error")
      .withParameters(List.of(new Parameter().withKey("key").withValue(key)));
    return new PatronSettingsException(err, Response.Status.NOT_FOUND);
  }

  /**
   * Creates an exception for an invalid setting value.
   *
   * @param key   the setting key
   * @param value the invalid value
   * @return a PatronSettingsException with 422 status
   */
  public static PatronSettingsException invalidSettingValue(String key, Object value) {
    var strValue = value == null ? "null" : value.toString();
    var message = String.format("Invalid setting value: %s for key: %s", strValue, key);
    var err = new Error()
      .withMessage(message)
      .withCode("invalid_setting_value")
      .withParameters(List.of(new Parameter().withKey("value").withValue(strValue)));
    return new PatronSettingsException(err, 422);
  }

  /**
   * Constructs a PatronSettingsException without capturing a stack trace.
   *
   * <p>The stack trace is disabled to reduce allocation overhead for expected control-flow errors.
   *
   * @param error      the error payload to return to the client
   * @param statusCode the HTTP status to use for the response
   */
  public PatronSettingsException(Error error, Response.Status statusCode) {
    super(error.getMessage(), null, true, false);
    this.error = error;
    this.statusCode = statusCode.getStatusCode();
  }

  /**
   * Constructs a PatronSettingsException without capturing a stack trace.
   *
   * <p>The stack trace is disabled to reduce allocation overhead for expected control-flow errors.
   *
   * @param error      the error payload to return to the client
   * @param statusCode the HTTP status to use for the response
   */
  public PatronSettingsException(Error error, int statusCode) {
    super(error.getMessage(), null, true, false);
    this.error = error;
    this.statusCode = statusCode;
  }

  /**
   * Constructs a PatronSettingsException with a cause, without capturing a stack trace.
   *
   * <p>The stack trace is disabled to reduce allocation overhead for expected control-flow errors.
   *
   * @param error      the error payload to return to the client
   * @param statusCode the HTTP status to use for the response
   * @param cause      the cause of this exception
   */
  public PatronSettingsException(Error error, int statusCode, Throwable cause) {
    super(error.getMessage(), cause, true, false);
    this.error = error;
    this.statusCode = statusCode;
  }

  /**
   * Builds a JAX-RS {@link Response} from this exception using the configured error payload and status.
   *
   * @return a JAX-RS Response with JSON error entity and appropriate status
   */
  public Response buildErrorResponse() {
    return Response.status(statusCode)
      .entity(new Errors().withErrors(List.of(error)))
      .header(CONTENT_TYPE, APPLICATION_JSON)
      .build();
  }

  /**
   * Returns the error payload associated with this exception.
   *
   * @return the {@link Error} object containing error details
   */
  public Error getError() {
    return error;
  }
}
