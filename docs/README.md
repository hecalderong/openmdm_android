# Documentación OpenMDM-Android

Esta carpeta concentra la documentación técnica y operativa del proyecto, organizada por componente.

## Índice

- [Puesta en marcha](getting-started.md)
- [Arquitectura del sistema](architecture.md)
- [Backend (NestJS)](backend.md)
- [Frontend Dashboard (Vue)](frontend.md)
- [Android App (Java)](android.md)

## Mapa por componente

| Componente | Código fuente | Documento |
|---|---|---|
| Backend API + WebSocket | `backend/src` | [backend.md](backend.md) |
| Frontend Dashboard | `frontend/src` | [frontend.md](frontend.md) |
| Android MDM App | `android-app/app/src/main` | [android.md](android.md) |
| Infraestructura Docker | `docker/` | [getting-started.md](getting-started.md), [architecture.md](architecture.md) |

## Navegación recomendada

1. Comenzar en [Puesta en marcha](getting-started.md).
2. Revisar [Arquitectura del sistema](architecture.md).
3. Entrar al documento de tu componente objetivo.

## Referencias rápidas de código

- Backend entrypoint: `backend/src/main.ts`
- Backend app module: `backend/src/app.module.ts`
- Frontend entrypoint: `frontend/src/main.ts`
- Frontend rutas: `frontend/src/router/index.ts`
- Android actividad principal: `android-app/app/src/main/java/com/openmdm/mdm/ui/MainActivity.java`
