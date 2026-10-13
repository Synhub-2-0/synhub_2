import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TourPage } from './tour-page';
import { TOUR_CHAPTERS } from '../../../domain/model/tour-chapter';

describe('TourPage', () => {
  let fixture: ComponentFixture<TourPage>;
  let page: TourPage;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TourPage] }).compileComponents();
    fixture = TestBed.createComponent(TourPage);
    page = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('se crea con un capítulo por entrada', () => {
    expect(page).toBeTruthy();
    expect(page.chapters).toBe(TOUR_CHAPTERS);
    expect(page.ends).toHaveLength(TOUR_CHAPTERS.length);
  });

  it('el capítulo actual sigue al tiempo del video', () => {
    expect(page.currentIndex()).toBe(0);
    page.currentTime.set(TOUR_CHAPTERS[2].start + 0.1);
    expect(page.currentIndex()).toBe(2);
    expect(page.current().id).toBe(TOUR_CHAPTERS[2].id);
  });

  it('la barra de progreso completa los capítulos anteriores y avanza el actual', () => {
    const second = TOUR_CHAPTERS[1];
    page.currentTime.set(second.start + (page.ends[1] - second.start) / 2);
    const progress = page.progress();
    expect(progress[0]).toBe(1);
    expect(progress[1]).toBeCloseTo(0.5, 5);
    expect(progress.slice(2).every((p) => p === 0)).toBe(true);
  });

  it('formatea los segundos como m:ss', () => {
    expect(page.formatTime(0)).toBe('0:00');
    expect(page.formatTime(149.5)).toBe('2:29');
  });
});
