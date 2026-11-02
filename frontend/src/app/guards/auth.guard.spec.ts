import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';

import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  const runGuard = () => TestBed.runInInjectionContext(
    () => authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
  );

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['isLoggedIn']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    });
  });

  it('allows navigation when logged in', () => {
    authService.isLoggedIn.and.returnValue(true);

    expect(runGuard()).toBeTrue();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('redirects to /login when not logged in', () => {
    authService.isLoggedIn.and.returnValue(false);

    expect(runGuard()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
