package uk.gov.ons.ctp.integration.event.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.EventSender;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

public class NativeRabbitEventSender implements EventSender {

  Connection connection;
  Channel channel;

  ObjectMapper objectMapper;

  public NativeRabbitEventSender(RabbitConnectionDetails connectionDetails)
      throws TimeoutException, IOException {
    objectMapper = new ObjectMapper();

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(connectionDetails.getHost());
    factory.setPort(connectionDetails.getPort());
    factory.setUsername(connectionDetails.getUser());
    factory.setPassword(connectionDetails.getPassword());
    connection = null;
    connection = factory.newConnection();
    channel = connection.createChannel();
    channel.exchangeDeclare("events", "topic", true);
  }

  @Override
  public void close() throws IOException {
    connection.close();
  }

  @Override
  public void sendEvent(RoutingKey routingKey, GenericEvent genericEvent) throws Exception {
    channel.basicPublish(
        "events",
        routingKey.getKey(),
        null,
        objectMapper.writeValueAsString(genericEvent).getBytes("UTF-8"));
  }
}
