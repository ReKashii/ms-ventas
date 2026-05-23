package cl.bookpointchile.ventas.client;

import cl.bookpointchile.ventas.dto.StockResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class InventarioClientFallback implements InventarioClient {

    @Override
    public StockResponseDTO checkStock(Long productoId, Integer cantidad) {
        // Simulación: Si el ID del producto es 999, simulamos que no hay stock suficiente
        if (productoId == 999L) {
            return StockResponseDTO.builder()
                    .productoId(productoId)
                    .disponible(false)
                    .stockActual(0)
                    .build();
        }

        // Simulación por defecto: Stock disponible
        return StockResponseDTO.builder()
                .productoId(productoId)
                .disponible(true)
                .stockActual(cantidad + 15)
                .build();
    }
}
