package net.consensys.wittgenstein.server.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import net.consensys.wittgenstein.core.EnvelopeInfo;
import net.consensys.wittgenstein.core.External;
import net.consensys.wittgenstein.core.Node;
import net.consensys.wittgenstein.core.WParameters;
import net.consensys.wittgenstein.core.messages.SendMessage;
import net.consensys.wittgenstein.protocols.harmony.Harmony;
import net.consensys.wittgenstein.protocols.harmony.HarmonyConfig;
import net.consensys.wittgenstein.protocols.harmony.output.OutputInfo;
import net.consensys.wittgenstein.server.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@EnableAutoConfiguration
@RequestMapping("/w")
public class WServer extends ExternalWS implements IServer, External {
  private Server server = new Server();

  @PostMapping(value = "/harmony")
  public ResponseEntity runHarmony(@RequestBody HarmonyConfig config) {
    System.out.println(config.toString());
    try {
      OutputInfo outputInfo = Harmony.run(config);
      return ResponseEntity.ok().body(outputInfo);
    } catch (IOException e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @GetMapping(value = "/network/nodes")
  @Override
  public List<? extends Node> getNodeInfo() {
    return server.getNodeInfo();
  }

  @GetMapping(value = "/network/time")
  @Override
  public int getTime() {
    return server.getTime();
  }

  @GetMapping(value = "/protocols")
  @Override
  public List<String> getProtocols() {
    return server.getProtocols();
  }

  @GetMapping(value = "/protocols/{fullClassName}")
  @Override
  public WParameters getProtocolParameters(@PathVariable("fullClassName") String fullClassName) {
    return server.getProtocolParameters(fullClassName);
  }

  @PostMapping(value = "/network/init/{fullClassName}")
  public void init(
      @PathVariable("fullClassName") String fullClassName, @RequestBody WParameters parameters) {
    server.init(fullClassName, parameters);
  }

  @PostMapping(value = "/network/runMs/{ms}")
  @Override
  public void runMs(@PathVariable("ms") int ms) {
    server.runMs(ms);
  }

  @GetMapping(value = "/network/nodes/{nodeId}")
  @Override
  public Node getNodeInfo(@PathVariable("nodeId") int nodeId) {
    return server.getNodeInfo(nodeId);
  }

  @GetMapping(value = "/network/messages")
  @Override
  public List<EnvelopeInfo<?>> getMessages() {
    return server.getMessages();
  }

  @PostMapping(value = "/nodes/{nodeId}/start")
  @Override
  public void startNode(@PathVariable("nodeId") int nodeId) {
    server.startNode(nodeId);
  }

  @PostMapping(value = "/network/nodes/{nodeId}/stop")
  @Override
  public void stopNode(@PathVariable("nodeId") int nodeId) {
    server.stopNode(nodeId);
  }

  @PostMapping(value = "/network/nodes/{nodeId}/external")
  @Override
  public void setExternal(
      @PathVariable("nodeId") int nodeId, @RequestBody String externalServiceFullAddress) {
    server.setExternal(nodeId, externalServiceFullAddress);
  }

  @PostMapping(value = "/network/send")
  @Override
  public <TN extends Node> void sendMessage(@RequestBody SendMessage msg) {
    server.sendMessage(msg);
  }

  /** We map all fields in the parameters, not taking into account the getters/setters */
  @SuppressWarnings("unused")
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = ObjectMapperFactory.objectMapper();

    for (Class<?> p : server.getParametersName()) {
      mapper.registerSubtypes(new NamedType(p, p.getSimpleName()));
    }

    return mapper;
  }

  public static void main(String... args) {
    SpringApplication.run(WServer.class, args);
  }
}
