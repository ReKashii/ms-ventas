###  Arquitectura Implementada                                                                                                                                                                   
                                                                                                                                                                                                    
  Se ha seguido rigurosamente el patrón CSR y las mejores prácticas en Spring Boot 3.2.x con Java 17:                                                                                               
                                                                                                                                                                                                    
  1. Modelos JPA Relacionales: Entidades  Venta  y  DetalleVenta  mapeadas bidireccionalmente. Se usó  @Getter / @Setter  de Lombok en lugar de  @Data  para prevenir desbordamientos de memoria (  
  StackOverflowError ) por referencias circulares en JPA.                                                                                                                                           
  2. DTOs de Entrada con Bean Validation (JSR 380): Validaciones estrictas en la capa Controller a través de  @Valid  y anotaciones como  @NotNull ,  @Min  y  @NotEmpty .                          
  3. Mapeo Limpio de Salida: DTOs específicos de respuesta para asegurar que la entidad JPA no se exponga al cliente Frontend, aislando la capa de base de datos.                                   
  4. Feign Client Simulado (ms-inventario): Un cliente Feign completo e interactivo apoyado por un componente  Fallback  que simula la lógica del stock localmente para facilitar tus pruebas sin   
  dependencias externas rígidas.                                                                                                                                                                    
  5. Manejo Global de Excepciones: Un  @RestControllerAdvice  que captura errores de negocio y errores sintácticos de validación, transformándolos en respuestas JSON uniformes ( ErrorResponse )   
  con estados HTTP correctos (400, 404, etc.).                                                                                                                                                      
  6. Logging Profesional (SLF4J): Trazabilidad completa con  @Slf4j  en el flujo crítico de transacciones para auditoría en tiempo real.                                                            
  ──────                                                                                                                                                                                            
  ### 📂 Clases Creadas y Módulos del Código                                                                                                                                                        
                                                                                                                                                                                                    
  Encontrarás todo el código fuente organizado en los siguientes archivos y paquetes Java dentro de  ventas/src/main/java/cl/bookpointchile/ventas :                                                
                                                                                                                                                                                                    
  #### 1. Capa de Dominio (Model)                                                                                                                                                                   
                                                                                                                                                                                                    
  •  TipoVenta.java  y  TipoDescuento.java : Enums que controlan las modalidades de compra ( PRESENCIAL ,  ONLINE ) y las promociones aplicadas.                                                    
  • Venta.java:                                                                                                                                                                                 
      • Entidad principal JPA con un  folio  único auto-generado (ej:  BP-PRE-A34F5C9B ).                                                                                                           
      • Relación  @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)  con  DetalleVenta .                                                                              
      • Métodos utilitarios  addDetalle()  y  removeDetalle()  para mantener sincronizada la bidireccionalidad de Hibernate.                                                                        
  • DetalleVenta.java:                                                                                                                                                                                
      • Entidad hija con relación  @ManyToOne  hacia  Venta  configurada con  @JsonIgnore  para evitar loops de serialización.                                                                      
                                                                                                                                                                                                    
                                                                                                                                                                                                    
  #### 2. Objetos de Transferencia de Datos (DTO)                                                                                                                                                   
                                                                                                                                                                                                    
  •  VentaRequestDTO.java  y  DetalleVentaRequestDTO.java : DTOs equipados con JSR 380 para interceptar entradas vacías o negativas.                                                                
  •  VentaResponseDTO.java  y  DetalleVentaResponseDTO.java : Estructuras limpias listas para el consumo de tu aplicación cliente (CSR).                                                            
  •  StockResponseDTO.java : Modelo de datos de comunicación externa.                                                                                                                               
                                                                                                                                                                                                    
  #### 3. Comunicación Inter-servicios (Feign Client)                                                                                                                                               
                                                                                                                                                                                                    
  • InventarioClient.java: Interfaz  @FeignClient  declarativa que apunta a  ms-inventario .                                                                                                              
  • InventarioClientFallback.java:                                                                                                                                                                                
      • Clase  @Component  de simulación inteligente. Regla de prueba programada: Si consultas con el ID de producto  999 , simulará automáticamente "Stock Insuficiente" (retorna  disponible =    
      false ), para cualquier otro ID de producto retornará stock disponible para facilitar tus flujos de testing.                                                                                  
                                                                                                                                                                                                    
                                                                                                                                                                                                    
  #### 4. Control de Excepciones (Exception)                                                                                                                                                        
                                                                                                                                                                                                    
  •  ResourceNotFoundException.java ,  InsufficientStockException.java  y  InvalidSaleException.java : Excepciones de negocio extendidas de  RuntimeException .                                     
  •  ErrorResponse.java : Modelo estándar JSON para retornar errores con metadatos (timestamp, status, message, path y detalles específicos de validación).                                         
  • GlobalExceptionHandler.java: Interceptor de controladores REST. Adapta las excepciones personalizadas y convierte los errores del validador de Spring ( MethodArgumentNotValidException ) en una
lista clara 
  de campos inválidos con estado HTTP 400 Bad Request.                                                                                                                                              
                                                                                                                                                                                                    
  #### 5. Capa de Persistencia (Repository)                                                                                                                                                         
                                                                                                                                                                                                    
  • VentaRepository.java: Interfaz JPA con consulta optimizada por Folio:  Optional<Venta> findByFolio(String folio) .                                                                                   
                                                                                                                                                                                                    
  #### 6. Capa de Servicios (Service)                                                                                                                                                               
                                                                                                                                                                                                    
  • VentaServiceImpl.java:                                                                                                                                                                                
      • Contiene la lógica del negocio centralizada bajo la etiqueta  @Transactional .                                                                                                              
      • Trazabilidad por Logs: Hace uso del logger  @Slf4j  imprimiendo logs detallados ( log.info ,  log.warn ,  log.error ) en puntos críticos.                                                   
      • Reglas de Descuentos Aplicadas:                                                                                                                                                             
          • Código  "CONVENIO_ESTUDIANTIL"  o  "ESTUDIANTE15" : Descuento del 15% (Convenios en caja física / Asistente).                                                                           
          • Código  "DESCUENTO10" : Descuento del 10% (Venta Web / Cupones).                                                                                                                        
          • Código  "PROMO20" : Descuento del 20% (Campañas especiales Web).                                                                                                                        
          • Cualquier otro código inválido arroja  InvalidSaleException  (HTTP 400).                                                                                                                
      • Validación de Caja: Si el tipo de venta es  PRESENCIAL , es obligatorio ingresar el  asistenteNombre . De lo contrario, arroja error.                                                       
                                                                                                                                                                                                    
                                                                                                                                                                                                    
  #### 7. Capa de Controladores (Controller)                                                                                                                                                        
                                                                                                                                                                                                    
  • VentaController.java:                                                                                                                                                                                
      • Expone los endpoints REST bajo  /api/ventas .                                                                                                                                               
      • Incluye  @CrossOrigin(origins = "*")  para permitir el consumo inmediato del frontend y evitar problemas de políticas CORS en CSR.                                                          
      • Utiliza  @Valid  para disparar las validaciones sintácticas JSR 380 de manera automática.                                                                                                   
                                                                                                                                                                                                    
  ──────                                                                                                                                                                                            
  ### ⚙️ Archivo de Configuración Creado                                                                                                                                                             
                                                                                                                                                                                                    
  He sobrescrito tu configuración para usar estrictamente el formato solicitado en:                                                                                                                 
                                                                                                                                                                                                    
  • application.properties:                                                                                                                                                                                
  Contiene la base de datos MySQL dinámica ( bookpoint_ventas ), las directivas de creación automática de tablas ( spring.jpa.hibernate.ddl-auto=update ), los timeouts de Feign y los niveles de   
  auditoría de trazas en consola.                                                                                                                                                                   
  ──────                                                                                                                                                                                            
  ### 🧪 Pruebas Rápidas de Integración                                                                                                                                                             
                                                                                                                                                                                                    
  Ejemplo para probar el microservicio localmente enviando un payload REST POST a  http://localhost:8081/api/ventas :                                                                                     
                                                                                                                                                                                                    
  #### Caso 1: Registrar una Venta en Caja (Presencial con Convenio Estudiantil)                                                                                                                    
                                                                                                                                                                                                    
  POST  /api/ventas                                                                                                                                                                                 
                                                                                                                                                                                                    
    {                                                                                                                                                                                               
      "tipoVenta": "PRESENCIAL",                                                                                                                                                                    
      "clienteNombre": "Renato Duoc",                                                                                                                                                               
      "clienteRut": "12.345.678-9",                                                                                                                                                                 
      "asistenteNombre": "Diego López",                                                                                                                                                             
      "codigoDescuento": "CONVENIO_ESTUDIANTIL",                                                                                                                                                    
      "detalles": [                                                                                                                                                                                 
        {                                                                                                                                                                                           
          "productoId": 101,                                                                                                                                                                        
          "productoNombre": "Introducción a los Algoritmos en Java",                                                                                                                                
          "cantidad": 2,                                                                                                                                                                            
          "precioUnitario": 25000.00                                                                                                                                                                
        }                                                                                                                                                                                           
      ]                                                                                                                                                                                             
    }                                                                                                                                                                                               
                                                                                                                                                                                                    
  • Lógica aplicada: ms-ventas consultará el stock simulado del producto  101 . Al ser válido, registrará la transacción, aplicará un 15% de descuento estudiantil sobre el subtotal ($50,000)      
  resultando en un total final de $42,500, persistiendo en BD y retornando el folio único.                                                                                                          
                                                                                                                                                                                                    
  #### Caso 2: Simulación de Stock Insuficiente (Venta Online Fallida)                                                                                                                              
                                                                                                                                                                                                    
  POST  /api/ventas                                                                                                                                                                                 
                                                                                                                                                                                                    
    {                                                                                                                                                                                               
      "tipoVenta": "ONLINE",                                                                                                                                                                        
      "clienteNombre": "Comprador Web",                                                                                                                                                             
      "clienteRut": "98.765.432-1",                                                                                                                                                                 
      "codigoDescuento": "DESCUENTO10",                                                                                                                                                             
      "detalles": [                                                                                                                                                                                 
        {                                                                                                                                                                                           
          "productoId": 999,                                                                                                                                                                        
          "productoNombre": "Libro de Programación Avanzada",                                                                                                                                       
          "cantidad": 1,                                                                                                                                                                            
          "precioUnitario": 35000.00                                                                                                                                                                
        }                                                                                                                                                                                           
      ]                                                                                                                                                                                             
    }                                                                                                                                                                                               
                                                                                                                                                                                                    
  • Lógica aplicada: El microservicio identificará el ID de prueba  999  y la respuesta del FeignClient simulado disparará la excepción  InsufficientStockException  retornando un JSON de error    
  estructurado con código HTTP 400. 
