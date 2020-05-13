package uk.gov.ons.ctp.integration.event.generator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.EventPayload;

public class EventGenerator {
  private static final Logger log = LoggerFactory.getLogger(EventGenerator.class);

  private EventPublisher publisher;

  public EventGenerator(EventPublisher publisher) {
    this.publisher = publisher;
  }

  public List<EventPayload> process(
      EventType eventType,
      Source source,
      Channel channel,
      String fileName,
      Class<? extends EventPayload> payloadClass)
      throws Exception {
    List<Map<String, String>> contexts = readFromCsv(fileName);
    return process(eventType, source, channel, contexts, payloadClass);
  }

  public List<EventPayload> process(
      EventType eventType,
      Source source,
      Channel channel,
      List<Map<String, String>> contexts,
      Class<? extends EventPayload> payloadClass)
      throws Exception {
    List<EventPayload> payloads = new ArrayList<>();
    for (Map<String, String> context : contexts) {
      EventPayload payload = publish(eventType, source, channel, context, payloadClass);
      payloads.add(payload);
    }
    return payloads;
  }

  private EventPayload publish(
      EventType eventType,
      Source source,
      Channel channel,
      Map<String, String> context,
      Class<? extends EventPayload> payloadClass)
      throws Exception {
    EventPayload payload = generatePayload(context, payloadClass);
    publisher.sendEvent(eventType, source, channel, payload);
    return payload;
  }

  private List<Map<String, String>> readFromCsv(String filename) throws Exception {
    List<Map<String, String>> contexts = new ArrayList<>();

    List<String> header = new ArrayList<>();
    List<String> record = new ArrayList<>();
    try (Scanner scanner = new Scanner(new File(filename)); ) {
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
    JsonNode node = loadObjectNode(payloadType);
    for (Map.Entry<String, String> entry : context.entrySet()) {
      String value;
      if (entry.getValue() == null) {
        value = null;
      } else if (entry.getValue().startsWith("#")) {
        switch (entry.getValue().substring(1)) {
          case "uuid":
            value = UUID.randomUUID().toString();
            break;
          default:
            throw new Exception("Unknown value token : " + entry.getValue());
        }
      } else {
        value = entry.getValue();
      }
      setJsonNodeValue(node, entry.getKey(), value);
    }

    EventPayload payload = mapper.treeToValue(node, payloadClass);
    return payload;
  }

  private void setJsonNodeValue(JsonNode node, String key, String value) throws Exception {
    String[] parts = key.split("\\.");
    if (parts.length > 1) {
      String remainder = String.join(".", Arrays.copyOfRange(parts, 1, parts.length));
      JsonNode targetNode = node.get(parts[0]);
      if (targetNode == null) {
        throw new Exception(
            "Error: Child node '" + parts[0] + "' does not exist for " + "key '" + key + "'");
      }
      setJsonNodeValue(targetNode, remainder, value);
    } else {
      ObjectNode objectNode = (ObjectNode) node;
      if (!objectNode.has(key)) {
        throw new Exception("Error: Attempting to set value for unknown node: " + key);
      }
      objectNode.put(key, value);
    }
  }

  private JsonNode loadObjectNode(final String qualifier) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    JsonNode jsonNode = null;
    String path = generatePath(qualifier);
    try {
      InputStream inStream = new ClassPathResource(path).getInputStream();
      jsonNode = (JsonNode) mapper.readTree(inStream);
    } catch (Throwable t) {
      log.info("Problem loading fixture {} reason {}", path, t.getMessage());
      throw t;
    }
    return jsonNode;
  }

  private String generatePath(final String qualifier) {
    return "template/" + qualifier + ".json";
  }
}
