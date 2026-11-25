package pe.upn.yapaa_api.service;
import org.junit.jupiter.api.*;import com.sun.net.httpserver.HttpServer;import java.net.InetSocketAddress;import java.nio.charset.StandardCharsets;import java.time.Clock;import pe.upn.yapaa_api.security.*;import pe.upn.yapaa_api.model.Cuenta;import static org.mockito.Mockito.*;import static org.junit.jupiter.api.Assertions.*;
class MapasProveedorTest {
 HttpServer servidor;UsuarioActual usuario;MapasService mapas;Cuenta cuenta;
 @BeforeEach void iniciar() throws Exception {
  servidor=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);servidor.createContext("/",e->{var datos="{\"features\":[{\"geometry\":{\"coordinates\":[-79.04,-8.12]},\"properties\":{\"street\":\"Avenida Larco\",\"city\":\"Trujillo\",\"osm_value\":\"primary\"}},{\"geometry\":{\"coordinates\":[-77.03,-12.04]},\"properties\":{\"street\":\"Fuera de Trujillo\"}}]}".getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(200,datos.length);e.getResponseBody().write(datos);e.close();});servidor.start();
  usuario=mock(UsuarioActual.class);cuenta=new Cuenta();cuenta.setIdCuenta(10);when(usuario.obtenerCuenta()).thenReturn(cuenta);when(usuario.obtenerCorreo()).thenReturn("cliente@example.test");mapas=new MapasService("http://127.0.0.1:"+servidor.getAddress().getPort(),usuario,new LimiteIntentos(Clock.systemUTC()));
 }
 @AfterEach void detener(){servidor.stop(0);}
 @Test void filtraCoberturaYVinculaSeleccionALaCuenta(){var datos=mapas.buscar("Larco");assertEquals(1,datos.size());var p=datos.getFirst();assertEquals(p,mapas.validar(p.token()));var otra=new Cuenta();otra.setIdCuenta(20);when(usuario.obtenerCuenta()).thenReturn(otra);assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->mapas.validar(p.token()));}
 @Test void inversaMantieneElPuntoExactoYRechazaCoordenadasFuera(){var p=mapas.inversa(-8.1201,-79.0401);assertEquals(-8.1201,p.latitud());assertEquals(-79.0401,p.longitud());assertTrue(p.direccion().contains("Larco"));assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->mapas.inversa(-12.04,-77.03));}
}
