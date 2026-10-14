#!/bin/sh
# Generates rum-config.js from environment variables so the same image works
# in Docker Compose and in Kubernetes without rebuilding.
cat > /usr/share/nginx/html/rum-config.js <<JS
window.RUM_CONFIG = {
  applicationId: "${DD_RUM_APP_ID}",
  clientToken: "${DD_RUM_CLIENT_TOKEN}",
  site: "${DD_SITE:-datadoghq.eu}",
  env: "${DD_ENV:-course}",
  version: "${DD_VERSION:-1.0.0}"
};
JS
exec nginx -g 'daemon off;'
