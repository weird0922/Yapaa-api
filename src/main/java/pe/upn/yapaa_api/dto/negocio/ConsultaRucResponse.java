package pe.upn.yapaa_api.dto.negocio;
public record ConsultaRucResponse(String ruc, String razonSocial, String estado, String condicion,
        boolean verificado, boolean demostracion) {}
