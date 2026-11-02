import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  const TOKEN_KEY = 'demobank_jwt_token';
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('login posts credentials and stores the returned token', () => {
    const credentials = { email: 'alice@demobank.test', password: 'secret' };

    service.login(credentials).subscribe(response => {
      expect(response.token).toBe('jwt-token');
    });

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(credentials);
    req.flush({ token: 'jwt-token' });

    expect(localStorage.getItem(TOKEN_KEY)).toBe('jwt-token');
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('register posts the new user and stores the returned token', () => {
    const user = { firstName: 'Alice', lastName: 'Martin', email: 'alice@demobank.test', password: 'secret' };

    service.register(user).subscribe();

    const req = http.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(user);
    req.flush({ token: 'jwt-token' });

    expect(service.getToken()).toBe('jwt-token');
  });

  it('does not store a token when login fails', () => {
    service.login({ email: 'alice@demobank.test', password: 'wrong' }).subscribe({
      next: () => fail('login should have failed'),
      error: () => {}
    });

    http.expectOne('/api/auth/login').flush(null, { status: 403, statusText: 'Forbidden' });

    expect(service.isLoggedIn()).toBeFalse();
  });

  it('logout removes the token', () => {
    service.saveToken('jwt-token');

    service.logout();

    expect(service.getToken()).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
  });
});
