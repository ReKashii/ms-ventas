package cl.bookpointchile.ventas.dto;

import cl.bookpointchile.ventas.model.TipoDescuento;
import cl.bookpointchile.ventas.model.TipoVenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponseDTO {
    private Long id;
    private String folio;
    private LocalDateTime fecha;
    private TipoVenta tipoVenta;
    private String clienteNombre;
    private String clienteRut;
    private String asistenteNombre;
    private BigDecimal descuentoAplicado;
    private TipoDescuento tipoDescuento;
    private String codigoDescuento;
    private BigDecimal subtotal;
    private BigDecimal total;
    private List<DetalleVentaResponseDTO> detalles;
}
