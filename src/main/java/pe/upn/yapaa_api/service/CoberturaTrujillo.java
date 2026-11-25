package pe.upn.yapaa_api.service;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
public final class CoberturaTrujillo {
 private CoberturaTrujillo(){}
 // Zona urbana inicial; no equivale a toda la provincia.
 public static boolean contiene(double lat,double lon){return Double.isFinite(lat)&&Double.isFinite(lon)&&lat>=-8.22&&lat<=-7.98&&lon>=-79.13&&lon<=-78.94;}
 public static void exigir(double lat,double lon){if(!contiene(lat,lon))throw new ReglaNegocioException("Selecciona una dirección dentro de la cobertura urbana de Trujillo.");}
 public static double metros(double a,double b,double c,double d){double x=Math.sin(Math.toRadians(c-a)/2),y=Math.sin(Math.toRadians(d-b)/2);return 6371000*2*Math.asin(Math.min(1,Math.sqrt(x*x+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*y*y)));}
}
