package uk.gov.ons.ctp.integration.event.generator.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.rabbit.RabbitHelper;
import uk.gov.ons.ctp.integration.event.generator.util.TimeoutParser;

/**
 * This endpoint gives command line access to Rabbit. It basically delegates to to Rabbit support
 * class, so it can also be used for manual testing of the Rabbit support code.
 */
@RestController
@RequestMapping(produces = "application/json")
public class RabbitEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(RabbitEndpoint.class);

  private static final String RABBIT_EXCHANGE = "events";

  private ObjectMapper mapper = new ObjectMapper();

  @RequestMapping(value = "/rabbit/create/{eventType}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> createQueue(
      @PathVariable(value = "eventType") final String eventTypeAsString) throws Exception {
    log.info("Creating queue for events of type: '" + eventTypeAsString + "'");
    EventType eventType = EventType.valueOf(eventTypeAsString);
    String queueName = rabbit().createQueue(eventType);
    return ResponseEntity.ok(queueName);
  }

  @RequestMapping(value = "/rabbit/flush/{queueName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<Integer> flushQueue(
      @PathVariable(value = "queueName") final String queueName) throws Exception {
    log.info("Flushing queue: '" + queueName + "'");
    int count = rabbit().flushQueue(queueName);
    return ResponseEntity.ok(count);
  }

  @RequestMapping(value = "/rabbit/get/{queueName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> get(
      @PathVariable(value = "queueName") final String queueName, @RequestParam String timeout)
      throws Exception {

    log.info("Getting from queue: '" + queueName + "' with timeout of '" + timeout + "'");

    String messageBody = rabbit().getMessage(queueName, TimeoutParser.parseTimeoutString(timeout));

    if (messageBody == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(messageBody);
  }

  @RequestMapping(value = "/rabbit/get/object/{queueName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> get(
      @PathVariable(value = "queueName") final String queueName,
      @RequestParam String clazzName,
      @RequestParam String timeout)
      throws Exception {

    log.info(
        "Getting from queue: '"
            + queueName
            + "' and converting to an object of type '"
            + clazzName
            + "', with timeout of '"
            + timeout
            + "'");

    // Read message as object
    Class<?> clazz = Class.forName(clazzName);
    Object resultAsObject =
        rabbit().getMessage(queueName, clazz, TimeoutParser.parseTimeoutString(timeout));

    // Bail out if no object read from queue.
    if (resultAsObject == null) {
      return ResponseEntity.notFound().build();
    }

    // Convert object to string
    String messageBody = mapper.writeValueAsString(resultAsObject);

    if (messageBody == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(messageBody);
  }

  @RequestMapping(value = "/rabbit/close", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> close(String queueName) throws Exception {
    log.info("Closing Rabbit connection");
    rabbit().close();
    return ResponseEntity.ok("Connection closed");
  }

  private RabbitHelper rabbit() throws CTPException {
    return RabbitHelper.instance(RABBIT_EXCHANGE, false);
  }
}
