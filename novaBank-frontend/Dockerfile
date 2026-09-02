FROM nginx:alpine
WORKDIR /usr/share/nginx/html
# Nettoyage des fichiers web par défaut de NGINX
RUN rm -rf ./*
# Copie explicite du contenu du dossier de build
COPY dist/nova-bank-frontend/ ./
# Copie de la configuration NGINX
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]