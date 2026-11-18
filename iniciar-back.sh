

cd "$(dirname "$0")"
mkdir -p logs

# direcciones de todos los servicios
export IDENTIDAD_URL=http://localhost:8081
export ALOJAMIENTOS_URL=http://localhost:8082
export DISPONIBILIDAD_URL=http://localhost:8083
export COTIZACION_URL=http://localhost:8085
export RESERVAS_URL=http://localhost:8086
export NOTIFICACIONES_URL=http://localhost:8088
export AUDITORIA_URL=http://localhost:8090

# administrador que se crea al prender identidad
export ADMIN_EMAIL=admin@gmail.com
export ADMIN_INITIAL_PASSWORD=Admin1234

for servicio in identidad alojamientos disponibilidad busqueda cotizacion reservas notificaciones resenas auditoria reportes; do
  echo "Prendiendo $servicio..."
  (cd services/$servicio && ./mvnw -q spring-boot:run > ../../logs/$servicio.log 2>&1 &)
  sleep 5
done
