package pe.upn.yapaa_api.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ValidacionRegistroTest {
 @Test void nombresRealesConAcentosYGuiones(){assertEquals("Maria-Jose O'Connor",ValidacionRegistro.nombre("  Maria-Jose   O'Connor  "));assertEquals("Peña",ValidacionRegistro.nombre("Peña"));assertThrows(RuntimeException.class,()->ValidacionRegistro.nombre("Juan123"));}
 @Test void celularesYClavesNoTruncanDatos(){assertThrows(RuntimeException.class,()->ValidacionRegistro.telefono("123456789"));assertThrows(RuntimeException.class,()->ValidacionRegistro.clave("1234567890","otra"));assertThrows(RuntimeException.class,()->ValidacionRegistro.clave("á".repeat(40),"á".repeat(40)));ValidacionRegistro.clave("una frase suficientemente larga","una frase suficientemente larga");}
 @Test void correoNormalizadoSinControlesNiEspacios(){assertEquals("usuario+prueba@example.com",VerificacionCorreoService.normalizar(" Usuario+Prueba@Example.COM "));assertThrows(RuntimeException.class,()->VerificacionCorreoService.normalizar("a@b.com\r\nBCC:x@example.com"));}
}
