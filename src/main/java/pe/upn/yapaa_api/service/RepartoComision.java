package pe.upn.yapaa_api.service;
import java.math.*;
import java.util.*;
public final class RepartoComision {
 private RepartoComision(){}
 // Distribucion proporcional al centimo: la suma de los productos coincide con la comision cobrada.
 public static List<BigDecimal> repartir(List<BigDecimal> bases,BigDecimal total){
  var suma=bases.stream().reduce(BigDecimal.ZERO,BigDecimal::add);var partes=new ArrayList<BigDecimal>();var residuos=new ArrayList<BigDecimal>();
  for(var base:bases){var exacto=suma.signum()==0?BigDecimal.ZERO:base.multiply(total).divide(suma,12,RoundingMode.HALF_UP);var piso=exacto.setScale(2,RoundingMode.DOWN);partes.add(piso);residuos.add(exacto.subtract(piso));}
  int restantes=total.subtract(partes.stream().reduce(BigDecimal.ZERO,BigDecimal::add)).movePointRight(2).intValueExact();
  var orden=new ArrayList<Integer>();for(int i=0;i<bases.size();i++)orden.add(i);orden.sort(Comparator.<Integer,BigDecimal>comparing(residuos::get).reversed().thenComparingInt(i->i));
  for(int i=0;i<restantes;i++){int indice=orden.get(i%orden.size());partes.set(indice,partes.get(indice).add(new BigDecimal("0.01")));}return partes;
 }
}
