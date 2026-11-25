package pe.upn.yapaa_api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.auth.LoginRequest;
import pe.upn.yapaa_api.dto.auth.LoginResponse;
import pe.upn.yapaa_api.dto.auth.RegistroRequest;
import pe.upn.yapaa_api.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final pe.upn.yapaa_api.security.LimiteIntentos limites;

    public AuthController(AuthService authService, pe.upn.yapaa_api.security.LimiteIntentos limites) {
        this.authService = authService;this.limites=limites;
    }

    @org.springframework.beans.factory.annotation.Value("${yapaa.demo.habilitado:false}")
    private boolean demoHabilitado;

    @GetMapping("/demo")
    public ResponseEntity<Result<Boolean>> demo() { return ResponseEntity.ok(Result.exitoso(demoHabilitado)); }

    @GetMapping("/sesion")
    public ResponseEntity<Result<LoginResponse>> sesion() {
        return ResponseEntity.ok(authService.sesionActual());
    }

    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@Valid @RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest peticion) {
        limites.comprobar("login:"+peticion.getRemoteAddr(),30);
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro-negocio")
    public ResponseEntity<Result<LoginResponse>> registrarNegocio(@Valid @RequestBody pe.upn.yapaa_api.dto.auth.RegistroNegocioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarNegocio(request));
    }

    @PostMapping("/registro")
    public ResponseEntity<Result<LoginResponse>> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }
}
