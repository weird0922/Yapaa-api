package pe.upn.yapaa_api.service;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class RepartoComisionTest {
 @Test void centavosSeConservanConTresLineas(){
  var resultado=RepartoComision.repartir(List.of(new BigDecimal("1"),new BigDecimal("1"),new BigDecimal("1")),new BigDecimal("0.01"));
  assertEquals(List.of(new BigDecimal("0.01"),new BigDecimal("0.00"),new BigDecimal("0.00")),resultado);
 }
 @Test void repartoProporcionalNoCambiaLaComisionTotal(){
  var resultado=RepartoComision.repartir(List.of(new BigDecimal("19.80"),new BigDecimal("7.50"),new BigDecimal("4.90")),new BigDecimal("4.83"));
  assertEquals(new BigDecimal("4.83"),resultado.stream().reduce(BigDecimal.ZERO,BigDecimal::add));
  assertTrue(resultado.stream().allMatch(n->n.scale()==2&&n.signum()>=0));
 }
}
