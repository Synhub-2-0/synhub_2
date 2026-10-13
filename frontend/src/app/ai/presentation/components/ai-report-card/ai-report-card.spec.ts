import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AiReportCard } from './ai-report-card';
import { AiApi } from '../../../infrastructure/ai.api';

describe('AiReportCard', () => {
  let fixture: ComponentFixture<AiReportCard>;
  let api: { classifyTask: ReturnType<typeof vi.fn>; getGroupReport: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    api = { classifyTask: vi.fn(), getGroupReport: vi.fn().mockReturnValue(of('## Resumen\n- **Ana**: sobrecarga')) };
    await TestBed.configureTestingModule({ imports: [AiReportCard], providers: [{ provide: AiApi, useValue: api }] }).compileComponents();
    fixture = TestBed.createComponent(AiReportCard);
  });

  it('sin grupo no pide el informe', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.componentInstance.generate();
    expect(api.getGroupReport).not.toHaveBeenCalled();
  });

  it('genera el informe del grupo y lo muestra en bloques', async () => {
    fixture.componentRef.setInput('groupId', 10);
    fixture.componentInstance.generate();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(api.getGroupReport).toHaveBeenCalledWith(10);
    expect(fixture.componentInstance.blocks().map((b) => b.type)).toEqual(['heading', 'item']);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Resumen');
  });
});
