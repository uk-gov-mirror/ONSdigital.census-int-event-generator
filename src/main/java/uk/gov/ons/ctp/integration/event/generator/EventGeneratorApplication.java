package uk.gov.ons.ctp.integration.event.generator;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.annotation.IntegrationComponentScan;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventSender;
import uk.gov.ons.ctp.common.event.SpringRabbitEventSender;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

@SpringBootApplication
@IntegrationComponentScan("uk.gov.ons.ctp.integration")
@ComponentScan(basePackages = {"uk.gov.ons.ctp.integration"})
@ImportResource("springintegration/main.xml")
public class EventGeneratorApplication {

  public static void main(final String[] args) {
    SpringApplication.run(EventGeneratorApplication.class, args);
  }

  @Bean
  public EventGenerator eventGenerator(EventPublisher eventPublisher) {
    return new EventGenerator(eventPublisher);
  }

  /**
   * Bean used to publish asynchronous event messages
   *
   * @param connectionFactory RabbitMQ connection settings and strategies
   */
  @Bean
  public EventPublisher eventPublisher(final ConnectionFactory connectionFactory) {
    final var template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    template.setExchange("events");
    template.setChannelTransacted(true);

    EventSender sender = new SpringRabbitEventSender(template);
    return new EventPublisher(sender);
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
}
