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
    @MockitoBean CorreoOtpService correoOtp;
    @MockitoBean RutasService rutas;
    @MockitoBean MapasService mapas;
    @Autowired VerificacionCorreoService verificacion;
    java.util.Map<String,String> codigos=new java.util.HashMap<>();
    private String verificarCorreo(String correo){verificacion.solicitar(correo);return verificacion.verificar(correo,codigos.get(correo));}
    MockMvc mvc;
    @BeforeEach void preparar(){
        doAnswer(inv->{codigos.put(inv.getArgument(0),inv.getArgument(1));return null;}).when(correoOtp).enviar(anyString(),anyString());
        when(rutas.distancia(anyDouble(),anyDouble(),anyDouble(),anyDouble())).thenReturn(5000.0);
        when(mapas.validar(anyString())).thenReturn(new MapasService.Punto("punto","Avenida Larco 500, Trujillo",-8.12,-79.04));
        mvc=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        when(consultaRuc.consultar(anyString())).thenAnswer(inv->new ConsultaRucResponse(inv.getArgument(0),"RAZON SOCIAL CONSULTADA S.A.C.","ACTIVO","HABIDO",true,false));
    }
    @AfterEach void limpiar(){SecurityContextHolder.clearContext();}
    private String correo(){return "integracion-"+UUID.randomUUID()+"@example.test";}
    private String ruc(){return "20"+String.format("%09d",Integer.toUnsignedLong(UUID.randomUUID().hashCode())%1000000000L);}
    private LoginResponse cliente(){
        var r=new RegistroRequest();r.setCorreo(correo());r.setContrasena("PruebaSegura2026");r.setConfirmarContrasena("PruebaSegura2026");r.setTokenVerificacion(verificarCorreo(r.getCorreo()));
        r.setNombres("Cliente");r.setApellidos("Temporal");r.setTelefono("900000001");r.setRol("CLIENTE");
        return auth.registrar(r).getDatos();
    }
    private RegistroNegocioRequest solicitud(){
        var r=new RegistroNegocioRequest();r.setCorreo(correo());r.setContrasena("PruebaSegura2026");r.setConfirmarContrasena("PruebaSegura2026");r.setTokenVerificacion(verificarCorreo(r.getCorreo()));
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
        assertEquals(3,productos.findById(id).orElseThrow().getStock());
        mvc.perform(patch("/api/pedidos/1/pago").header("Authorization","Bearer "+cliente.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"metodoPago\":\"EFECTIVO\"}")).andExpect(status().isForbidden());
    }
    @Test void soloElAdministradorApruebaLaNuevaCuentaNegocio() throws Exception{
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);
        mvc.perform(patch("/api/negocios/"+n.getIdNegocio()+"/aprobacion").param("estado","APROBADO").header("Authorization","Bearer "+s.getToken())).andExpect(status().isForbidden());
        var admin=cuentas.findByCorreo("admin@admin.com").orElseThrow();
        String token=jwt.generarToken(admin.getCorreo(),List.of("ADMIN"));
        mvc.perform(patch("/api/negocios/"+n.getIdNegocio()+"/aprobacion").param("estado","APROBADO").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        assertEquals("APROBADO",n.getEstadoAprobacion());
    }

    @Autowired DeliveryService delivery;
    @Autowired DireccionesService direcciones;
    @Autowired PlanService planes; @Autowired NegocioPlanRepository vinculos;
    @Autowired PedidoNegocioRepository ventas; @Autowired PedidoRepository pedidos;
    private PedidoNegocio pedidoTemporal(Negocio n,Producto p,LoginResponse c) throws Exception {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM ubicacion_negocio WHERE id_negocio=?",Integer.class,n.getIdNegocio())==0)jdbc.update("INSERT INTO ubicacion_negocio(id_negocio,direccion,latitud,longitud) VALUES(?,?,?,?)",n.getIdNegocio(),"Origen temporal",-8.11,-79.03);
        Integer dir=jdbc.queryForObject("INSERT INTO direccion_cliente(id_cuenta,nombre,direccion,latitud,longitud,predeterminada) OUTPUT INSERTED.id_direccion VALUES(?,?,?,?,?,0)",Integer.class,c.getIdCuenta(),"Temporal","Avenida Larco 500, Trujillo",-8.12,-79.04);
        acceder(c);var cotizacion=delivery.cotizar(n.getIdNegocio(),dir);
        String cuerpo="{\"grupos\":[{\"idNegocio\":"+n.getIdNegocio()+",\"modalidadEntrega\":\"DELIVERY\",\"cotizacionDelivery\":\""+cotizacion.id()+"\",\"direccionEntrega\":\"FALSA\",\"costoDelivery\":0,\"items\":[{\"idProducto\":"+p.getIdProducto()+",\"cantidad\":2}]}]}";
        mvc.perform(post("/api/pedidos").header("Authorization","Bearer "+c.getToken()).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isCreated());
        return ventas.findByNegocioIdNegocio(n.getIdNegocio(),org.springframework.data.domain.Pageable.unpaged()).stream().max(java.util.Comparator.comparing(PedidoNegocio::getIdPedidoNegocio)).orElseThrow();
    }
    private void pagar(PedidoNegocio venta,LoginResponse cliente) throws Exception {
        mvc.perform(post("/api/pedidos/"+venta.getPedido().getIdPedido()+"/pago-simulado").header("Authorization","Bearer "+cliente.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"escenario\":\"APROBADO\"}")).andExpect(status().isOk());
    }
    @Test void nuevaAfiliacionTienePromocionYLaVentaGeneraUnaSolaComisionSinDelivery() throws Exception {
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);var p=producto(n);
        var plan=vinculos.findFirstByNegocioIdNegocioAndEstadoOrderByFechaInicioDesc(n.getIdNegocio(),"ACTIVO").orElseThrow();
        assertEquals("Promocional",plan.getPlan().getNombre());assertNotNull(plan.getFechaFin());
        var comprador=cliente();var v=pedidoTemporal(n,p,comprador);long antes=comisiones.count(),pagosAntes=pagos.count();
        mvc.perform(post("/api/ventas/"+v.getIdPedidoNegocio()+"/confirmar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isBadRequest());
        pagar(v,comprador);
        mvc.perform(post("/api/ventas/"+v.getIdPedidoNegocio()+"/confirmar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk());
        var c=comisiones.findByPedidoNegocioIdPedidoNegocio(v.getIdPedidoNegocio()).orElseThrow();
        assertEquals(0,new java.math.BigDecimal("19.80").compareTo(c.getBaseCalculo()));
        assertEquals(0,new java.math.BigDecimal("1.98").compareTo(c.getMontoComision()));
        assertEquals("LIQUIDADA",c.getEstado());assertEquals("COMPLETADO",v.getEstado());
        assertEquals("COMPLETADO",v.getPedido().getEstadoPedido());
        mvc.perform(post("/api/ventas/"+v.getIdPedidoNegocio()+"/confirmar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk());
        assertEquals(antes+1,comisiones.count());assertEquals(pagosAntes+1,pagos.count());
        mvc.perform(post("/api/ventas/"+v.getIdPedidoNegocio()+"/cancelar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isBadRequest());
    }
    @Test void planesNormalYProAplicanTasasYRestringenReportesEnServidor() throws Exception {
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);var p=producto(n);var cliente=cliente();
        mvc.perform(put("/api/negocios/"+n.getIdNegocio()+"/plan").header("Authorization","Bearer "+s.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Normal\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/negocios/"+n.getIdNegocio()+"/indicadores").header("Authorization","Bearer "+s.getToken())).andExpect(status().isForbidden());
        mvc.perform(get("/api/negocios/"+n.getIdNegocio()+"/reporte.csv").header("Authorization","Bearer "+s.getToken())).andExpect(status().isForbidden());
        var v=pedidoTemporal(n,p,cliente);pagar(v,cliente);
        mvc.perform(post("/api/ventas/"+v.getIdPedidoNegocio()+"/confirmar").header("Authorization","Bearer "+s.getToken())).andExpect(status().isOk());
        assertEquals(0,new java.math.BigDecimal("2.97").compareTo(comisiones.findByPedidoNegocioIdPedidoNegocio(v.getIdPedidoNegocio()).orElseThrow().getMontoComision()));
        mvc.perform(put("/api/negocios/"+n.getIdNegocio()+"/plan").header("Authorization","Bearer "+s.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Pro\"}")).andExpect(status().isOk());
        // Cambiar de plan no altera comisiones de ventas anteriores.
        assertEquals(0,new java.math.BigDecimal("15").compareTo(comisiones.findByPedidoNegocioIdPedidoNegocio(v.getIdPedidoNegocio()).orElseThrow().getPorcentajeAplicado()));
        var segundo=pedidoTemporal(n,producto(n),cliente);pagar(segundo,cliente);
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
    @Test void cancelarPendienteNoIncrementaStockNiGeneraComision() throws Exception {
        var s=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(s);var p=producto(n);var idProducto=p.getIdProducto();var v=pedidoTemporal(n,p,cliente());var id=v.getIdPedidoNegocio();
        em.flush();em.clear();assertEquals(3,productos.findById(idProducto).orElseThrow().getStock());
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

    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired DashboardService dashboard;
    private void acceder(LoginResponse s){SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(s.getCorreo(),null,s.getRoles().stream().map(r->new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+r)).toList()));}
    @Test void otpIncorrectoTieneCincoIntentosYNoSeReutiliza(){
        var c=correo();verificacion.solicitar(c);String correcto=codigos.get(c),incorrecto=correcto.equals("000000")?"111111":"000000";
        for(int i=0;i<5;i++)assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->verificacion.verificar(c,incorrecto));
        assertEquals(5,jdbc.queryForObject("SELECT intentos FROM verificacion_correo WHERE correo=?",Integer.class,c));
        assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->verificacion.verificar(c,correcto));
    }
    @Test void otpVerificadoSeConsumeUnaSolaVezYEstaVinculadoAlCorreo(){
        String c=correo(),token=verificarCorreo(c);
        assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->verificacion.verificar(c,codigos.get(c)));
        assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->verificacion.consumir(correo(),token));
        verificacion.consumir(c,token);
        assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->verificacion.consumir(c,token));
    }
    @Test void otpVencidoYReenvioInmediatoSeRechazan(){
        String c=correo();verificacion.solicitar(c);
        assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->verificacion.solicitar(c));
        jdbc.update("UPDATE verificacion_correo SET vence=DATEADD(minute,-1,GETDATE()) WHERE correo=?",c);
        assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->verificacion.verificar(c,codigos.get(c)));
    }
    @Test void nuevasCuentasRequierenContrasenasCoincidentesYCorreoVerificado(){
        var r=new RegistroRequest();r.setCorreo(correo());r.setContrasena("UnaClaveLarga2026");r.setConfirmarContrasena("OtraClaveLarga2026");r.setNombres("Maria Jose");r.setApellidos("Perez Diaz");r.setTelefono("999888777");
        assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->auth.registrar(r));
        r.setConfirmarContrasena(r.getContrasena());
        assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->auth.registrar(r));
    }
    @Test void pagoSimuladoRechazoReintentoAprobadoYRepeticionSinDuplicar() throws Exception {
        var negocio=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(negocio);var cliente=cliente();var producto=producto(n);var venta=pedidoTemporal(n,producto,cliente);var id=venta.getPedido().getIdPedido();
        String ruta="/api/pedidos/"+id+"/pago-simulado";long cantidad=pagos.count(),comisionesAntes=comisiones.count();
        mvc.perform(post(ruta).header("Authorization","Bearer "+cliente.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"escenario\":\"RECHAZADO\"}")).andExpect(status().isOk());
        assertEquals(3, productos.findById(producto.getIdProducto()).orElseThrow().getStock());
        for(int i=0;i<2;i++)mvc.perform(post(ruta).header("Authorization","Bearer "+cliente.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"escenario\":\"APROBADO\"}")).andExpect(status().isOk());
        assertEquals(1, productos.findById(producto.getIdProducto()).orElseThrow().getStock());
        assertEquals(cantidad+2,pagos.count());assertEquals(comisionesAntes+1,comisiones.count());
        var pago=pagos.findByPedidoIdPedido(id,org.springframework.data.domain.Pageable.unpaged()).stream().filter(a->"CONFIRMADO".equals(a.getEstadoPago())).findFirst().orElseThrow();
        assertTrue(pago.getReferencia().startsWith("SIM-"));assertEquals(0,venta.getPedido().getTotal().compareTo(pago.getMonto()));
        var ajeno=cliente();mvc.perform(post(ruta).header("Authorization","Bearer "+ajeno.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"escenario\":\"APROBADO\"}")).andExpect(status().isForbidden());
        mvc.perform(post(ruta).header("Authorization","Bearer "+negocio.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"escenario\":\"APROBADO\"}")).andExpect(status().isForbidden());
    }
    @Test void dashboardSoloAdministradorConDatosSeparadosYFechasValidas() throws Exception {
        var cliente=cliente();mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard").header("Authorization","Bearer "+cliente.getToken())).andExpect(status().isForbidden());
        var admin=cuentas.findByCorreo("admin@admin.com").orElseThrow();String token=jwt.generarToken(admin.getCorreo(),List.of("ADMIN"));
        String respuesta=mvc.perform(get("/api/dashboard").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(respuesta.contains("comisionYapaa"));assertTrue(respuesta.contains("totalCobrado"));assertTrue(respuesta.contains("negociosResumen"));
        mvc.perform(get("/api/dashboard").param("desde","2026-12-01").param("hasta","2026-01-01").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
    }

    @Test void anulacionDePagoSimuladoPermiteCancelarYNoDuplicaStock() throws Exception {
        var negocio=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(negocio);var cliente=cliente();var producto=producto(n);var venta=pedidoTemporal(n,producto,cliente);var id=venta.getPedido().getIdPedido();
        String ruta="/api/pedidos/"+id+"/pago-simulado";
        mvc.perform(post(ruta).header("Authorization","Bearer "+cliente.getToken()).contentType(MediaType.APPLICATION_JSON).content("{\"escenario\":\"APROBADO\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/ventas/"+venta.getIdPedidoNegocio()+"/cancelar").header("Authorization","Bearer "+negocio.getToken())).andExpect(status().isBadRequest());
        for(int i=0;i<2;i++)mvc.perform(post(ruta+"/anular").header("Authorization","Bearer "+cliente.getToken())).andExpect(status().isOk());
        assertEquals("REEMBOLSADO",venta.getPedido().getEstadoPago());
        assertEquals("CANCELADO",venta.getEstado());
        assertEquals(3,productos.findById(producto.getIdProducto()).orElseThrow().getStock());
        mvc.perform(post("/api/ventas/"+venta.getIdPedidoNegocio()+"/cancelar").header("Authorization","Bearer "+negocio.getToken())).andExpect(status().isOk());
        assertEquals("ANULADA",comisiones.findByPedidoNegocioIdPedidoNegocio(venta.getIdPedidoNegocio()).orElseThrow().getEstado());
    }
    @Test void codigoOtpTieneExactamenteUnMinutoDeVigencia(){
        String c=correo();verificacion.solicitar(c);
        assertEquals(60,jdbc.queryForObject("SELECT DATEDIFF(second,enviado,vence) FROM verificacion_correo WHERE correo=?",Integer.class,c));
    }

    @Test void pagoSumaAutomaticamenteEnDashboardYAnulacionLoRevierte() throws Exception {
        var negocio=auth.registrarNegocio(solicitud()).getDatos();var n=negocioDe(negocio);var cliente=cliente();var venta=pedidoTemporal(n,producto(n),cliente);
        var admin=cuentas.findByCorreo("admin@admin.com").orElseThrow();String token=jwt.generarToken(admin.getCorreo(),List.of("ADMIN"));
        var sesionAdmin=new LoginResponse();sesionAdmin.setCorreo(admin.getCorreo());sesionAdmin.setRoles(List.of("ADMIN"));acceder(sesionAdmin);
        var desde=java.time.LocalDate.now().withDayOfMonth(1);var hasta=java.time.LocalDate.now();var antes=dashboard.consultar(desde,hasta);
        pagar(venta,cliente);acceder(sesionAdmin);em.flush();
        var despues=dashboard.consultar(desde,hasta);
        assertEquals(0,new java.math.BigDecimal("26.80").compareTo(despues.totalCobrado().subtract(antes.totalCobrado())));
        assertEquals(0,new java.math.BigDecimal("1.98").compareTo(despues.comisionYapaa().subtract(antes.comisionYapaa())));
        assertEquals(0,despues.totalCobrado().compareTo(despues.comisionYapaa().add(despues.ingresoNegocios())));
        var detalle=dashboard.detalle(n.getIdNegocio(),desde,hasta,0);assertEquals(1,detalle.totalElementos());
        var v=detalle.ventas().getFirst();assertEquals(0,v.comision().compareTo(v.detalle().stream().map(DashboardService.ProductoVenta::comision).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add)));
        mvc.perform(patch("/api/comisiones/"+comisiones.findByPedidoNegocioIdPedidoNegocio(venta.getIdPedidoNegocio()).orElseThrow().getIdComision()+"/liquidar").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/pedidos/"+venta.getPedido().getIdPedido()+"/pago-simulado/anular").header("Authorization","Bearer "+cliente.getToken())).andExpect(status().isOk());
        acceder(sesionAdmin);em.flush();var finalizado=dashboard.consultar(desde,hasta);
        assertEquals(0,antes.comisionYapaa().compareTo(finalizado.comisionYapaa()));assertEquals(0,antes.totalCobrado().compareTo(finalizado.totalCobrado()));
    }

    @Test void direccionesSonPrivadasYConservanUnaPredeterminada() {
        var c=cliente();acceder(c);var a=direcciones.guardar("Casa","Puerta azul","punto",false);var b=direcciones.guardar("Trabajo","Piso 2","punto",false);
        assertTrue(a.predeterminada());assertFalse(b.predeterminada());direcciones.predeterminada(b.id());assertEquals(b.id(),direcciones.listar().getFirst().id());
        direcciones.eliminar(b.id());assertTrue(direcciones.propia(a.id()).predeterminada());
        acceder(cliente());assertThrows(pe.upn.yapaa_api.exception.RecursoNoEncontradoException.class,()->direcciones.propia(a.id()));
    }
    @Test void deliveryNoAceptaImporteDelNavegadorNiCotizacionAjenaOReutilizada() throws Exception {
        var n=negocioDe(auth.registrarNegocio(solicitud()).getDatos());var c=cliente();var p=producto(n);var venta=pedidoTemporal(n,p,c);
        assertEquals(0,new java.math.BigDecimal("7.00").compareTo(venta.getCostoDelivery()));assertTrue(venta.getDireccionEntrega().contains("Larco"));assertFalse(venta.getDireccionEntrega().contains("FALSA"));
        String id=jdbc.queryForObject("SELECT TOP 1 id FROM cotizacion_delivery WHERE id_cuenta=?",String.class,c.getIdCuenta());acceder(c);assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->delivery.consumir(n.getIdNegocio(),id));
        acceder(cliente());assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->delivery.consumir(n.getIdNegocio(),id));
    }
    @Test void noHayDeliverySinCotizacionNiFueraDeCobertura() throws Exception {
        var n=negocioDe(auth.registrarNegocio(solicitud()).getDatos());var c=cliente();var p=producto(n);
        String cuerpo="{\"grupos\":[{\"idNegocio\":"+n.getIdNegocio()+",\"modalidadEntrega\":\"DELIVERY\",\"direccionEntrega\":\"Una direccion\",\"costoDelivery\":1,\"items\":[{\"idProducto\":"+p.getIdProducto()+",\"cantidad\":1}]}]}";
        mvc.perform(post("/api/pedidos").header("Authorization","Bearer "+c.getToken()).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isBadRequest());
        assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->DeliveryService.tarifa(6001));assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->CoberturaTrujillo.exigir(-12.04,-77.03));
    }
}
