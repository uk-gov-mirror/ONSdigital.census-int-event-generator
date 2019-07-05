package uk.gov.ons.ctp.integration.event.generator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RabbitConnectionDetails {
  private String host;
  private Integer port;
  private String user;
  private String password;
}
