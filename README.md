# Marketplace de Alojamientos - Inversiones LR

Proyecto de Ingeniería de Software 1 - Universidad El Bosque.

Es una página donde los anfitriones publican alojamientos y los huéspedes los buscan, cotizan y reservan. También tiene pagos de prueba con Stripe, cancelaciones, reseñas, notificaciones, auditoría, dashboard y reportes.

## Integrantes

- Jenny Alejandra Gómez López
- Brian Steban Rodríguez Ortiz
- Nicole Diane Cruz García
- Tomás Álzate Yepes

## Cómo está organizado

El back son 11 servicios hechos en Spring Boot con Java 21, cada uno en su carpeta dentro de `services/` y con su propio puerto. El front está en Angular en la carpeta `frontend/`.

| Servicio | Puerto |
|---|---|
| Front (Angular) | 4200 |
| identidad | 8081 |
| alojamientos | 8082 |
| disponibilidad | 8083 |
| busqueda | 8084 |
| cotizacion | 8085 |
| reservas | 8086 |
| pagos | 8087 |
| notificaciones | 8088 |
| resenas | 8089 |
| auditoria | 8090 |
| reportes | 8091 |

Cada servicio usa una base de datos H2, entonces no hay que instalar nada de base de datos.

## Qué hay que tener instalado

- Java 21
- Node.js 24
- Git Bash (En Windows)

## Cómo correrlo

1. Clonar el repositorio:
```bash
git clone https://github.com/Proyecto-Software-Marketing/marketplace-alojamientos.git
cd marketplace-alojamientos
```

2. Copiar `.env` y poner la clave de Stripe:
```
STRIPE_SECRET_KEY=sk_test_tu_clave
STRIPE_CURRENCY=cop
```
El `.env` no se sube a GitHub.

3. Prender el back (en Git Bash):
```bash
./iniciar-back.sh
```
Esto prende los 11 servicios. Hay que esperar como 1 o 2 minutos. Para ver si ya prendieron todos:
```bash
grep -l "Started" logs/*.log
```
Tienen que salir los 11. Si falta alguno se puede mirar su log, por ejemplo `tail -30 logs/pagos.log`.


4. Prender el front (en otra terminal):
```bash
cd frontend
npm install
npx ng serve
```
Y abrir http://localhost:4200

5. Para apagar todo:
```bash
./detener-back.sh
```
Si hay que reiniciar, primero se apaga y después se prende. Si se prende dos veces salen los puertos ocupados.

## Usuarios

- Administrador: `admin@gmail.com` / `Admin1234` (se crea solo al prender el back).
- Anfitrión y huésped: se crean en Registrarse.

## Para probarlo

1. Con el anfitrión crear una publicación con servicios, reglas, imagen y tarifas, y enviarla a revisión.
2. Agregarle un periodo en Disponibilidad.
3. Con el admin aprobarla en Aprobaciones.
4. Con el huésped buscarla, ver la cotización y reservar. El pago se ve en Stripe en modo prueba.
5. En Mis reservas se puede cancelar y se aplica la política de cancelación.
6. Con el admin se puede ver el dashboard, la auditoría, los reportes, los usuarios y bloquear publicaciones.

Para dejar una reseña la reserva tiene que estar completada (que ya haya pasado la fecha de salida).

## Pruebas

Dentro de cada servicio:
```bash
./mvnw test
```
Y el front se revisa con `npx ng build` dentro de `frontend`.

En cada Pull Request GitHub Actions corre las pruebas de los 11 servicios, SonarCloud revisa el código y se compila el front. A `main` solo se sube por Pull Request.

## Cómo trabajamos con Git

Historia en Jira → rama → commits → Pull Request → GitHub Actions y SonarCloud → revisión → merge.

- Ramas: `feature/MKT-XX-descripcion`
- Commits: `MKT-XX: lo que se hizo`

## Documentación

- Confluence: https://alejagomez026.atlassian.net/wiki/spaces/PMA
- Jira: proyecto MKT
- Contratos de la API: `openapi.yaml` y la carpeta `openapi_por_servicio/`
