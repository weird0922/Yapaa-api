package pe.upn.yapaa_api.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import java.security.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
@Service
public class VerificacionCorreoService {
 private final JdbcTemplate jdbc;private final CorreoOtpService correo;private final Clock reloj;private final SecureRandom azar=new SecureRandom();
 @Value("${yapaa.jwt.secreto}") private String secreto;
 public VerificacionCorreoService(JdbcTemplate j,CorreoOtpService c,Clock r){jdbc=j;correo=c;reloj=r;}
 public static String normalizar(String valor){String c=valor==null?"":valor.strip().toLowerCase(Locale.ROOT);if(c.length()>254||!c.matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+")||c.startsWith(".")||c.contains(".."))throw new ReglaNegocioException("Ingresa un correo valido.");return c;}
 private LocalDateTime ahora(){return LocalDateTime.ofInstant(reloj.instant(),reloj.getZone());}
 private record Fila(String hash,String sal,LocalDateTime vence,LocalDateTime enviado,LocalDateTime ventana,int envios,int intentos,boolean consumido){}
 private List<Fila> buscar(String c){return jdbc.query("SELECT * FROM dbo.verificacion_correo WITH (UPDLOCK,HOLDLOCK) WHERE correo=?",(r,i)->new Fila(r.getString("codigo_hash"),r.getString("sal"),r.getTimestamp("vence").toLocalDateTime(),r.getTimestamp("enviado").toLocalDateTime(),r.getTimestamp("ventana").toLocalDateTime(),r.getInt("envios"),r.getInt("intentos"),r.getBoolean("consumido")),c);}
 @Transactional public boolean solicitar(String valor){
  String c=normalizar(valor);var filas=buscar(c);LocalDateTime t=ahora(),ventana=t;int envios=1;
  if(!filas.isEmpty()){var f=filas.getFirst();if(f.enviado().plusSeconds(60).isAfter(t))throw new ReglaNegocioException("Espera 60 segundos antes de solicitar otro codigo.");if(f.ventana().plusHours(1).isAfter(t)){ventana=f.ventana();envios=f.envios()+1;}if(envios>5)throw new ReglaNegocioException("Has solicitado demasiados codigos. Intenta dentro de una hora.");}
  String codigo=String.format("%06d",azar.nextInt(1000000)),sal=UUID.randomUUID().toString(),hash=firmar(c+":"+codigo+":"+sal);
  if(filas.isEmpty())jdbc.update("INSERT INTO dbo.verificacion_correo(correo,codigo_hash,sal,vence,enviado,ventana,envios,intentos,consumido) VALUES(?,?,?,?,?,?,?,0,0)",c,hash,sal,Timestamp.valueOf(t.plusMinutes(1)),Timestamp.valueOf(t),Timestamp.valueOf(ventana),envios);
  else jdbc.update("UPDATE dbo.verificacion_correo SET codigo_hash=?,sal=?,vence=?,enviado=?,ventana=?,envios=?,intentos=0,token_hash=NULL,token_vence=NULL,consumido=0 WHERE correo=?",hash,sal,Timestamp.valueOf(t.plusMinutes(1)),Timestamp.valueOf(t),Timestamp.valueOf(ventana),envios,c);
  correo.enviar(c,codigo);return correo.esLocal();
 }
 @Transactional(noRollbackFor=ReglaNegocioException.class) public String verificar(String valor,String codigo){
  String c=normalizar(valor);var filas=buscar(c);if(filas.isEmpty())throw new ReglaNegocioException("Solicita un codigo nuevo.");var f=filas.getFirst();
  if(f.consumido()||f.intentos()>=5||(!f.vence().isAfter(ahora())||!f.enviado().plusMinutes(1).isAfter(ahora())))throw new ReglaNegocioException("El codigo vencio o alcanzo su limite de intentos. Solicita otro.");
  jdbc.update("UPDATE dbo.verificacion_correo SET intentos=intentos+1 WHERE correo=?",c);
  if(codigo==null||!codigo.matches("[0-9]{6}")||!MessageDigest.isEqual(f.hash().getBytes(StandardCharsets.UTF_8),firmar(c+":"+codigo+":"+f.sal()).getBytes(StandardCharsets.UTF_8)))throw new ReglaNegocioException("El codigo no es correcto.");
  byte[] aleatorio=new byte[32];azar.nextBytes(aleatorio);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
  jdbc.update("UPDATE dbo.verificacion_correo SET intentos=5,token_hash=?,token_vence=? WHERE correo=?",firmar(token),Timestamp.valueOf(ahora().plusMinutes(15)),c);return token;
 }
 @Transactional public void consumir(String valor,String token){
  String c=normalizar(valor);if(token==null||token.length()!=43)throw new ReglaNegocioException("Verifica tu correo antes de crear la cuenta.");
  int n=jdbc.update("UPDATE dbo.verificacion_correo SET consumido=1 WHERE correo=? AND token_hash=? AND token_vence>? AND consumido=0",c,firmar(token),Timestamp.valueOf(ahora()));
  if(n!=1)throw new ReglaNegocioException("La verificacion no es valida o vencio. Verifica nuevamente tu correo.");
 }
 private String firmar(String valor){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(valor.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("No se pudo validar el codigo.");}}
}
