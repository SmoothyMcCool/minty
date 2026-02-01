# Multi-stage Dockerfile for Minty
# Stage 1: Build Frontend (Angular)
# Stage 2: Build Backend (Maven)
# Stage 3: Runtime (Tomcat)

# ============================================
# Stage 1: Build Angular Frontend
# ============================================
FROM node:22-alpine AS frontend-builder

WORKDIR /app/frontend/app

# Copy package files first for better layer caching
COPY webapp/frontend/app/package*.json ./

# Install dependencies (using npm install since there's no package-lock.json)
RUN npm install

# Copy frontend source
COPY webapp/frontend/app/ ./

# Build for production (uses production config with /Minty/ base href)
# Output goes to ../deploy (i.e., /app/frontend/deploy)
RUN npm run build -- --configuration=production

# ============================================
# Stage 2: Build Java Backend
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS backend-builder

WORKDIR /app

# Copy parent POM and install it first (for dependency caching)
COPY webapp/backend/parent/pom.xml /app/parent/pom.xml
RUN cd /app/parent && mvn install -N -q

# Copy all backend modules
COPY webapp/backend/ /app/backend/

# Copy built frontend from previous stage to the location Maven expects
# The bundle pom.xml references ../../frontend/deploy relative to webapp/backend/bundle
# So we need it at /app/frontend/deploy
COPY --from=frontend-builder /app/frontend/deploy /app/frontend/deploy

# Build the backend with Docker-friendly paths
# Override the output directories that normally point to Windows paths
RUN cd /app/backend/solution && \
    mvn clean package -DskipTests \
    -Doutput.directory=/app/output \
    -Dtomcat.base=/app/tomcat

# The WAR file will be at /app/tomcat/webapps/Minty.war

# ============================================
# Stage 3: Runtime with Tomcat
# ============================================
FROM tomcat:11-jdk21-temurin

# Remove default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the WAR file
COPY --from=backend-builder /app/tomcat/webapps/Minty.war /usr/local/tomcat/webapps/Minty.war

# Copy configuration files to Tomcat conf directory
COPY --from=backend-builder /app/tomcat/conf/Minty /usr/local/tomcat/conf/Minty

# Copy pug templates to a staging location (will be copied to volume at runtime)
COPY --from=backend-builder /app/output/working/pug /app/pug-templates

# Copy entrypoint script
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Set environment variables for configuration overrides
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Dspring.config.additional-location=file:/usr/local/tomcat/conf/Minty/"

# Expose Tomcat port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/Minty/api/login || exit 1

# Use entrypoint to create directories and start Tomcat
ENTRYPOINT ["/entrypoint.sh"]
