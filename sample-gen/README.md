
# Quarkus Framework
- k8s-native java framework designed to optimize Java for containerized env (Docker / K8s)

## Advantages
 - Live Coding and Dev Services (instant reload in save, automatically spin up unconfigured dependencies via docker daemon) during development

## Steps - Initialization

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:create \
  -DprojectGroupId=com.example \
  -DprojectArtifactId=task-manager \
  -Dextensions="resteasy-reactive-jackson, hibernate-orm-panache, jdbc-postgresql"
cd task-manager
```
Generate a new Quarkus project with pre-configured folder structure (```-DprojectGroupId```), `pom.xml` file and the specified libraries

- `quarkus-maven-plugin:create`: Quarkus project generator
- `-DprojectArtifactId`: project root dir name
- `-Dextensions`: essential backend modules

```bash
./mvnw quarkus:dev
```
Run the Maven wrapper to start up the development mode
- quarkus will automatically spin up two containers (`test-container` and `postgres-container`)
- check `javac -version` before running, it should be same as the version mentioned in `./mvnw -version`

## Build Runnable JAR file

```bash
mvn package
java -jar target/quarkus-app/quarkus-run.jar
```
