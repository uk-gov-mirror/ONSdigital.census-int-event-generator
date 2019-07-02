package uk.gov.ons.ctp.integration.event.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.annotation.IntegrationComponentScan;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.event.generator.service.impl.PublishingService;

@SpringBootApplication
@IntegrationComponentScan("uk.gov.ons.ctp.integration")
@ComponentScan(basePackages = {"uk.gov.ons.ctp.integration"})
@ImportResource("springintegration/main.xml")
public class EventGenerator implements ApplicationRunner {

  private static Logger LOG = LoggerFactory.getLogger(EventGenerator.class);

  @Autowired PublishingService publishingService;


  @Value("${queueconfig.event-exchange}")
  private String eventExchange;
  
  public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(EventGenerator.class, args);
    ctx.close();
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    ObjectMapper om = new ObjectMapper();
    String fileName = null;
    String payloadType = null;

    List<String> nonOptArgs = args.getNonOptionArgs();
    for (String arg:nonOptArgs) {
      if (arg.startsWith("file=")) {
        fileName = arg.substring(5);
      } else if (arg.startsWith("type=")) {
        payloadType = arg.substring(5);
      }
    }
    
    if (fileName==null || payloadType==null) {
      System.out.println("Usage : file=overrides.csv type=[CollectionCase]");
      return;
    }

    Class<? extends EventPayload> payloadClass = null;
    switch (payloadType.toUpperCase()) {
      case "COLLECTIONCASE":
        payloadClass = CollectionCase.class;
        break;
      default:
        throw new Exception("Unhandled type");
    }
    
    List<EventPayload> payloads = publishingService.process(fileName, payloadClass);
  }

  /**
   * Custom Object Mapper
   *
   * @return a customer object mapper
   */
  @Bean
  @Primary
  public CustomObjectMapper customObjectMapper() {
    return new CustomObjectMapper();
  }

  
  /**
   * Bean used to publish asynchronous event messages
   *
   * @param connectionFactory RabbitMQ connection settings and strategies
   * @return the event publisher
   */
  @Bean
  public EventPublisher eventPublisher(final ConnectionFactory connectionFactory) {
    final var template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    template.setExchange("events");
    template.setChannelTransacted(true);

    return new EventPublisher(template);
  }

  
  

}
