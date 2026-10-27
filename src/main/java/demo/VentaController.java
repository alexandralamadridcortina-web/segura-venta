package demo;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
public class VentaController {
    private final VentaService servicio;
    public VentaController(VentaService servicio) {
        this.servicio = servicio;
    }
    @PostMapping("/ventas")
    public ResponseEntity<Venta> vender(
            @RequestBody Solicitud solicitud) {
        VentaService.ResultadoRegistro resultado =
                servicio.registrarConEstado(solicitud);
        return ResponseEntity.status(
                resultado.creada()
                        ? HttpStatus.CREATED
                        : HttpStatus.OK
        ).body(resultado.venta());
    }
    @GetMapping("/ventas")
    public List<Venta> consultar(
            @RequestParam(required = false)
            String referenciaSolicitud,
            @RequestParam(defaultValue = "20")
            int limite) {
        return servicio.consultar(
                referenciaSolicitud, limite);
    }
}