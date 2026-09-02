import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AccountResponse {
  id: number;
  accountNumber: string;
  balance: number;
}

export interface TransferRequest {
  sourceAccountNumber: string;
  targetAccountNumber: string;
  amount: number;
  description: string;
}

export interface TransactionResponse {
  id: number;
  amount: number;
  description: string;
  timestamp: string;
  sourceAccount: string;
  targetAccount: string;
}

@Injectable({
  providedIn: 'root'
})
export class BankingService {
  private http = inject(HttpClient);
  private apiUrl = '/api';

  getMyAccounts(): Observable<AccountResponse[]> {
    return this.http.get<AccountResponse[]>(`${this.apiUrl}/accounts/me`);
  }

  createAccount(): Observable<AccountResponse> {
    return this.http.post<AccountResponse>(`${this.apiUrl}/accounts`, {});
  }

  transfer(request: TransferRequest): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.apiUrl}/transactions/transfer`, request);
  }

  getHistory(accountNumber: string): Observable<TransactionResponse[]> {
    return this.http.get<TransactionResponse[]>(`${this.apiUrl}/transactions/history/${accountNumber}`);
  }
}
