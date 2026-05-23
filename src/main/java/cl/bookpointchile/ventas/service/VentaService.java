package cl.bookpointchile.ventas.service;

import cl.bookpointchile.ventas.dto.VentaRequestDTO;
import cl.bookpointchile.ventas.dto.VentaResponseDTO;

import java.util.List;

public interface VentaService {
    VentaResponseDTO registrarVenta(VentaRequestDTO request);
    VentaResponseDTO obtenerVentaPorFolio(String folio);
    List<VentaResponseDTO> obtenerTodas();
}
