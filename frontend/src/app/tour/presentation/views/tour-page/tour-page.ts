import { Component, ElementRef, computed, signal, viewChild } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import {
  TOUR_CHAPTERS, TOUR_DURATION, TOUR_POSTER_URL, TOUR_VIDEO_URL,
} from '../../../domain/model/tour-chapter';

@Component({
  selector: 'app-tour-page',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './tour-page.html',
})
export class TourPage {
  readonly chapters = TOUR_CHAPTERS;
  readonly videoUrl = TOUR_VIDEO_URL;
  readonly posterUrl = TOUR_POSTER_URL;
  readonly rates = [1, 1.5, 2];
  readonly ends = TOUR_CHAPTERS.map((_, i) => TOUR_CHAPTERS[i + 1]?.start ?? TOUR_DURATION);

  private readonly video = viewChild.required<ElementRef<HTMLVideoElement>>('video');
  readonly currentTime = signal(0);
  readonly rate = signal(1);

  readonly currentIndex = computed(() => {
    const t = this.currentTime();
    let i = 0;
    while (i + 1 < this.chapters.length && t >= this.chapters[i + 1].start) i++;
    return i;
  });
  readonly current = computed(() => this.chapters[this.currentIndex()]);

  /** Avance 0-1 de cada segmento de la línea de tiempo */
  readonly progress = computed(() => this.chapters.map((c, k) => {
    const i = this.currentIndex();
    if (k !== i) return k < i ? 1 : 0;
    return Math.min(1, Math.max(0, (this.currentTime() - c.start) / (this.ends[k] - c.start)));
  }));

  onTimeUpdate(): void {
    this.currentTime.set(this.video().nativeElement.currentTime);
  }

  goTo(index: number): void {
    const k = Math.max(0, Math.min(this.chapters.length - 1, index));
    const el = this.video().nativeElement;
    el.currentTime = this.chapters[k].start + 0.05;
    this.currentTime.set(el.currentTime);
    el.play().catch(() => {});
  }

  setRate(rate: number): void {
    this.rate.set(rate);
    this.video().nativeElement.playbackRate = rate;
  }

  formatTime(seconds: number): string {
    return `${Math.floor(seconds / 60)}:${String(Math.floor(seconds % 60)).padStart(2, '0')}`;
  }
}
