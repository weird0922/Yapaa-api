package pe.upn.yapaa_api.dto.plan;
import java.math.BigDecimal;
public record ResumenVentas(long ventasConfirmadas,long unidadesVendidas,BigDecimal subtotalVendido,
    BigDecimal comisionGenerada,BigDecimal ingresoNetoProductos) {}
