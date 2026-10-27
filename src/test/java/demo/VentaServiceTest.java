package demo;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class VentaServiceTest {

    private VentaService servicio() {

        return new VentaService(0);

    }

    private Solicitud solicitud(String referencia) {

        return new Solicitud(

                referencia, "VIDA-01", "FICTICIO-001");

    }

    @Test

    void duplicadoMismoContenido() {

        VentaService s = servicio();

        Venta primera = s.registrar(solicitud("abc"));

        Venta segunda = s.registrar(solicitud("abc"));

        assertEquals(primera, segunda);

        assertEquals(9, s.getCupos());

        assertEquals(1, s.totalVentas());

    }

    @Test

    void rechazosNoConsumenCupos() {

        VentaService s = servicio();

        assertEquals(

                HttpStatus.BAD_REQUEST,

                assertThrows(

                        ResponseStatusException.class,

                        () -> s.registrar(

                                new Solicitud("", "VIDA-01", "doc"))

                ).getStatusCode()

        );

        assertEquals(

                HttpStatus.NOT_FOUND,

                assertThrows(

                        ResponseStatusException.class,

                        () -> s.registrar(

                                new Solicitud("ref", "NO-EXISTE", "doc"))

                ).getStatusCode()

        );

        s.registrar(solicitud("ref"));

        assertEquals(

                HttpStatus.CONFLICT,

                assertThrows(

                        ResponseStatusException.class,

                        () -> s.registrar(

                                new Solicitud("ref", "VIDA-01", "OTRO"))

                ).getStatusCode()

        );

        assertEquals(9, s.getCupos());

        assertEquals(1, s.totalVentas());

    }

    @Test

    void concurrenciaMismaReferencia() throws Exception {

        VentaService s = servicio();

        ExecutorService pool =

                Executors.newFixedThreadPool(12);

        try {

            CountDownLatch salida = new CountDownLatch(1);

            List<Future<Venta>> resultados = new ArrayList<>();

            for (int i = 0; i < 12; i++) {

                resultados.add(pool.submit(() -> {

                    salida.await();

                    return s.registrar(solicitud("igual"));

                }));

            }

            salida.countDown();

            Set<String> ids = new HashSet<>();

            for (Future<Venta> resultado : resultados) {

                ids.add(resultado.get(

                        10, TimeUnit.SECONDS).id());

            }

            assertEquals(1, ids.size());

            assertEquals(9, s.getCupos());

            assertEquals(1, s.totalVentas());

        } finally {

            pool.shutdownNow();

        }

    }

    @Test

    void concurrenciaUltimoCupo() throws Exception {

        VentaService s = servicio();

        for (int i = 0; i < 9; i++) {

            s.registrar(solicitud("prev-" + i));

        }

        ExecutorService pool =

                Executors.newFixedThreadPool(2);

        try {

            CountDownLatch salida = new CountDownLatch(1);

            Callable<Boolean> tareaA = () -> {

                salida.await();

                try {

                    s.registrar(solicitud("final-a"));

                    return true;

                } catch (ResponseStatusException e) {

                    assertEquals(

                            HttpStatus.CONFLICT,

                            e.getStatusCode());

                    return false;

                }

            };

            Callable<Boolean> tareaB = () -> {

                salida.await();

                try {

                    s.registrar(solicitud("final-b"));

                    return true;

                } catch (ResponseStatusException e) {

                    assertEquals(

                            HttpStatus.CONFLICT,

                            e.getStatusCode());

                    return false;

                }

            };

            Future<Boolean> resultadoA =

                    pool.submit(tareaA);

            Future<Boolean> resultadoB =

                    pool.submit(tareaB);

            salida.countDown();

            int confirmadas =

                    (resultadoA.get(10, TimeUnit.SECONDS) ? 1 : 0)

                            + (resultadoB.get(10, TimeUnit.SECONDS) ? 1 : 0);

            assertEquals(1, confirmadas);

            assertEquals(0, s.getCupos());

            assertEquals(10, s.totalVentas());

        } finally {

            pool.shutdownNow();

        }

    }

    @Test

    void consultaOrdenLimiteExactitud() {

        VentaService s = new VentaService(1000);

        assertEquals(20, s.consultar(null, 20).size());

        assertEquals(

                1,

                s.consultar("hist-ref-50", 20).size()

        );

        assertTrue(

                s.consultar("inexistente", 20).isEmpty()

        );

        assertEquals(

                HttpStatus.BAD_REQUEST,

                assertThrows(

                        ResponseStatusException.class,

                        () -> s.consultar(null, 101)

                ).getStatusCode()

        );

        List<Venta> lista = s.consultar(null, 100);

        for (int i = 1; i < lista.size(); i++) {

            assertFalse(

                    lista.get(i - 1).fecha()

                            .isBefore(lista.get(i).fecha())

            );

        }

        assertEquals(10, s.getCupos());

    }

    @Test

    void reintentoDespuesDeAgotarCupos() {

        VentaService s = servicio();

        Venta primera =

                s.registrar(solicitud("primera"));

        for (int i = 0; i < 9; i++) {

            s.registrar(solicitud("extra-" + i));

        }

        assertEquals(

                primera,

                s.registrar(solicitud("primera"))

        );

        assertEquals(0, s.getCupos());

    }

}
