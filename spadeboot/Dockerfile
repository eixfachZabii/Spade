FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

# Install necessary libraries and tools for Gurobi
RUN apt-get update && apt-get install -y \
    libc6 \
    libgcc-s1 \
    libstdc++6 \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Download and install Gurobi ARM64 version
RUN wget -q https://packages.gurobi.com/12.0/gurobi12.0.2_armlinux64.tar.gz \
    && tar xzf gurobi12.0.2_armlinux64.tar.gz \
    && mv gurobi1202 /opt/gurobi \
    && rm gurobi12.0.2_armlinux64.tar.gz

# Set up Gurobi environment variables for ARM64
ENV GUROBI_HOME=/opt/gurobi/armlinux64
ENV PATH="${PATH}:${GUROBI_HOME}/bin"
ENV LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:${GUROBI_HOME}/lib"

# Copy native libraries to standard Java library path
RUN cp /opt/gurobi/armlinux64/lib/libGurobiJni120.so /usr/lib/ && \
    cp /opt/gurobi/armlinux64/lib/libgurobi120.so /usr/lib/ && \
    ldconfig

# Create directory for Gurobi license
RUN mkdir -p /opt/gurobi/licenses

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
