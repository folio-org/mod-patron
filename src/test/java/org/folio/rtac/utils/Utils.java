/**
 *
 */
package org.folio.rtac.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Random;

import org.apache.commons.io.IOUtils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author mreno
 *
 */
public final class Utils {
  private static final Logger logger = LoggerFactory.getLogger("okapi");

  private Utils() {
    super();
  }

  /**
   * Returns a random ephemeral port that has a high probability of not being
   * in use.
   *
   * @return a random port number.
   */
  public static int getRandomPort() {
    int port = -1;
    do {
      // Use a random ephemeral port
      port = new Random().nextInt(16_384) + 49_152;
      try {
        final ServerSocket socket = new ServerSocket(port);
        socket.close();
      } catch (IOException e) {
        continue;
      }
      break;
    } while (true);

    return port;
  }

  public static String readMockFile(final String path) {
    try {
      final InputStream is = Utils.class.getClassLoader().getResourceAsStream(path);

      if (is != null) {
        return IOUtils.toString(is, "UTF-8");
      } else {
        return "";
      }
    } catch (Throwable e) {
      logger.error(String.format("Unable to read mock configuration in %s file", path));
    }

    return "";
  }
}
