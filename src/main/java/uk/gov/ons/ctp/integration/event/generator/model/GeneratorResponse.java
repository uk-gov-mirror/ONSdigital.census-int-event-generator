package uk.gov.ons.ctp.integration.event.generator.model;

import java.util.List;
import lombok.Data;
import uk.gov.ons.ctp.common.event.model.EventPayload;

@Data
public class GeneratorResponse {
  private List<EventPayload> payloads;
}
