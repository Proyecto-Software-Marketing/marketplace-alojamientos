
for puerto in 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091; do
  pid=$(netstat -ano | grep ":$puerto " | grep LISTENING | awk '{print $5}' | head -1)
  if [ -n "$pid" ]; then
    taskkill //F //PID $pid > /dev/null && echo "Apagado puerto $puerto"
  fi
done