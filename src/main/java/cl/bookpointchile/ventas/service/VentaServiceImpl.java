package cl.bookpointchile.ventas.service;

import cl.bookpointchile.ventas.client.InventarioClient;
import cl.bookpointchile.ventas.dto.*;
import cl.bookpointchile.ventas.exception.InsufficientStockException;
import cl.bookpointchile.ventas.exception.InvalidSaleException;
import cl.bookpointchile.ventas.exception.ResourceNotFoundException;
import cl.bookpointchile.ventas.model.*;
import cl.bookpointchile.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final InventarioClient inventarioClient;

    @Override
    @Transactional
    public VentaResponseDTO registrarVenta(VentaRequestDTO request) {
        log.info("Iniciando registro de venta. Tipo: {}, Canal: {}", 
                request.getTipoVenta(), request.getTipoVenta() == TipoVenta.PRESENCIAL ? "Caja" : "Online");

        // 1. Validaciones de Negocio Específicas
        if (request.getTipoVenta() == TipoVenta.PRESENCIAL && 
                (request.getAsistenteNombre() == null || request.getAsistenteNombre().trim().isEmpty())) {
            log.error("Error de validación: Venta presencial sin nombre de asistente.");
            throw new InvalidSaleException("Para ventas presenciales en caja es obligatorio ingresar el nombre del asistente de ventas.");
        }

        // 2. Consulta y Validación de Stock mediante FeignClient
        for (DetalleVentaRequestDTO item : request.getDetalles()) {
            log.info("Consultando disponibilidad en ms-inventario para el producto ID: {}, Nombre: {}, Cantidad solicitada: {}", 
                    item.getProductoId(), item.getProductoNombre(), item.getCantidad());
            
            StockResponseDTO stockResp = inventarioClient.checkStock(item.getProductoId(), item.getCantidad());
            
            if (!stockResp.isDisponible()) {
                log.error("Stock insuficiente en ms-inventario para el producto ID: {} ({}). Stock actual simulado: {}", 
                        item.getProductoId(), item.getProductoNombre(), stockResp.getStockActual());
                throw new InsufficientStockException("El producto '" + item.getProductoNombre() + "' (ID: " + 
                        item.getProductoId() + ") no tiene stock suficiente. Disponible simulado: " + stockResp.getStockActual());
            }
            log.info("Stock disponible confirmado para producto ID: {}", item.getProductoId());
        }

        // 3. Cálculo de Subtotales y Totales
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleVentaRequestDTO item : request.getDetalles()) {
            BigDecimal itemSubtotal = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            subtotal = subtotal.add(itemSubtotal);
        }

        // 4. Aplicar Descuentos o Convenios Estudiantiles / Cupones Web
        BigDecimal descuento = BigDecimal.ZERO;
        TipoDescuento tipoDescuento = TipoDescuento.NINGUNO;
        String codigo = request.getCodigoDescuento();

        if (codigo != null && !codigo.trim().isEmpty()) {
            String codigoClean = codigo.trim().toUpperCase();
            if (codigoClean.equals("CONVENIO_ESTUDIANTIL") || codigoClean.equals("ESTUDIANTE15")) {
                descuento = subtotal.multiply(new BigDecimal("0.15")); // 15% Descuento Estudiantil
                tipoDescuento = TipoDescuento.CONVENIO_ESTUDIANTIL;
                log.info("Descuento del 15% aplicado por convenio estudiantil: {}", codigoClean);
            } else if (codigoClean.equals("DESCUENTO10")) {
                descuento = subtotal.multiply(new BigDecimal("0.10")); // 10% Cupón
                tipoDescuento = TipoDescuento.CUPON;
                log.info("Descuento del 10% aplicado por cupón: {}", codigoClean);
            } else if (codigoClean.equals("PROMO20")) {
                descuento = subtotal.multiply(new BigDecimal("0.20")); // 20% Cupón Web
                tipoDescuento = TipoDescuento.CUPON;
                log.info("Descuento del 20% aplicado por cupón promocional: {}", codigoClean);
            } else {
                log.warn("Código de descuento no válido intentado: {}", codigo);
                throw new InvalidSaleException("El cupón o convenio estudiantil '" + codigo + "' ingresado no es válido.");
            }
        }

        // Redondear descuento y calcular total
        descuento = descuento.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        // 5. Generar Entidad Venta y Detalles (Bidireccional)
        String folioUnico = "BP-" + request.getTipoVenta().name().substring(0, 3) + "-" + 
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Venta venta = Venta.builder()
                .folio(folioUnico)
                .fecha(LocalDateTime.now())
                .tipoVenta(request.getTipoVenta())
                .clienteNombre(request.getClienteNombre())
                .clienteRut(request.getClienteRut())
                .asistenteNombre(request.getTipoVenta() == TipoVenta.PRESENCIAL ? request.getAsistenteNombre() : null)
                .subtotal(subtotal)
                .descuentoAplicado(descuento)
                .tipoDescuento(tipoDescuento)
                .codigoDescuento(tipoDescuento != TipoDescuento.NINGUNO ? codigo.trim().toUpperCase() : null)
                .total(total)
                .build();

        for (DetalleVentaRequestDTO item : request.getDetalles()) {
            BigDecimal itemSubtotal = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            DetalleVenta detalle = DetalleVenta.builder()
                    .productoId(item.getProductoId())
                    .productoNombre(item.getProductoNombre())
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioUnitario())
                    .subtotal(itemSubtotal)
                    .build();
            venta.addDetalle(detalle);
        }

        // 6. Guardar en Base de Datos
        Venta ventaGuardada = ventaRepository.save(venta);
        log.info("Venta guardada con éxito en la base de datos. Folio: {}, ID de Venta: {}, Total: {}", 
                ventaGuardada.getFolio(), ventaGuardada.getId(), ventaGuardada.getTotal());

        return mapToResponse(ventaGuardada);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponseDTO obtenerVentaPorFolio(String folio) {
        log.info("Buscando venta con folio: {}", folio);
        Venta venta = ventaRepository.findByFolio(folio)
                .orElseThrow(() -> {
                    log.error("Venta no encontrada con folio: {}", folio);
                    return new ResourceNotFoundException("La venta con el folio '" + folio + "' no existe.");
                });
        return mapToResponse(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> obtenerTodas() {
        log.info("Obteniendo listado de todas las ventas.");
        return ventaRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Mapper manual Helper para mantener el diseño CSR libre de acoplamientos pesados
    private VentaResponseDTO mapToResponse(Venta venta) {
        List<DetalleVentaResponseDTO> detalleDTOs = venta.getDetalles().stream()
                .map(d -> DetalleVentaResponseDTO.builder()
                        .id(d.getId())
                        .productoId(d.getProductoId())
                        .productoNombre(d.getProductoNombre())
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .subtotal(d.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return VentaResponseDTO.builder()
                .id(venta.getId())
                .folio(venta.getFolio())
                .fecha(venta.getFecha())
                .tipoVenta(venta.getTipoVenta())
                .clienteNombre(venta.getClienteNombre())
                .clienteRut(venta.getClienteRut())
                .asistenteNombre(venta.getAsistenteNombre())
                .subtotal(venta.getSubtotal())
                .descuentoAplicado(venta.getDescuentoAplicado())
                .tipoDescuento(venta.getTipoDescuento())
                .codigoDescuento(venta.getCodigoDescuento())
                .total(venta.getTotal())
                .detalles(detalleDTOs)
                .build();
    }
}
