package demo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(properties = "seguraventa.historico=0")
class SeguraVentaApplicationTests {
    @Test
    void contextLoads() {
    }
}