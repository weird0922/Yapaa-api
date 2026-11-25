package pe.upn.yapaa_api.service;
import org.junit.jupiter.api.Test;import java.math.BigDecimal;import static org.junit.jupiter.api.Assertions.*;
class TarifaDeliveryTest {
 @Test void basePorDistanciaYRedondeo(){assertEquals(new BigDecimal("4.00"),DeliveryService.tarifa(0));assertEquals(new BigDecimal("4.00"),DeliveryService.tarifa(2000));assertEquals(new BigDecimal("4.50"),DeliveryService.tarifa(2001));assertEquals(new BigDecimal("5.00"),DeliveryService.tarifa(3000));assertEquals(new BigDecimal("7.00"),DeliveryService.tarifa(5000));assertEquals(new BigDecimal("8.00"),DeliveryService.tarifa(6000));}
 @Test void datosNoFinitosOLejanosSeRechazan(){for(double n:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,6001})assertThrows(pe.upn.yapaa_api.exception.ReglaNegocioException.class,()->DeliveryService.tarifa(n));}
}
