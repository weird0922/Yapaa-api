package pe.upn.yapaa_api.service;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import pe.upn.yapaa_api.exception.ServicioExternoException;
import java.nio.file.*;
import java.util.UUID;
@Service
public class CorreoOtpService {
 private final ObjectProvider<JavaMailSender> remitente;
 @Value("${yapaa.correo.modo:smtp}") private String modo;
 @Value("${yapaa.demo.habilitado:false}") private boolean demo;
 @Value("${spring.mail.username:}") private String correo;
 public CorreoOtpService(ObjectProvider<JavaMailSender> r){remitente=r;}
 public boolean esLocal(){return demo && "local".equals(modo);}
 public void enviar(String destino,String codigo){
  if(esLocal()){
   try{Path dir=Path.of("config","buzon-local");Files.createDirectories(dir);Files.writeString(dir.resolve(UUID.randomUUID()+".txt"),"PRUEBA LOCAL. Este mensaje no se envio por correo.\nPara: "+destino+"\nCodigo: "+codigo+"\nVigencia: 1 minuto.\n",StandardOpenOption.CREATE_NEW);return;}
   catch(Exception ex){throw new ServicioExternoException("No pudimos preparar el codigo de prueba. Intenta nuevamente.");}
  }
  JavaMailSender sender=remitente.getIfAvailable();
  if(sender==null||correo.isBlank())throw new ServicioExternoException("El envio de correos aun no esta configurado. Contacta a YAPAA.");
  var mensaje=new SimpleMailMessage();mensaje.setFrom(correo);mensaje.setTo(destino);mensaje.setSubject("Verifica tu correo en YAPAA");
  mensaje.setText("Tu codigo de verificacion de YAPAA es: "+codigo+"\nVence en 1 minuto y solo puede utilizarse una vez.\nSi no solicitaste este codigo, ignora este mensaje.");
  try{sender.send(mensaje);}catch(Exception ex){throw new ServicioExternoException("No pudimos enviar el codigo. Revisa tu correo e intentalo mas tarde.");}
 }
}
