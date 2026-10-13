import { TOUR_CHAPTERS, TOUR_DURATION, TOUR_POSTER_URL, TOUR_VIDEO_URL } from './tour-chapter';

describe('Capítulos del tour', () => {
  it('el primero empieza en 0', () => {
    expect(TOUR_CHAPTERS[0].start).toBe(0);
  });

  it('los ids son únicos', () => {
    const ids = TOUR_CHAPTERS.map((c) => c.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it('los inicios crecen y quedan dentro de la duración del video', () => {
    TOUR_CHAPTERS.forEach((chapter, i) => {
      if (i > 0) expect(chapter.start).toBeGreaterThan(TOUR_CHAPTERS[i - 1].start);
      expect(chapter.start).toBeLessThan(TOUR_DURATION);
      expect(chapter.title.trim()).not.toBe('');
    });
  });

  it('el video y el póster se sirven desde /recorrido', () => {
    expect(TOUR_VIDEO_URL).toBe('/recorrido/synhub-recorrido.mp4');
    expect(TOUR_POSTER_URL).toBe('/recorrido/poster.jpg');
  });
});
