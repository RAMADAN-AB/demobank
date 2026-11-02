import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { BankingService } from './banking.service';

describe('BankingService', () => {
  let service: BankingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(BankingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getMyAccounts fetches the current user accounts', () => {
    const accounts = [{ id: 1, accountNumber: 'FR76AAAAAAAAAA', balance: 1000 }];

    service.getMyAccounts().subscribe(result => expect(result).toEqual(accounts));

    const req = http.expectOne('/api/accounts/me');
    expect(req.request.method).toBe('GET');
    req.flush(accounts);
  });

  it('createAccount posts to the accounts endpoint', () => {
    service.createAccount().subscribe();

    const req = http.expectOne('/api/accounts');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 2, accountNumber: 'FR76BBBBBBBBBB', balance: 1000 });
  });

  it('transfer posts the transfer request', () => {
    const transfer = {
      sourceAccountNumber: 'FR76AAAAAAAAAA',
      targetAccountNumber: 'FR76BBBBBBBBBB',
      amount: 50,
      description: 'Loyer'
    };

    service.transfer(transfer).subscribe();

    const req = http.expectOne('/api/transactions/transfer');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(transfer);
    req.flush({});
  });

  it('getHistory fetches the history of the given account', () => {
    service.getHistory('FR76AAAAAAAAAA').subscribe(result => expect(result).toEqual([]));

    const req = http.expectOne('/api/transactions/history/FR76AAAAAAAAAA');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
