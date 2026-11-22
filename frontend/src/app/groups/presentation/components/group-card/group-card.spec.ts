import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { GroupCard } from './group-card';
import { Group } from '../../../domain/model/group.entity';

describe('GroupCard', () => {
  let component: GroupCard;
  let fixture: ComponentFixture<GroupCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GroupCard],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(GroupCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('group', new Group({
      id: 1, name: 'Diseño & Branding Collab', imgUrl: '', description: 'Equipo de diseño', code: 'ABC123', memberCount: 6,
    }));
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('el detalle se abre en /groups/{id} sin datos de rol en la URL', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.openGroupDetails();
    expect(navigate).toHaveBeenCalledWith(['/groups', 1]);
  });

  it('shows the group name', () => {
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Diseño & Branding Collab');
  });
});
