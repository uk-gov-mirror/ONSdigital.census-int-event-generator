package uk.gov.ons.ctp.integration.event.generator.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.event.model.EventPayload;

@Service
public class TemplatingService {

  public EventPayload generatePayload(
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

  /** */
  private static ObjectNode loadObjectNode(final String qualifier) throws Exception {
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

  /** */
  private static String generatePath(final String qualifier) {
    return "template/" + qualifier + ".json";
  }
}
