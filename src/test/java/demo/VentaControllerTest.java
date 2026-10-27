package demo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties = "seguraventa.historico=0")
@AutoConfigureMockMvc
class VentaControllerTest {
    @Autowired
    MockMvc mvc;
    @Test
    void postYReintento() throws Exception {
        String body = """
               {
                 "referenciaSolicitud": "web-test",
                 "campaniaId": "VIDA-01",
                 "documento": "FICTICIO-123"
               }
               """;
        mvc.perform(
                        post("/ventas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prima").value(50000))
                .andExpect(jsonPath("$.documento").doesNotExist())
                .andExpect(header().exists("X-Operacion-Id"));
        mvc.perform(
                        post("/ventas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk());
        mvc.perform(
                        get("/ventas")
                                .param("referenciaSolicitud", "web-test")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].referenciaSolicitud")
                                .value("web-test")
                );
    }
    @Test
    void solicitudInvalida() throws Exception {
        mvc.perform(
                        post("/ventas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                               {
                                 "referenciaSolicitud": "",
                                 "campaniaId": "VIDA-01",
                                 "documento": "FICTICIO"
                               }
                               """)
                )
                .andExpect(status().isBadRequest());
        mvc.perform(
                        get("/ventas").param("limite", "0")
                )
                .andExpect(status().isBadRequest());
    }
}