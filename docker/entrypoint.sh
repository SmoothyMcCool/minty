#!/bin/bash
# Create data directories if they don't exist
# These are created at runtime because the volume mount happens after the image is built

mkdir -p /app/data/working/docs
mkdir -p /app/data/working/temp
mkdir -p /app/data/working/plugins
mkdir -p /app/data/working/logs
mkdir -p /app/data/working/pug

# Copy pug templates if the directory is empty (first run)
if [ -z "$(ls -A /app/data/working/pug 2>/dev/null)" ]; then
    cp -r /app/pug-templates/* /app/data/working/pug/ 2>/dev/null || true
fi

# Start Tomcat
exec catalina.sh run
