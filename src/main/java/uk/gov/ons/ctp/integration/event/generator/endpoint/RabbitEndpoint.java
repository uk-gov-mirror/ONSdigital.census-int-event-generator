package uk.gov.ons.ctp.integration.event.generator.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
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

    RabbitHelper rabbit = RabbitHelper.instance(RABBIT_EXCHANGE);
    String queueName = rabbit.createQueue(eventType);

    return ResponseEntity.ok(queueName);
  }

  @RequestMapping(value = "/rabbit/flush/{queueName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<Integer> flushQueue(
      @PathVariable(value = "queueName") final String queueName) throws Exception {

    log.info("Flushing queue: '" + queueName + "'");

    RabbitHelper rabbit = RabbitHelper.instance(RABBIT_EXCHANGE);

    int count = rabbit.flushQueue(queueName);

    return ResponseEntity.ok(count);
  }

  /**
   * Note that this endpoint is only provided for manual testing of the RabbitHelper.sendEvent(). It
   * is not intended to be used.
   *
   * <p>It basically duplicates the whole point of the EventGenerator, and only works with limited
   * fixed data.
   */
  @RequestMapping(value = "/rabbit/send", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> sendEvent() throws Exception {

    log.info("Sending event: '");

    RabbitHelper rabbit = RabbitHelper.instance(RABBIT_EXCHANGE);

    SurveyLaunchedResponse surveryLaunched =
        SurveyLaunchedResponse.builder()
            .questionnaireId("q123")
            .caseId(UUID.randomUUID())
            .agentId("x123")
            .build();

    String transactionId =
        rabbit.sendEvent(
            EventType.SURVEY_LAUNCHED, Source.RESPONDENT_HOME, Channel.RH, surveryLaunched);

    return ResponseEntity.ok(transactionId);
  }

  @RequestMapping(value = "/rabbit/get/{queueName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> get(
      @PathVariable(value = "queueName") final String queueName, @RequestParam String timeout)
      throws Exception {

    log.info("Getting from queue: '" + queueName + "' with timeout of '" + timeout + "'");

    RabbitHelper rabbit = RabbitHelper.instance(RABBIT_EXCHANGE);
    String messageBody = rabbit.getMessage(queueName, TimeoutParser.parseTimeoutString(timeout));

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
    RabbitHelper rabbit = RabbitHelper.instance(RABBIT_EXCHANGE);
    Object resultAsObject =
        rabbit.getMessage(queueName, clazz, TimeoutParser.parseTimeoutString(timeout));

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

    RabbitHelper rabbit = RabbitHelper.instance(RABBIT_EXCHANGE);
    rabbit.close();

    return ResponseEntity.ok("Connection closed");
  }
}
