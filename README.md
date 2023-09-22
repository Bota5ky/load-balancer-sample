Spring Cloud LoadBalancer demo refer from https://spring.io/guides/gs/spring-cloud-loadbalancer/

```bash
SERVER_PORT=8090 ./gradlew :say-hello:bootRun
SERVER_PORT=9092 ./gradlew :say-hello:bootRun
SERVER_PORT=9999 ./gradlew :say-hello:bootRun

SERVER_PORT=8888 ./gradlew :user:bootRun
```
