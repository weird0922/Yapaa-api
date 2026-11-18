package pe.upn.yapaa_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFiltro extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioDetallesService usuarioDetallesService;

    public JwtFiltro(JwtService jwtService, UsuarioDetallesService usuarioDetallesService) {
        this.jwtService = jwtService;
        this.usuarioDetallesService = usuarioDetallesService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String encabezado = request.getHeader("Authorization");
        String token = null;
        String correo = null;

        if (encabezado != null && encabezado.startsWith("Bearer ")) {
            token = encabezado.substring(7);
            if (jwtService.esValido(token)) {
                correo = jwtService.obtenerCorreo(token);
            }
        }

        if (correo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails usuario = usuarioDetallesService.loadUserByUsername(correo);
            if (usuario.isEnabled()) {
            UsernamePasswordAuthenticationToken autenticacion =
                    new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }
        }

        filterChain.doFilter(request, response);
    }
}
