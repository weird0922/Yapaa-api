package pe.upn.yapaa_api.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pe.upn.yapaa_api.common.Result;

/**
 * Regla de errores de la API YAPAA.
 *
 * <p>Los mensajes que se envian al cliente describen la situacion desde el punto de vista del
 * usuario. Nunca se expone informacion tecnica (traza, sentencias SQL, nombre de clases o de la
 * base de datos). Ante un error no controlado se registra la causa en el log del servidor y el
 * cliente recibe un mensaje generico.</p>
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    private static final String MENSAJE_INTERNO =
            "Ocurrio un error inesperado en el servidor. Vuelve a intentarlo en unos instantes.";

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<Result<String>> manejarServicioExterno(ServicioExternoException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Result.error(ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Result<String>> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(ex.getMessage()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Result<String>> manejarReglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public ResponseEntity<Result<String>> manejarCuentaDeshabilitada() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error("Credenciales invalidas."));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<String>> manejarCredenciales(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error("Credenciales invalidas."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<String>> manejarAccesoDenegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error("No tiene permisos para realizar esta operacion."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Los datos enviados no son validos.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(mensaje));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<String>> manejarRestricciones(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violacion -> violacion.getMessage())
                .orElse("Los datos enviados no son validos.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(mensaje));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<String>> manejarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error("El parametro '" + ex.getName() + "' no tiene un valor valido."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<String>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error("El cuerpo de la solicitud no tiene un formato valido."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<String>> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.error("El metodo HTTP usado no esta permitido para esta direccion."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<String>> manejarRutaNoEncontrada(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error("La direccion solicitada no existe."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<String>> manejarIntegridad(DataIntegrityViolationException ex) {
        log.warn("Restriccion de integridad incumplida: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.error("La operacion no se pudo completar porque entra en conflicto con "
                        + "datos existentes o referencias invalidas."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<String>> manejarAccesoDatos(DataAccessException ex) {
        log.error("Error de acceso a datos en la API YAPAA", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.error("El servicio no esta disponible en este momento. "
                        + "Vuelve a intentarlo en unos instantes."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> manejarGeneral(Exception ex) {
        log.error("Error no controlado en la API YAPAA", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(MENSAJE_INTERNO));
    }
}
