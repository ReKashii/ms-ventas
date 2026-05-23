package cl.bookpointchile.ventas.controller;

import cl.bookpointchile.ventas.dto.VentaRequestDTO;
import cl.bookpointchile.ventas.dto.VentaResponseDTO;
import cl.bookpointchile.ventas.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permite la comunicación ágil con el Frontend bajo el patrón CSR
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    public ResponseEntity<VentaResponseDTO> registrarVenta(@Valid @RequestBody VentaRequestDTO request) {
        VentaResponseDTO response = ventaService.registrarVenta(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{folio}")
    public ResponseEntity<VentaResponseDTO> obtenerVentaPorFolio(@PathVariable String folio) {
        VentaResponseDTO response = ventaService.obtenerVentaPorFolio(folio);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<VentaResponseDTO>> obtenerTodas() {
        List<VentaResponseDTO> response = ventaService.obtenerTodas();
        return ResponseEntity.ok(response);
    }
}
