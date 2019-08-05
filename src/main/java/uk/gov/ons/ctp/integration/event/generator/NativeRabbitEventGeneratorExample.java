package uk.gov.ons.ctp.integration.event.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.NativeRabbitEventSender;
import uk.gov.ons.ctp.common.event.RabbitConnectionDetails;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.EventPayload;

public class NativeRabbitEventGeneratorExample {

  public static void main(String[] args) {
    RabbitConnectionDetails connectionDetails;
    try {
      connectionDetails = new RabbitConnectionDetails();
      connectionDetails.setHost("localhost");
      connectionDetails.setUsername("guest");
      connectionDetails.setPassword("guest");
      connectionDetails.setPort(35672);

      Map<String, String> entry1 = new HashMap<>();
      entry1.put("id", "#uuid");
      entry1.put("caseRef", "foo");

      Map<String, String> entry2 = new HashMap<>();
      entry2.put("id", "#uuid");
      entry2.put("caseRef", "bar");

      List<Map<String, String>> contexts = new ArrayList<>();
      contexts.add(entry1);
      contexts.add(entry2);

      NativeRabbitEventSender sender = new NativeRabbitEventSender(connectionDetails, "events");
      EventGenerator eventGenerator = new EventGenerator(new EventPublisher(sender));
      List<EventPayload> payloads =
          eventGenerator.process(
              EventType.CASE_CREATED,
              Source.CASE_SERVICE,
              Channel.RM,
              contexts,
              CollectionCase.class);
      ObjectMapper objectMapper = new ObjectMapper();
      System.out.println(objectMapper.writeValueAsString(payloads));

      sender.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
