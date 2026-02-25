# DOCKERFILE SIMPLE - Una sola etapa
FROM node:18-alpine

# Instalar herramientas para extraer APK
RUN apk add --no-cache unzip binutils

WORKDIR /app

# Copiar package.json
COPY package*.json ./
COPY tsconfig*.json ./

# Instalar dependencias
RUN npm install --legacy-peer-deps --loglevel=verbose

# Copiar código fuente
COPY src ./src

# Compilar TypeScript
RUN npm run build

# Crear directorio uploads y dar permisos al usuario node
RUN mkdir -p uploads/apks && chown -R node:node /app

# Cambiar a usuario node (UID 1000)
USER node

EXPOSE 3000 3001

CMD ["node", "dist/main"]
