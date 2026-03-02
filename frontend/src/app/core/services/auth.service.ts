import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  isAuthenticated = signal(false);

  login(credentials: any) {
    console.log('AuthService communicating with API...', credentials);
    this.isAuthenticated.set(true);
  }

  logout() {
    this.isAuthenticated.set(false);
  }
}
