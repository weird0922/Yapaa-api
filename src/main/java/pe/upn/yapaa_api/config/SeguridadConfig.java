package pe.upn.yapaa_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.upn.yapaa_api.security.JwtFiltro;
import pe.upn.yapaa_api.security.UsuarioDetallesService;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    private final JwtFiltro jwtFiltro;
    private final UsuarioDetallesService usuarioDetallesService;

    public SeguridadConfig(JwtFiltro jwtFiltro, UsuarioDetallesService usuarioDetallesService) {
        this.jwtFiltro = jwtFiltro;
        this.usuarioDetallesService = usuarioDetallesService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(usuarioDetallesService);
        proveedor.setPasswordEncoder(passwordEncoder());
        return proveedor;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion)
            throws Exception {
        return configuracion.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource origen = new UrlBasedCorsConfigurationSource();
        origen.registerCorsConfiguration("/**", configuracion);
        return origen;
    }

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/demo").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro", "/api/auth/registro-negocio").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/negocios/ruc/*", "/api/planes").permitAll()
                        .requestMatchers("/api/ventas/**", "/api/negocios/*/plan", "/api/negocios/*/ventas", "/api/negocios/*/indicadores", "/api/negocios/*/reporte.csv").hasAnyRole("NEGOCIO","ADMIN")
                        .requestMatchers("/api/pagos/**", "/api/pedidos/*/pago", "/api/negocios/afiliacion").denyAll()
                        .requestMatchers("/api/productos/mios").hasAnyRole("NEGOCIO", "ADMIN")
                        .requestMatchers("/api/auth/sesion", "/api/negocios/mios").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/negocios/*/aprobacion").hasRole("ADMIN")
                        .requestMatchers("/api/cuentas/**", "/api/comisiones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/clientes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/estado/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/estado").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/categorias/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").access((autenticacion, contexto) -> {
                            var permisos = autenticacion.get().getAuthorities();
                            return new org.springframework.security.authorization.AuthorizationDecision(
                                    permisos.stream().noneMatch(p -> p.getAuthority().equals("ROLE_NEGOCIO"))
                                    || permisos.stream().anyMatch(p -> p.getAuthority().equals("ROLE_ADMIN")));
                        })
                        .requestMatchers("/api/categorias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/negocios/cuenta/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/negocios/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/negocios").hasAnyRole("NEGOCIO", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/productos/**").hasAnyRole("NEGOCIO", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/productos/**").hasAnyRole("NEGOCIO", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasAnyRole("NEGOCIO", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasAnyRole("NEGOCIO", "ADMIN")
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"exitoso\":false,\"mensaje\":\"Inicia sesion para continuar.\",\"datos\":null}");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"exitoso\":false,\"mensaje\":\"No tienes permisos para esta operacion.\",\"datos\":null}");
                        }))
                .sessionManagement(sesion ->
                        sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
