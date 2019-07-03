package uk.gov.ons.ctp.integration.event.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import uk.gov.ons.ctp.common.event.EventSender;
import uk.gov.ons.ctp.common.event.RabbitConnectionDetails;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

public class NativeRabbitEventSender implements EventSender {

  Connection connection;
  Channel channel;

  ObjectMapper objectMapper;

  public NativeRabbitEventSender(RabbitConnectionDetails connectionDetails) throws Exception {
    objectMapper = new ObjectMapper();

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(connectionDetails.getHost());
    factory.setPort(connectionDetails.getPort());
    factory.setUsername(connectionDetails.getUser());
    factory.setPassword(connectionDetails.getPassword());
    connection = null;
    try {
      connection = factory.newConnection();
      channel = connection.createChannel();
      channel.exchangeDeclare("events", "topic", true);
    } catch (Exception e) {
      throw e;
    }
  }

  @Override
  public void close() throws Exception {
    connection.close();
  }

  @Override
  public void sendEvent(String routingKey, GenericEvent genericEvent) throws Exception {
    channel.basicPublish("events", routingKey, null, objectMapper.writeValueAsString(genericEvent).getBytes("UTF-8"));
  }

}
