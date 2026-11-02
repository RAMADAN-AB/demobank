import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '../services/auth.service';

describe('jwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('adds the bearer token to API requests when logged in', () => {
    authService.saveToken('jwt-token');

    http.get('/api/accounts/me').subscribe();

    const req = httpMock.expectOne('/api/accounts/me');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush([]);
  });

  it('sends no Authorization header when not logged in', () => {
    http.get('/api/accounts/me').subscribe();

    const req = httpMock.expectOne('/api/accounts/me');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });

  it('never sends the token to the auth endpoints', () => {
    authService.saveToken('jwt-token');

    http.post('/api/auth/login', {}).subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({ token: 'new-token' });
  });
});
