# Tour: frontend

Carpeta: [`frontend/src/app/tour`](../../../../frontend/src/app/tour). Solo tiene `domain` y `presentation`, porque no llama a la API.

## Ruta

`/recorrido` → `TourPage`, cargada con `loadComponent` directamente en `app.routes.ts` (sin archivo `tour.routes.ts`) y protegida con `iamGuard`. El enlace está en el menú lateral de `Layout`.

## Modelo (`domain/model/tour-chapter.ts`)

| Constante | Valor |
|---|---|
| `TOUR_VIDEO_URL` | `/recorrido/synhub-recorrido.mp4` |
| `TOUR_POSTER_URL` | `/recorrido/poster.jpg` |
| `TOUR_DURATION` | `149.5` (segundos) |
| `TOUR_CHAPTERS` | Lista de `TourChapter { id, title, start, description }`. Los tiempos `start` salen de la grabación (`demo-video/chapters.json`). |

Si se vuelve a grabar el video, hay que actualizar `TOUR_DURATION` y los `start` de cada capítulo.

## Vista `TourPage`

- `<video>` con póster, controles, un botón de descarga y velocidades de reproducción (`1×`, `1.5×`, `2×`).
- Línea de tiempo segmentada por capítulos: `currentIndex` y `progress` son `computed` sobre la signal `currentTime`, así que el capítulo activo y el avance de cada segmento se actualizan mientras se reproduce.
- Al seleccionar un capítulo, el video salta a su `start`.

## Pruebas

No tiene `*.spec.ts`.
