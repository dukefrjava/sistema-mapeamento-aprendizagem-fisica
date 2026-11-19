# =============================================================================
#  SIMAF - Backend Java (Spring Boot 4 / Java 21)
# -----------------------------------------------------------------------------
#  Build local (rodar dentro da pasta simaf/):
#     docker build -t simaf-api .
#
#  Rodar local (exemplo apontando pro Postgres e pro scanner Python):
#     docker run --rm -p 8080:8080 \
#       -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/simaf \
#       -e SPRING_DATASOURCE_USERNAME=app \
#       -e SPRING_DATASOURCE_PASSWORD=senha \
#       -e SCANNER_URL=http://host:8000 \
#       simaf-api
#
#  No Render: Root Directory = simaf | Runtime = Docker
#  (o Render injeta a variável PORT sozinho; a imagem já respeita ela)
# =============================================================================


# -----------------------------------------------------------------------------
#  ETAPA 1: BUILD - compila o projeto com Maven e gera o .jar
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# COPIA SÓ O pom.xml PRIMEIRO: ASSIM O DOCKER REAPROVEITA O CACHE DAS
# DEPENDÊNCIAS E NÃO BAIXA TUDO DE NOVO QUANDO SÓ O CÓDIGO MUDA
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# AGORA O CÓDIGO-FONTE E O EMPACOTAMENTO
# (TESTES PULADOS: O contextLoads PRECISA DE UM POSTGRES RODANDO)
COPY src ./src
RUN mvn -B -q package -DskipTests \
 && cp target/*.jar app.jar

# QUEBRA O .jar EM CAMADAS (dependências / loader / código da aplicação):
# NOS PRÓXIMOS DEPLOYS SÓ A CAMADA DO CÓDIGO MUDA, E O APP SOBE MAIS RÁPIDO
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted


# -----------------------------------------------------------------------------
#  ETAPA 2: RUNTIME - imagem final enxuta, só com a JRE (sem Maven/JDK)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# USUÁRIO SEM PRIVILÉGIO DE ROOT
RUN addgroup -S simaf && adduser -S simaf -G simaf
WORKDIR /app

# COPIA AS CAMADAS DA MENOS PARA A MAIS ALTERADA
COPY --from=build --chown=simaf:simaf /build/extracted/dependencies/ ./
COPY --from=build --chown=simaf:simaf /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=simaf:simaf /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=simaf:simaf /build/extracted/application/ ./

USER simaf

# --- AJUSTES DA JVM PARA CONTAINERS PEQUENOS (plano grátis = 512 MB) ---------
#  MaxRAMPercentage      -> heap proporcional à memória do container
#  UseSerialGC           -> coletor de lixo mais leve (ideal p/ pouca CPU/RAM)
#  TieredStopAtLevel=1   -> sobe bem mais rápido (ajuda no "acordar" do Render)
#  ExitOnOutOfMemoryError-> se faltar memória, reinicia em vez de travar
#  user.timezone         -> datas no horário de Brasília
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError -Duser.timezone=America/Sao_Paulo"

# --- PADRÕES DE PRODUÇÃO (podem ser sobrescritos no painel do Render) --------
#  Desliga o log de SQL e o DEBUG do Spring Web, que poluem o log em produção
ENV SPRING_JPA_SHOW_SQL=false \
    LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=INFO \
    PORT=8080

EXPOSE 8080

# CONFERE SE O APP ESTÁ RESPONDENDO (útil no docker compose / docker ps)
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -q -O /dev/null "http://127.0.0.1:${PORT}/simaf.html" || exit 1

# "exec" FAZ O JAVA SER O PROCESSO PRINCIPAL (recebe o sinal de desligar certo)
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT} org.springframework.boot.loader.launch.JarLauncher"]
