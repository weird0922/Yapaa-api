package pe.upn.yapaa_api.service;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import java.nio.charset.StandardCharsets;
public final class ValidacionRegistro {
 private ValidacionRegistro(){}
 public static void clave(String c,String confirmacion){if(c==null||c.length()<10||c.length()>64||c.getBytes(StandardCharsets.UTF_8).length>72||c.isBlank()||c.chars().anyMatch(Character::isISOControl))throw new ReglaNegocioException("Usa entre 10 y 64 caracteres para tu contrasena, sin caracteres de control.");if(!c.equals(confirmacion))throw new ReglaNegocioException("Las contrasenas no coinciden.");}
 public static String nombre(String n){String v=n==null?"":n.strip().replaceAll("\\s+"," ");if(v.length()<2||v.length()>100||!v.matches("[\\p{L}\\p{M}]+(?:[ '\u2019-][\\p{L}\\p{M}]+)*"))throw new ReglaNegocioException("Revisa tus nombres y apellidos. Usa letras, espacios, apostrofes o guiones.");return v;}
 public static String telefono(String n){if(n==null||!n.matches("9[0-9]{8}"))throw new ReglaNegocioException("Ingresa un celular peruano de 9 digitos que empiece con 9.");return n;}
}
