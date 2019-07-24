package uk.gov.ons.ctp.integration.event.generator.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.integration.event.generator.service.FirestoreService;

/** This endpoint gives Cucumber tests support level access to the projects Firestore content. */
@RestController
@RequestMapping(produces = "application/json")
public class FirestoreEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(FirestoreEndpoint.class);

  private FirestoreService firestoreService = new FirestoreService();

  /**
   * This endpoint allows the caller to wait for an object to appear in Firestore. If the object is
   * not found within the timeout period then we respond with a 404 (Object not found).
   *
   * <p>The caller can optionally wait for an object to be updated by specifying the updated
   * timestamp or by the contact of a named field. If both criteria are specified then both
   * conditions must be satisfied before we regard the object has having arrived in Firestore.
   *
   * @param collection is the name of the collection to search, eg, 'case'
   * @param key is the key of the target object in the collection. eg,
   *     'f868fcfc-7280-40ea-ab01-b173ac245da3'
   * @param newerThan, is an optional argument to specify the timestamp that the an object must have
   *     been updated after. Waiting will continue until the until the update timestamp of the
   *     object is greater than this value, or the timeout period is reached. This value is the
   *     number of milliseconds since the epoch.
   * @param contentCheckPath, is an optional path to a field whose content we check to decide if an
   *     object has been updated, eg 'contact.forename' or 'state'. If the target object does not
   *     contain the field with the expected value then waiting will continue until it does, or the
   *     timeout is reached.
   * @param expectedValue, is the value than a field must contain if 'contentCheckPath' has been
   *     set.
   * @param timeout specifies how long the caller is prepared to wait for an object to appear in
   *     Firestore. This string must end with either a 'ms' suffix for milliseconds or 's' for
   *     seconds, eg '750ms', '10s or '2.5s'.
   * @return The number of update timestamp of a found object.
   * @throws Exception if something went wrong, eg, Firestore exception or if the 'contentCheckPath'
   *     field could not be found in the target object.
   */
  @RequestMapping(value = "/firestore/wait", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> firestoreWait(
      String collection,
      String key,
      Long newerThan,
      String contentCheckPath,
      String expectedValue,
      String timeout)
      throws Exception {
    long startTime = System.currentTimeMillis();
    long timeoutMillis = parseTimeoutString(timeout);
    long timeoutLimit = startTime + timeoutMillis;

    log.info(
        "Firestore wait for collection '"
            + collection
            + "' to contain '"
            + key
            + "' "
            + "for up to '"
            + timeout
            + "'");

    // Validate matching path+value arguments
    if (contentCheckPath != null ^ expectedValue != null) {
      String errorMessage =
          "Mismatched 'path' and 'value' arguments."
              + " Either both must be supplied or neither supplied";
      return ResponseEntity.badRequest().body(errorMessage);
    }

    // Wait until the object appears in Firestore, or we timeout waiting
    boolean found = false;
    long objectUpdateTimestamp;
    do {
      objectUpdateTimestamp =
          firestoreService.objectExists(
              collection, key, newerThan, contentCheckPath, expectedValue);
      if (objectUpdateTimestamp > 0) {
        log.debug("Found object");
        found = true;
        break;
      }

      Thread.sleep(10);
    } while (System.currentTimeMillis() < timeoutLimit);

    if (!found) {
      log.debug("Failed to find object");
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(Long.toString(objectUpdateTimestamp));
  }

  private long parseTimeoutString(String timeout) throws Exception {
    int multiplier;
    if (timeout.endsWith("ms")) {
      multiplier = 1;
    } else if (timeout.endsWith("s")) {
      multiplier = 1000;
    } else {
      throw new Exception("timeout must end with either 'ms' for milliseconds or 's' for seconds");
    }

    String timeoutValue = timeout.replaceAll("(ms|s)", "");

    double timeoutAsDouble = Double.parseDouble(timeoutValue) * multiplier;
    return (long) timeoutAsDouble;
  }
}
