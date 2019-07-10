package uk.gov.ons.ctp.integration.event.generator.model;

import java.util.List;
import java.util.Map;
import lombok.Data;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;

@Data
public class GeneratorRequest {
  private EventType eventType;
  private Source source;
  private Channel channel;

  private List<Map<String, String>> contexts;
}
