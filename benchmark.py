import sys

import time

import statistics

import urllib.request

repeticiones = int(sys.argv[1]) if len(sys.argv) > 1 else 100

consultas = [

    ("listado", "/ventas?limite=20"),

    (

        "exacta",

        "/ventas?referenciaSolicitud=hist-ref-50&limite=20"

    )

]

for nombre, ruta in consultas:

    url = "http://localhost:8080" + ruta

    def medir():

        inicio = time.perf_counter_ns()

        with urllib.request.urlopen(url, timeout=10) as respuesta:

            assert respuesta.status == 200

            respuesta.read()

        return (time.perf_counter_ns() - inicio) / 1_000_000

    for _ in range(10):

        medir()

    valores = [medir() for _ in range(repeticiones)]

    ordenados = sorted(valores)

    indice_p95 = max(

        0,

        int(0.95 * len(ordenados) + 0.999999) - 1

    )

    p95 = ordenados[indice_p95]

    print(

        f"{nombre}: "

        f"n={repeticiones}, "

        f"mediana={statistics.median(valores):.3f} ms, "

        f"p95={p95:.3f} ms, "

        f"minimo={min(valores):.3f} ms, "

        f"maximo={max(valores):.3f} ms"

    )
