package uk.gov.ons.ctp.integration.event.generator.endpoint;

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
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.rabbit.RabbitInteraction;
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

  @RequestMapping(value = "/rabbit/create/{eventType}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> createQueue(
      @PathVariable(value = "eventType") final String eventTypeAsString) throws Exception {

    log.info("Creating queue for events of type: '" + eventTypeAsString + "'");

    EventType eventType = EventType.valueOf(eventTypeAsString);

    RabbitInteraction rabbit = RabbitInteraction.instance(RABBIT_EXCHANGE);
    String queueName = rabbit.createQueue(eventType);

    return ResponseEntity.ok(queueName);
  }

  @RequestMapping(value = "/rabbit/flush/{queueName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<Integer> flushQueue(
      @PathVariable(value = "queueName") final String queueName) throws Exception {

    log.info("Flushing queue: '" + queueName + "'");

    RabbitInteraction rabbit = RabbitInteraction.instance(RABBIT_EXCHANGE);

    int count = rabbit.flushQueue(queueName);

    return ResponseEntity.ok(count);
  }

  @RequestMapping(value = "/rabbit/get/{queueName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> get(
      @PathVariable(value = "queueName") final String queueName, @RequestParam String timeout)
      throws Exception {

    log.info("Getting from queue: '" + queueName + "' with timeout of '" + timeout + "'");

    RabbitInteraction rabbit = RabbitInteraction.instance(RABBIT_EXCHANGE);
    String messageBody = rabbit.getMessage(queueName, TimeoutParser.parseTimeoutString(timeout));

    if (messageBody == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(messageBody);
  }

  @RequestMapping(value = "/rabbit/close", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> close(String queueName) throws Exception {

    log.info("Closing Rabbit connection");

    RabbitInteraction rabbit = RabbitInteraction.instance(RABBIT_EXCHANGE);
    rabbit.close();

    return ResponseEntity.ok("Connection closed");
  }
}
