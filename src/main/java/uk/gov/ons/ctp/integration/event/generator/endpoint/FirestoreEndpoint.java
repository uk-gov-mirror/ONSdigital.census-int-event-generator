package uk.gov.ons.ctp.integration.event.generator.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.firestore.FirestoreWait;
import uk.gov.ons.ctp.integration.event.generator.util.TimeoutParser;

/** This endpoint gives Cucumber tests support level access to the projects Firestore content. */
@RestController
@RequestMapping(produces = "application/json")
public class FirestoreEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(FirestoreEndpoint.class);

  /**
   * This endpoint allows the caller to wait for an object to appear in Firestore. If the object is
   * not found within the timeout period then we respond with a 404 (Object not found).
   *
   * <p>The caller can optionally wait for an object to be updated by specifying the updated
   * timestamp or by the content of a named field. If both criteria are specified then both
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
   *     object has been updated, eg, 'contact.forename' or 'state'. If the target object does not
   *     contain the field with the expected value then waiting will continue until it does, or the
   *     timeout is reached.
   * @param expectedValue, is the value that a field must contain if 'contentCheckPath' has been
   *     specified.
   * @param timeout specifies how long the caller is prepared to wait for an object to appear in
   *     Firestore. This string must end with either a 'ms' suffix for milliseconds or 's' for
   *     seconds, eg, '750ms', '10s or '2.5s'.
   * @return The update timestamp of a found object, or null if not found within the timeout.
   */
  @RequestMapping(value = "/firestore/wait", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> firestoreWait(
      @RequestParam String collection,
      @RequestParam String key,
      Long newerThan,
      String contentCheckPath,
      String expectedValue,
      @RequestParam String timeout)
      throws Exception {

    log.info(
        "Firestore wait. Looking for for collection '"
            + collection
            + "' to contain '"
            + key
            + "' "
            + "for up to '"
            + timeout
            + "'");

    if (collection == null || key == null || timeout == null) {
      return ResponseEntity.badRequest().body("collection, key and timeout must all be specified");
    }

    Long objectUpdateTimestamp;
    try {
      long timeoutMillis = TimeoutParser.parseTimeoutString(timeout);

      FirestoreWait firestore =
          FirestoreWait.builder()
              .collection(collection)
              .key(key)
              .newerThan(newerThan)
              .contentCheckPath(contentCheckPath)
              .expectedValue(expectedValue)
              .timeout(timeoutMillis)
              .build();
      objectUpdateTimestamp = firestore.waitForObject();
    } catch (CTPException e) {
      if (e.getFault() == Fault.VALIDATION_FAILED) {
        return ResponseEntity.badRequest().body(e.getMessage());
      } else {
        return ResponseEntity.badRequest().body(e.getMessage());
      }
    }

    if (objectUpdateTimestamp == null) {
      log.debug("Failed to find object");
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(Long.toString(objectUpdateTimestamp));
  }
}
