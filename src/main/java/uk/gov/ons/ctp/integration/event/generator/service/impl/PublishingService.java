package uk.gov.ons.ctp.integration.event.generator.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.EventPayload;

@Service
public class PublishingService {

  @Autowired
  private EventPublisher publisher;
  @Autowired
  private TemplatingService templatingService;

  private Map<Class<? extends EventPayload>, String> routingMap = new HashMap<>();

  @PostConstruct
  public void setupRoutingMap() {
    routingMap.put(CollectionCase.class, "event.case.update");
  }


  public List<EventPayload> process(String fileName, Class<? extends EventPayload> payloadClass)
      throws Exception {
    List<EventPayload> payloads = new ArrayList<>();
    List<Map<String, String>> contexts = readFromCsv(fileName);

    for (Map<String, String> context : contexts) {
      payloads.add(publish(context, payloadClass));
    }
    return payloads;
  }


  private EventPayload publish(Map<String, String> context,
      Class<? extends EventPayload> payloadClass) throws Exception {
    String routingKey = routingMap.get(payloadClass);
    if (routingKey == null) {
      throw new Exception("Unhandled payload");
    }

    EventPayload payload = templatingService.generatePayload(context, payloadClass);
    publisher.sendEvent(routingKey, payload);
    return payload;
  }


  private List<Map<String, String>> readFromCsv(String filename) throws Exception {
    List<Map<String, String>> contexts = new ArrayList<>();

    List<String> header = new ArrayList<>();
    List<String> record = new ArrayList<>();
    try (Scanner scanner = new Scanner(new File(filename));) {
      var line = 0;
      while (scanner.hasNextLine()) {
        record = getRecordFromLine(scanner.nextLine());
        if (line == 0) {
          header = record;
        } else {
          Map<String, String> context = new HashMap<>();
          var col = 0;
          for (String key : header) {
            context.put(key, record.get(col));
            col++;
          }
          contexts.add(context);
        }
        line++;
      }
    }

    return contexts;
  }

  private List<String> getRecordFromLine(String line) {
    List<String> values = new ArrayList<String>();
    try (Scanner rowScanner = new Scanner(line)) {
      rowScanner.useDelimiter(",");
      while (rowScanner.hasNext()) {
        values.add(rowScanner.next());
      }
    }
    return values;
  }
}
