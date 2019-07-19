package uk.gov.ons.ctp.integration.event.generator.endpoint;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.integration.event.generator.service.FirestoreService;

/**
 * This endpoint gives Cucumber tests support level access to the projects Firestore content.
 */
@RestController
@RequestMapping(produces = "application/json")
public class FirestoreEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(FirestoreEndpoint.class);

  private FirestoreService firestoreService = new FirestoreService();
  
  
  @RequestMapping(value = "/firestore/wait", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<Long> generate(String collection, String key, String timeout)
      throws Exception {
    long startTime = System.currentTimeMillis();
    long timeoutMillis = parseTimeoutString(timeout);
    
    log.info("Firestore wait for collection '" + collection + "' to contain '" + key + "' for up to '" + timeout + "'");
    
    // Wait until the object appears in Firestore, or we timeout waiting
    boolean found = false;
    int numAttempts = 0;
    do { 
      numAttempts++;
      boolean objectExists = firestoreService.objectExists(collection, key);
      if (objectExists) {
        log.debug("Found object after " + numAttempts + " attempt(s)");
        found = true;
        break;
      }
      
      Thread.sleep(10);
    } while (startTime + timeoutMillis > System.currentTimeMillis());
   
    if (!found) {
      log.debug("Failed to find object after " + numAttempts + " attempt(s)");
      return ResponseEntity.notFound().build();
    }
    
    long executionTime = System.currentTimeMillis() - startTime;
    return ResponseEntity.ok(executionTime);
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
