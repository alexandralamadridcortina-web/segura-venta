package demo;

import java.math.BigDecimal;

import java.time.Instant;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;

@Service

public class VentaService {

    private int cupos = 10;

    private final Map<String, Venta> ventasPorReferencia =

            new HashMap<>();

    private final Map<String, String> documentosPorReferencia =

            new HashMap<>();

    private final List<Venta> historialOrdenado =

            new ArrayList<>();

    public VentaService(

            @Value("${seguraventa.historico:20000}") int historico) {

        if (historico < 0) {

            throw new IllegalArgumentException(

                    "Historico invalido");

        }

        Instant base = Instant.parse("2025-01-01T00:00:00Z");

        for (int i = 0; i < historico; i++) {

            String id = String.format("hist-%08d", i);

            Venta venta = new Venta(

                    id,

                    "hist-ref-" + i,

                    "ARCHIVO",

                    new BigDecimal("50000"),

                    base.plusSeconds(i)

            );

            ventasPorReferencia.put(

                    venta.referenciaSolicitud(), venta);

            historialOrdenado.add(venta);

        }

        ordenar();

    }

    public record ResultadoRegistro(

            Venta venta,

            boolean creada

    ) {

    }

    public synchronized Venta registrar(Solicitud solicitud) {

        return registrarConEstado(solicitud).venta();

    }

    public synchronized ResultadoRegistro registrarConEstado(

            Solicitud solicitud) {

        if (solicitud == null ||

                vacio(solicitud.referenciaSolicitud()) ||

                vacio(solicitud.campaniaId()) ||

                vacio(solicitud.documento())) {

            throw new ResponseStatusException(

                    HttpStatus.BAD_REQUEST,

                    "Solicitud invalida");

        }

        String referencia = solicitud.referenciaSolicitud();

        if (referencia.startsWith("hist-ref-")) {

            throw new ResponseStatusException(

                    HttpStatus.CONFLICT,

                    "Referencia reservada para archivo");

        }

        Venta existente = ventasPorReferencia.get(referencia);

        if (existente != null) {

            if (!existente.campaniaId().equals(

                    solicitud.campaniaId()) ||

                    !Objects.equals(

                            documentosPorReferencia.get(referencia),

                            solicitud.documento())) {

                throw new ResponseStatusException(

                        HttpStatus.CONFLICT,

                        "Referencia incompatible");

            }

            return new ResultadoRegistro(existente, false);

        }

        if (!"VIDA-01".equals(solicitud.campaniaId())) {

            throw new ResponseStatusException(

                    HttpStatus.NOT_FOUND,

                    "Campania inexistente");

        }

        if (cupos <= 0) {

            throw new ResponseStatusException(

                    HttpStatus.CONFLICT,

                    "Sin cupos");

        }

        Venta venta = new Venta(

                UUID.randomUUID().toString(),

                referencia,

                "VIDA-01",

                new BigDecimal("50000"),

                Instant.now()

        );

        ventasPorReferencia.put(referencia, venta);

        documentosPorReferencia.put(

                referencia, solicitud.documento());

        cupos--;

        historialOrdenado.add(venta);

        ordenar();

        return new ResultadoRegistro(venta, true);

    }

    private static boolean vacio(String texto) {

        return texto == null || texto.isBlank();

    }

    private void ordenar() {

        historialOrdenado.sort(

                Comparator.comparing(Venta::fecha)

                        .reversed()

                        .thenComparing(Venta::id)

        );

    }

    public synchronized List<Venta> consultar(

            String referenciaSolicitud,

            int limite) {

        if (limite < 1 || limite > 100) {

            throw new ResponseStatusException(

                    HttpStatus.BAD_REQUEST,

                    "Limite invalido");

        }

        if (referenciaSolicitud != null) {

            Venta venta =

                    ventasPorReferencia.get(referenciaSolicitud);

            return venta == null

                    ? List.of()

                    : List.of(venta);

        }

        return new ArrayList<>(

                historialOrdenado.subList(

                        0,

                        Math.min(limite, historialOrdenado.size())

                )

        );

    }

    public synchronized int getCupos() {

        return cupos;

    }

    public synchronized int totalVentas() {

        return ventasPorReferencia.size();

    }

}
