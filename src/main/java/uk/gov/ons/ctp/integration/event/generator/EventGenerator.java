package uk.gov.ons.ctp.integration.event.generator;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.EventPayload;

public class EventGenerator {

  private EventPublisher publisher;
  
  public EventGenerator(EventPublisher publisher) {
    this.publisher = publisher;
  }

  private static Map<Class<? extends EventPayload>, String> routingMap = new HashMap<>();
  static {
    routingMap.put(CollectionCase.class, "event.case.update");
  }

  public List<EventPayload> process(String fileName, Class<? extends EventPayload> payloadClass)
      throws Exception {
    List<Map<String, String>> contexts = readFromCsv(fileName);
    return process(contexts, payloadClass);
  }
  
  public List<EventPayload> process(List<Map<String, String>> contexts, Class<? extends EventPayload> payloadClass) throws Exception {
    List<EventPayload> payloads = new ArrayList<>();
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

    EventPayload payload = generatePayload(context, payloadClass);
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
  
  
  private EventPayload generatePayload(
      Map<String, String> context, Class<? extends EventPayload> payloadClass) throws Exception {

    ObjectMapper mapper = new ObjectMapper();
    String payloadType = null;
    payloadType = payloadClass.getSimpleName();
    ObjectNode json = loadObjectNode(payloadType);
    for (Map.Entry<String, String> entry : context.entrySet()) {
      String value = null;
      if (entry.getValue().startsWith("#")) {
        switch (entry.getValue().substring(1)) {
          case "uuid":
            value = UUID.randomUUID().toString();
            break;
          default:
            throw new Exception ("Unknown value token : " + entry.getValue());
        }
      } else {
        value = entry.getValue();
      }
      json.put(entry.getKey(), value);
    }

    EventPayload payload = mapper.treeToValue(json, payloadClass);
    return payload;
  }

  private ObjectNode loadObjectNode(final String qualifier) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    ObjectNode jsonNode = null;
    String path = generatePath(qualifier);
    try {
      InputStream inStream = new ClassPathResource(generatePath(qualifier)).getInputStream();
      jsonNode = (ObjectNode) mapper.readTree(inStream);
    } catch (Throwable t) {
      //      log.debug("Problem loading fixture {} reason {}", path, t.getMessage());
      throw t;
    }
    return jsonNode;
  }

  private String generatePath(final String qualifier) {
    return "template/" + qualifier + ".json";
  }
}
