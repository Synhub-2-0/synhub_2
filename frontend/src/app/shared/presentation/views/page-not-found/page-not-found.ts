import {Component, inject, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {MatButton} from '@angular/material/button';

@Component({
  imports: [
    MatButton
  ],
  selector: 'app-page-not-found',
  styleUrl: './page-not-found.css',
  templateUrl: './page-not-found.html',
})
export class PageNotFound implements OnInit {
  protected invalidPath: string = '';
  private route: ActivatedRoute = inject(ActivatedRoute);
  private router: Router = inject(Router);
  private countdownInterval: any;

  ngOnInit() {
    this.invalidPath = this.route.snapshot.url.map(url => url.path).join('/');
    this.startCountdown();
  }

  ngOnDestroy() {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
  }

  private startCountdown() {
    let countdown = 5;
    this.countdownInterval = setInterval(() => {
      countdown--;
      if (countdown <= 0) {
        clearInterval(this.countdownInterval);
        this.navigateToHome();
      }
    }, 1000);
  }

  protected navigateToHome() {
    this.router.navigate(['home']).then();
  }
}
