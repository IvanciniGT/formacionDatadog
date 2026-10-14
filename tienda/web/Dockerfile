# Build context is demo/ (see docker-compose.yaml), so paths are relative to it.
FROM nginx:1.27-alpine
COPY web/index.html /usr/share/nginx/html/index.html
COPY web/entrypoint.sh /entrypoint.sh
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
ENTRYPOINT ["/entrypoint.sh"]
