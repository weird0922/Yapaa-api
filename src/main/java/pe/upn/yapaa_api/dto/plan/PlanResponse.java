package pe.upn.yapaa_api.dto.plan;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PlanResponse(Integer idPlan, String nombre, BigDecimal porcentajeComision,
        String descripcion, boolean reportes, LocalDate fechaInicio, LocalDate fechaFin) {}
