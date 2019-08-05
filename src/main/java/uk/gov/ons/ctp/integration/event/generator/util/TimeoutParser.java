package uk.gov.ons.ctp.integration.event.generator.util;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.event.generator.endpoint.FirestoreEndpoint;

public class TimeoutParser {
  private static final Logger log = LoggerFactory.getLogger(FirestoreEndpoint.class);

  /**
   * Converts a string time specification into a milliseconds value. This method assumes a time in
   * the format of "[digits]ms|s", eg 500ms, 2s, 2.5s.
   *
   * @param timeout is a String specifying the time value.
   * @return long containing the number of milliseconds this time period represents.
   * @throws CTPException if the timeout string doesn't end with 'ms' or 's'.
   */
  public static long parseTimeoutString(String timeout) throws CTPException {
    int multiplier;
    if (timeout.endsWith("ms")) {
      multiplier = 1;
    } else if (timeout.endsWith("s")) {
      multiplier = 1000;
    } else {
      String errorMessage =
          "timeout specification ('"
              + timeout
              + "') must end with either 'ms' for milliseconds or 's' for seconds";
      log.error(errorMessage);
      throw new CTPException(Fault.VALIDATION_FAILED, errorMessage);
    }

    String timeoutValue = timeout.replaceAll("(ms|s)", "");

    double timeoutAsDouble = Double.parseDouble(timeoutValue) * multiplier;
    return (long) timeoutAsDouble;
  }
}
