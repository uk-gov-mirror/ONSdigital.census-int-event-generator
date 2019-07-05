package uk.gov.ons.ctp.integration.event.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.annotation.IntegrationComponentScan;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.EventSender;
import uk.gov.ons.ctp.common.event.SpringRabbitEventSender;
import uk.gov.ons.ctp.common.event.model.EventPayload;

@SpringBootApplication
@IntegrationComponentScan("uk.gov.ons.ctp.integration")
@ComponentScan(basePackages = {"uk.gov.ons.ctp.integration"})
@ImportResource("springintegration/main.xml")
public class CommandLineEventGenerator implements ApplicationRunner {

  public static final String ARG_FILE = "file";
  public static final String ARG_EVENT_TYPE = "eventType";
  public static final String ARG_SOURCE = "source";
  public static final String ARG_CHANNEL = "channel";

  private EventPublisher eventPublisher;
  private EventGenerator eventGenerator;

  private static Logger LOG = LoggerFactory.getLogger(CommandLineEventGenerator.class);

  public static void main(String[] args) {
    ConfigurableApplicationContext ctx =
        SpringApplication.run(CommandLineEventGenerator.class, args);
    ctx.close();
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    ObjectMapper om = new ObjectMapper();
    String fileName = null;
    EventType eventType = null;
    Source source = null;
    Channel channel = null;

    List<String> nonOptArgs = args.getNonOptionArgs();
    for (String arg : nonOptArgs) {
      if (arg.startsWith(ARG_FILE)) {
        fileName = arg.substring(ARG_FILE.length() + 1);
      } else if (arg.startsWith(ARG_EVENT_TYPE)) {
        eventType = EventType.valueOf(arg.substring(ARG_EVENT_TYPE.length() + 1));
      } else if (arg.startsWith(ARG_SOURCE)) {
        source = Source.valueOf(arg.substring(ARG_SOURCE.length() + 1));
      } else if (arg.startsWith(ARG_CHANNEL)) {
        channel = Channel.valueOf(arg.substring(ARG_CHANNEL.length() + 1));
      }
    }

    if (fileName == null || eventType == null || source == null || channel == null) {
      System.out.println(
          "Args example: file=overrides.csv eventType=CASE_CREATED source=CASE_SERVICE channel=RM");
      return;
    }

    Class<? extends EventPayload> payloadClass = eventType.getPayloadType();
    if (payloadClass == null) {
      System.err.println("eventType not yet supported");
      System.exit(1);
    }

    List<EventPayload> payloads =
        eventGenerator.process(eventType, source, channel, fileName, payloadClass);

    List<Map<String, String>> results = new ArrayList<>();
    for (EventPayload payload : payloads) {
      Map<String, String> result = new HashMap<>();
      om.valueToTree(payload)
          .fields()
          .forEachRemaining(node -> mapAppender(result, node, new ArrayList<String>()));
      results.add(result);
    }

    results
        .get(0)
        .forEach(
            (k, v) -> {
              System.out.print(k + ",");
            });
    System.out.println();
    results.forEach(
        m -> {
          m.forEach(
              (k, v) -> {
                System.out.print(v + ",");
              });
          System.out.println();
        });
  }

  @PostConstruct
  public void init() {
    this.eventGenerator = new EventGenerator(eventPublisher);
  }

  /**
   * Bean used to publish asynchronous event messages
   *
   * @param connectionFactory RabbitMQ connection settings and strategies
   */
  @Autowired
  public void createEventPublisher(final ConnectionFactory connectionFactory) {
    final var template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    template.setExchange("events");
    template.setChannelTransacted(true);

    EventSender sender = new SpringRabbitEventSender(template);
    eventPublisher = new EventPublisher(sender);
  }

  private void processArrayNode(ArrayNode arrayNode) {
    for (JsonNode jsonNode : arrayNode) {
      processObjectNode(jsonNode);
    }
  }

  private void processObjectNode(JsonNode jsonNode) {
    Map<String, String> result = new HashMap<>();
    Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.fields();
    iterator.forEachRemaining(node -> mapAppender(result, node, new ArrayList<String>()));
  }

  private void mapAppender(
      Map<String, String> result, Map.Entry<String, JsonNode> node, List<String> names) {
    names.add(node.getKey());
    if (node.getValue().isTextual()) {
      String name = names.stream().collect(Collectors.joining("."));
      result.put(name, node.getValue().asText());
    } else if (node.getValue().isArray()) {
      processArrayNode((ArrayNode) node.getValue());
    } else if (node.getValue().isNull()) {
      String name = names.stream().collect(Collectors.joining("."));
      result.put(name, null);
    } else {
      node.getValue()
          .fields()
          .forEachRemaining(nested -> mapAppender(result, nested, new ArrayList<>(names)));
    }
  }
}
