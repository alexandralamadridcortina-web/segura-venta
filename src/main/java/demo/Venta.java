package demo;
import java.math.BigDecimal;
import java.time.Instant;
public record Venta(
        String id,
        String referenciaSolicitud,
        String campaniaId,
        BigDecimal prima,
        Instant fecha
) {
}