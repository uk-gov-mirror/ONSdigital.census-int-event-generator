package uk.gov.ons.ctp.integration.event.generator.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.integration.event.generator.EventGenerator;
import uk.gov.ons.ctp.integration.event.generator.model.GeneratorRequest;
import uk.gov.ons.ctp.integration.event.generator.model.GeneratorResponse;

@RestController
@RequestMapping(produces = "application/json")
public class GeneratorEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(GeneratorEndpoint.class);

  @Autowired EventGenerator eventGenerator;

  @RequestMapping(value = "/generate", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<GeneratorResponse> generate(@Valid @RequestBody GeneratorRequest request)
      throws CTPException {
    log.with(request.getEventType())
        .with(request.getSource())
        .with(request.getChannel())
        .info("create events");
    Class<? extends EventPayload> payloadClass = request.getEventType().getPayloadType();
    if (payloadClass == null) {
      throw new CTPException(Fault.BAD_REQUEST, "eventType not yet supported");
    }

    List<EventPayload> payloads = null;
    try {
      payloads =
          eventGenerator.process(
              request.getEventType(),
              request.getSource(),
              request.getChannel(),
              request.getContexts(),
              payloadClass);
    } catch (Exception e) {
      log.error("Event generation failed", e);
      throw new CTPException(
          Fault.SYSTEM_ERROR, "Failed to generate events. " + "Cause: " + e.getMessage());
    }

    GeneratorResponse response = new GeneratorResponse();
    response.setPayloads(payloads);

    log.info("Event generation completed successfully");
    return ResponseEntity.ok(response);
  }
}
