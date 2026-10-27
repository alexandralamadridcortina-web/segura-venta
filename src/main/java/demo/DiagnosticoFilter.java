package demo;
import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class DiagnosticoFilter extends OncePerRequestFilter {
    private static final Logger log =
            LoggerFactory.getLogger(DiagnosticoFilter.class);
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {
        String operacion = UUID.randomUUID().toString();
        long inicio = System.nanoTime();
        response.setHeader("X-Operacion-Id", operacion);
        try {
            chain.doFilter(request, response);
        } finally {
            long duracionMs =
                    (System.nanoTime() - inicio) / 1_000_000;
            log.info(
                    "operacion={} metodo={} ruta={} estado={} duracionMs={}",
                    operacion,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duracionMs
            );
        }
    }
}