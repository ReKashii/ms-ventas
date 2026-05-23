package cl.bookpointchile.ventas.client;

import cl.bookpointchile.ventas.dto.StockResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ms-inventario", url = "${app.feign.ms-inventario.url:http://localhost:8082}", fallback = InventarioClientFallback.class)
public interface InventarioClient {

    @GetMapping("/api/inventario/check-stock")
    StockResponseDTO checkStock(
            @RequestParam("productoId") Long productoId,
            @RequestParam("cantidad") Integer cantidad
    );
}
