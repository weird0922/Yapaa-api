package pe.upn.yapaa_api.service;
import org.junit.jupiter.api.Test;import com.sun.net.httpserver.HttpServer;import java.net.InetSocketAddress;import java.nio.charset.StandardCharsets;import static org.junit.jupiter.api.Assertions.*;
class RutasProveedorTest {
 @Test void interpretaDistanciaDelProveedorYSnapping() throws Exception {
  var servidor=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  servidor.createContext("/",e->{assertEquals("identity",e.getRequestHeaders().getFirst("Accept-Encoding"));assertEquals("identity",e.getRequestHeaders().getFirst("Accept-Encoding"));assertTrue(e.getRequestURI().toString().contains("/route/v1/driving/"));var datos="{\"code\":\"Ok\",\"routes\":[{\"distance\":2482.8,\"duration\":264.9,\"weight\":264.9}],\"waypoints\":[{\"distance\":23.7},{\"distance\":10.5}]}".getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(200,datos.length);e.getResponseBody().write(datos);e.close();});servidor.start();
  try{var rutas=new RutasService("http://127.0.0.1:"+servidor.getAddress().getPort());assertEquals(2482.8,rutas.distancia(-8.105,-79.031,-8.12,-79.04));}finally{servidor.stop(0);}
 }
}
