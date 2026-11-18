package pe.upn.yapaa_api.service;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import pe.upn.yapaa_api.dto.auth.*;
import pe.upn.yapaa_api.dto.negocio.ConsultaRucResponse;
import pe.upn.yapaa_api.model.*;
import pe.upn.yapaa_api.repository.*;
import pe.upn.yapaa_api.security.JwtService;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties={"yapaa.demo.habilitado=false","spring.jpa.show-sql=false"})
@Transactional
class AfiliacionIntegracionTest {
    @Autowired AuthService auth; @Autowired NegocioRepository negocios;
    @Autowired CuentaRepository cuentas; @Autowired ProductoRepository productos;
    @Autowired CategoriaRepository categorias; @Autowired PagoRepository pagos; @Autowired ComisionRepository comisiones;
    @Autowired RolRepository roles; @Autowired JwtService jwt;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired WebApplicationContext contexto; @Autowired FilterChainProxy filtro;
    @MockitoBean ConsultaRucService consultaRuc;
    MockMvc mvc;
    @BeforeEach void preparar(){
        mvc=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        when(consultaRuc.consultar(anyString())).thenAnswer(inv->new ConsultaRucResponse(inv.getArgument(0),"RAZON SOCIAL CONSULTADA S.A.C.","ACTIVO","HABIDO",true,false));
    }
    @AfterEach void limpiar(){SecurityContextHolder.clearContext();}
    private String correo(){return "integracion-"+UUID.randomUUID()+"@example.test";}
    private String ruc(){return "20"+String.format("%09d",Integer.toUnsignedLong(UUID.randomUUID().hashCode())%1000000000L);}
    private LoginResponse cliente(){
        var r=new RegistroRequest();r.setCorreo(correo());r.setContrasena("PruebaSegura2026");
        r.setNombres("Cliente");r.setApellidos("Temporal");r.setTelefono("900000001");r.setRol("CLIENTE");
        return auth.registrar(r).getDatos();
    }
    private RegistroNegocioRequest solicitud(){
        var r=new RegistroNegocioRequest();r.setCorreo(correo());r.setContrasena("PruebaSegura2026");
        r.setRuc(ruc());r.setTipoContribuyente("PERSONA_JURIDICA");r.setNombreComercial("Local de integracion");
        r.setRazonSocial("VALOR NO CONFIABLE DEL NAVEGADOR");r.setDireccion("Direccion ficticia");
        r.setDistrito("Trujillo");r.setCiudad("Trujillo");r.setTelefonoContacto("900000001");return r;
    }
    private Negocio negocioDe(LoginResponse s){
        return negocios.findByCuentaIdCuenta(s.getIdCuenta(),org.springframework.data.domain.Pageable.unpaged()).getContent().getFirst();
    }
    private Producto producto(Negocio n){
        n.setEstadoAprobacion("APROBADO");negocios.save(n);
        var p=new Producto();p.setNegocio(n);p.setCategoria(categorias.findAll().getFirst());p.setNombre("Producto temporal "+UUID.randomUUID());
        p.setPrecioNormal(new java.math.BigDecimal("15"));p.setPrecioDescuento(new java.math.BigDecimal("9.90"));
        p.setStock(3);p.setEstadoPublicacion("PUBLICADO");p.setActivo(true);p.setFechaPublicacion(LocalDateTime.now());
        p.setFechaLimiteVenta(LocalDateTime.now().plusHours(4));return productos.save(p);
    }
    @Test void cuentaNegocioEsIndependienteYNoConvierteAlCliente(){
        var cliente=cliente();var nueva=auth.registrarNegocio(solicitud()).getDatos();
        assertNotEquals(cliente.getIdCuenta(),nueva.getIdCuenta());
        assertEquals(List.of("NEGOCIO"),nueva.getRoles());
        assertEquals(1,cuentas.findById(cliente.getIdCuenta()).orElseThrow().getRoles().size());
        assertEquals("CLIENTE",cuentas.findById(cliente.getIdCuenta()).orElseThrow().getRoles().iterator().next().getNombre());
        assertEquals("RAZON SOCIAL CONSULTADA S.A.C.",negocioDe(nueva).getRazonSocial());
        assertEquals("PENDIENTE",negocioDe(nueva).getEstadoAprobacion());
    }
    @Test void noPermiteReutilizarElCorreoDelClienteComoCuentaNegocio(){
        var cliente=cliente();var request=solicitud();request.setCorreo(cliente.getCorreo());
        assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->auth.registrarNegocio(request));
        assertEquals("CLIENTE",cuentas.findById(cliente.getIdCuenta()).orElseThrow().getRoles().iterator().next().getNombre());
    }
    @Test void negocioSoloVeSusProductosYNoPuedeComprarNiConsultarCatalogo() throws Exception{
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);var p=producto(n);
        mvc.perform(get("/api/productos/mios").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk());
        mvc.perform(get("/api/productos/disponibles").header("Authorization","Bearer "+s.getToken())).andExpect(status().isForbidden());
        mvc.perform(post("/api/pedidos").header("Authorization","Bearer "+s.getToken()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        var otro=auth.registrarNegocio(solicitud()).getDatos();var ajeno=producto(negocioDe(otro));
        mvc.perform(patch("/api/productos/"+ajeno.getIdProducto()+"/activo").param("activo","false").header("Authorization","Bearer "+s.getToken())).andExpect(status().isForbidden());
        mvc.perform(patch("/api/productos/"+p.getIdProducto()+"/activo").param("activo","false").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk());
        assertFalse(p.getActivo());
    }
    @Test void clienteRegistraPedidoConWhatsAppSinCrearPagoNiComision() throws Exception{
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);var p=producto(n);var cliente=cliente();
        long pagosAntes=pagos.count(), comisionesAntes=comisiones.count();
        String cuerpo="{\"grupos\":[{\"idNegocio\":"+n.getIdNegocio()+",\"modalidadEntrega\":\"RECOJO\",\"costoDelivery\":0,\"items\":[{\"idProducto\":"+p.getIdProducto()+",\"cantidad\":2}]}]}";
        String respuesta=mvc.perform(post("/api/pedidos").header("Authorization","Bearer "+cliente.getToken()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertTrue(respuesta.contains("https://wa.me/"));
        assertTrue(respuesta.contains("PENDIENTE"));
        assertEquals(pagosAntes,pagos.count());assertEquals(comisionesAntes,comisiones.count());
        Integer id=p.getIdProducto();em.flush();em.clear();
        assertEquals(1,productos.findById(id).orElseThrow().getStock());
        mvc.perform(patch("/api/pedidos/1/pago").header("Authorization","Bearer "+cliente.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"metodoPago\":\"EFECTIVO\"}")).andExpect(status().isForbidden());
    }
    @Test void soloElAdministradorApruebaLaNuevaCuentaNegocio() throws Exception{
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);
        mvc.perform(patch("/api/negocios/"+n.getIdNegocio()+"/aprobacion").param("estado","APROBADO").header("Authorization","Bearer "+s.getToken())).andExpect(status().isForbidden());
        var admin=new Cuenta();admin.setCorreo(correo());admin.setContrasenaHash("hash-de-prueba");admin.setEstado("ACTIVO");admin.getRoles().add(roles.findByNombre("ADMIN").orElseThrow());cuentas.save(admin);
        String token=jwt.generarToken(admin.getCorreo(),List.of("ADMIN"));
        mvc.perform(patch("/api/negocios/"+n.getIdNegocio()+"/aprobacion").param("estado","APROBADO").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        assertEquals("APROBADO",n.getEstadoAprobacion());
    }

    @Autowired PlanService planes; @Autowired NegocioPlanRepository vinculos;
    @Autowired PedidoNegocioRepository ventas; @Autowired PedidoRepository pedidos;
    private PedidoNegocio pedidoTemporal(Negocio n,Producto p,LoginResponse c) throws Exception {
        String cuerpo="{\"grupos\":[{\"idNegocio\":"+n.getIdNegocio()+",\"modalidadEntrega\":\"DELIVERY\",\"direccionEntrega\":\"Direccion temporal\",\"costoDelivery\":7,\"items\":[{\"idProducto\":"+p.getIdProducto()+",\"cantidad\":2}]}]}";
        mvc.perform(post("/api/pedidos").header("Authorization","Bearer "+c.getToken()).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isCreated());
        return ventas.findByNegocioIdNegocio(n.getIdNegocio(),org.springframework.data.domain.Pageable.unpaged()).stream().max(java.util.Comparator.comparing(PedidoNegocio::getIdPedidoNegocio)).orElseThrow();
    }
    @Test void nuevaAfiliacionTienePromocionYLaVentaGeneraUnaSolaComisionSinDelivery() throws Exception {
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);var p=producto(n);
        var plan=vinculos.findFirstByNegocioIdNegocioAndEstadoOrderByFechaInicioDesc(n.getIdNegocio(),"ACTIVO").orElseThrow();
        assertEquals("Promocional",plan.getPlan().getNombre());assertNotNull(plan.getFechaFin());
        var v=pedidoTemporal(n,p,cliente());long antes=comisiones.count(),pagosAntes=pagos.count();
        mvc.perform(post("/api/ventas/"+v.getIdPedidoNegocio()+"/confirmar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk());
        var c=comisiones.findByPedidoNegocioIdPedidoNegocio(v.getIdPedidoNegocio()).orElseThrow();
        assertEquals(0,new java.math.BigDecimal("19.80").compareTo(c.getBaseCalculo()));
        assertEquals(0,new java.math.BigDecimal("1.98").compareTo(c.getMontoComision()));
        assertEquals("PENDIENTE",c.getEstado());assertEquals("COMPLETADO",v.getEstado());
        assertEquals("COMPLETADO",v.getPedido().getEstadoPedido());
        mvc.perform(post("/api/ventas/"+v.getIdPedidoNegocio()+"/confirmar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk());
        assertEquals(antes+1,comisiones.count());assertEquals(pagosAntes,pagos.count());
        mvc.perform(post("/api/ventas/"+v.getIdPedidoNegocio()+"/cancelar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isBadRequest());
    }
    @Test void planesNormalYProAplicanTasasYRestringenReportesEnServidor() throws Exception {
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);var p=producto(n);var cliente=cliente();
        mvc.perform(put("/api/negocios/"+n.getIdNegocio()+"/plan").header("Authorization","Bearer "+s.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Normal\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/negocios/"+n.getIdNegocio()+"/indicadores").header("Authorization","Bearer "+s.getToken())).andExpect(status().isForbidden());
        mvc.perform(get("/api/negocios/"+n.getIdNegocio()+"/reporte.csv").header("Authorization","Bearer "+s.getToken())).andExpect(status().isForbidden());
        var v=pedidoTemporal(n,p,cliente);
        mvc.perform(post("/api/ventas/"+v.getIdPedidoNegocio()+"/confirmar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk());
        assertEquals(0,new java.math.BigDecimal("2.97").compareTo(comisiones.findByPedidoNegocioIdPedidoNegocio(v.getIdPedidoNegocio()).orElseThrow().getMontoComision()));
        mvc.perform(put("/api/negocios/"+n.getIdNegocio()+"/plan").header("Authorization","Bearer "+s.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Pro\"}")).andExpect(status().isOk());
        // Cambiar de plan no altera comisiones de ventas anteriores.
        assertEquals(0,new java.math.BigDecimal("15").compareTo(comisiones.findByPedidoNegocioIdPedidoNegocio(v.getIdPedidoNegocio()).orElseThrow().getPorcentajeAplicado()));
        var segundo=pedidoTemporal(n,producto(n),cliente);
        mvc.perform(post("/api/ventas/"+segundo.getIdPedidoNegocio()+"/confirmar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk());
        assertEquals(0,new java.math.BigDecimal("3.96").compareTo(comisiones.findByPedidoNegocioIdPedidoNegocio(segundo.getIdPedidoNegocio()).orElseThrow().getMontoComision()));
        String resumen=mvc.perform(get("/api/negocios/"+n.getIdNegocio()+"/indicadores").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(resumen.contains("\"ventasConfirmadas\":2"));
        String csv=mvc.perform(get("/api/negocios/"+n.getIdNegocio()+"/reporte.csv").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(csv.contains("comision;estado_comision"));assertTrue(csv.contains("3.96"));
        mvc.perform(put("/api/negocios/"+n.getIdNegocio()+"/plan").header("Authorization","Bearer "+s.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Promocional\"}")).andExpect(status().isBadRequest());
    }
    @Test void otroNegocioYClienteNoAccedenAVentasPlanesNiReportesAjenos() throws Exception {
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);var v=pedidoTemporal(n,producto(n),cliente());
        var otro=auth.registrarNegocio(solicitud()).getDatos();var cliente=cliente();
        for(String token:List.of(otro.getToken(),cliente.getToken())){
            mvc.perform(get("/api/negocios/"+n.getIdNegocio()+"/ventas").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
            mvc.perform(put("/api/negocios/"+n.getIdNegocio()+"/plan").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Pro\"}")).andExpect(status().isForbidden());
            mvc.perform(post("/api/ventas/"+v.getIdPedidoNegocio()+"/confirmar").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
            mvc.perform(get("/api/negocios/"+n.getIdNegocio()+"/reporte.csv").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/negocios/"+n.getIdNegocio()+"/plan")).andExpect(status().isUnauthorized());
    }
    @Test void cancelarDevuelveStockUnaSolaVezYNoGeneraComision() throws Exception {
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);var p=producto(n);var idProducto=p.getIdProducto();var v=pedidoTemporal(n,p,cliente());var id=v.getIdPedidoNegocio();
        em.flush();em.clear();assertEquals(1,productos.findById(idProducto).orElseThrow().getStock());
        for(int i=0;i<2;i++)mvc.perform(post("/api/ventas/"+id+"/cancelar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk());
        em.flush();em.clear();assertEquals(3,productos.findById(idProducto).orElseThrow().getStock());assertTrue(comisiones.findByPedidoNegocioIdPedidoNegocio(id).isEmpty());
        mvc.perform(post("/api/ventas/"+id+"/confirmar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isBadRequest());
    }
    @Test void promocionVencidaPasaANormalSinReiniciarse() {
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);
        var vinculo=vinculos.findFirstByNegocioIdNegocioAndEstadoOrderByFechaInicioDesc(n.getIdNegocio(),"ACTIVO").orElseThrow();
        vinculo.setFechaInicio(java.time.LocalDate.now().minusMonths(2));vinculo.setFechaFin(java.time.LocalDate.now().minusDays(1));vinculos.saveAndFlush(vinculo);
        var actual=planes.vigente(n.getIdNegocio());assertEquals("Normal",actual.getPlan().getNombre());assertEquals("FINALIZADO",vinculo.getEstado());
        assertEquals(actual.getIdNegocioPlan(),planes.vigente(n.getIdNegocio()).getIdNegocioPlan());
    }
}
