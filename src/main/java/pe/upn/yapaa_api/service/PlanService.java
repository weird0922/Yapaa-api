package pe.upn.yapaa_api.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.model.*;
import pe.upn.yapaa_api.repository.*;
import pe.upn.yapaa_api.dto.plan.PlanResponse;
import pe.upn.yapaa_api.exception.*;
import pe.upn.yapaa_api.security.UsuarioActual;
import java.time.*;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PlanService {
    private final PlanComercialRepository planes;
    private final NegocioPlanRepository vinculos;
    private final NegocioRepository negocios;
    private final UsuarioActual usuario;
    private final Clock reloj;
    public PlanService(PlanComercialRepository p, NegocioPlanRepository v, NegocioRepository n, UsuarioActual u, Clock reloj){
        planes=p;vinculos=v;negocios=n;usuario=u;this.reloj=reloj;
    }
    @Transactional(readOnly=true)
    public List<PlanResponse> disponibles(){
        return planes.findByActivo(true,org.springframework.data.domain.Pageable.unpaged()).stream()
            .filter(p->List.of("Normal","Pro").contains(p.getNombre())).map(p->respuesta(p,null)).toList();
    }
    @Transactional
    public void prepararCatalogo(){
        crearSiFalta("Promocional","10.00","Primer mes de afiliacion: comision promocional del 10%.");
        crearSiFalta("Normal","15.00","Productos, pedidos y consulta de comisiones.");
        crearSiFalta("Pro","20.00","Incluye indicadores de ventas, unidades y exportacion CSV.");
        for(Negocio n:negocios.findAll()) vigente(n.getIdNegocio());
    }
    private void crearSiFalta(String nombre,String tasa,String descripcion){
        if(!planes.existsByNombre(nombre)){
            var p=new PlanComercial();p.setNombre(nombre);p.setPorcentajeComision(new BigDecimal(tasa));p.setDescripcion(descripcion);p.setActivo(true);planes.save(p);
        }
    }
    @Transactional
    public NegocioPlan vigente(Integer id){
        Negocio n=negocios.bloquear(id).orElseThrow(()->new RecursoNoEncontradoException("Negocio no encontrado."));
        LocalDate hoy=LocalDate.now(reloj);
        var actual=vinculos.findFirstByNegocioIdNegocioAndEstadoOrderByFechaInicioDesc(id,"ACTIVO");
        if(actual.isPresent()){
            NegocioPlan v=actual.get();
            // Las afiliaciones promocionales antiguas tambien terminan tras su primer mes.
            if("Promocional".equals(v.getPlan().getNombre()) && v.getFechaFin()==null)
                v.setFechaFin(v.getFechaInicio().plusMonths(1).minusDays(1));
            if(v.getFechaInicio().isAfter(hoy)) throw new ReglaNegocioException("El plan aun no ha comenzado.");
            if(v.getFechaFin()==null || !v.getFechaFin().isBefore(hoy)) return v;
            v.setEstado("FINALIZADO");vinculos.saveAndFlush(v);
            return asignar(n,plan("Normal"),hoy,null);
        }
        LocalDate inicio=n.getFechaRegistro()==null?hoy:n.getFechaRegistro().toLocalDate();
        LocalDate fin=inicio.plusMonths(1).minusDays(1);
        return asignar(n,plan(fin.isBefore(hoy)?"Normal":"Promocional"),hoy,fin.isBefore(hoy)?null:fin);
    }
    @Transactional
    public PlanResponse consultar(Integer id){ autorizar(id);var v=vigente(id);return respuesta(v.getPlan(),v); }
    @Transactional
    public PlanResponse cambiar(Integer id,String nombre){
        autorizar(id);
        if(!List.of("Normal","Pro").contains(nombre))throw new ReglaNegocioException("Selecciona Normal o Pro.");
        var actual=vigente(id);var nuevo=plan(nombre);
        if(actual.getPlan().getIdPlan().equals(nuevo.getIdPlan()))return respuesta(nuevo,actual);
        actual.setEstado("FINALIZADO");actual.setFechaFin(LocalDate.now(reloj));vinculos.saveAndFlush(actual);
        var v=asignar(actual.getNegocio(),nuevo,LocalDate.now(reloj),null);
        return respuesta(nuevo,v);
    }
    public void autorizar(Integer id){usuario.verificarNegocio(negocios.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Negocio no encontrado.")));}
    @Transactional
    public void exigirPro(Integer id){autorizar(id);if(!"Pro".equals(vigente(id).getPlan().getNombre()))throw new org.springframework.security.access.AccessDeniedException("Los indicadores y reportes requieren el Plan Pro.");}
    private PlanComercial plan(String nombre){return planes.findByNombre(nombre).filter(p->Boolean.TRUE.equals(p.getActivo())).orElseThrow(()->new ReglaNegocioException("Plan no disponible: "+nombre));}
    private NegocioPlan asignar(Negocio n,PlanComercial p,LocalDate inicio,LocalDate fin){
        var v=new NegocioPlan();v.setNegocio(n);v.setPlan(p);v.setFechaInicio(inicio);v.setFechaFin(fin);v.setPorcentajeAplicado(p.getPorcentajeComision());v.setEstado("ACTIVO");return vinculos.saveAndFlush(v);
    }
    private PlanResponse respuesta(PlanComercial p,NegocioPlan v){return new PlanResponse(p.getIdPlan(),p.getNombre(),v==null?p.getPorcentajeComision():v.getPorcentajeAplicado(),p.getDescripcion(),"Pro".equals(p.getNombre()),v==null?null:v.getFechaInicio(),v==null?null:v.getFechaFin());}
}
