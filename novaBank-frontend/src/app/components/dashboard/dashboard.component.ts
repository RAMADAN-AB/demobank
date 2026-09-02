import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe, CurrencyPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { BankingService, AccountResponse, TransactionResponse } from '../../services/banking.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe, CurrencyPipe],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  private bankingService = inject(BankingService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  accounts: AccountResponse[] = [];
  selectedAccount: AccountResponse | null = null;
  transactions: TransactionResponse[] = [];

  isLoading = false;
  errorMessage = '';
  successMessage = '';

  transferForm: FormGroup = this.fb.group({
    targetAccountNumber: ['', Validators.required],
    amount: [null, [Validators.required, Validators.min(0.01)]],
    description: ['']
  });

  ngOnInit(): void {
    this.loadAccounts();
  }

  // Remplace le `track` des blocs @for (Angular 17+), indisponible en Angular 16
  trackById(index: number, item: { id: number }): number {
    return item.id;
  }

  loadAccounts(): void {
    this.bankingService.getMyAccounts().subscribe({
      next: (data) => {
        this.accounts = data;
        if (data.length > 0) {
          // Par défaut, sélectionner le premier compte
          this.selectAccount(data[0]);
        }
      },
      error: (err) => console.error('Erreur chargement comptes', err)
    });
  }

  selectAccount(acc: AccountResponse): void {
    this.selectedAccount = acc;
    this.loadTransactions(acc.accountNumber);
  }

  loadTransactions(accountNumber: string): void {
    this.bankingService.getHistory(accountNumber).subscribe({
      next: (data) => this.transactions = data,
      error: (err) => console.error('Erreur chargement transactions', err)
    });
  }

  onCreateAccount(): void {
    this.isLoading = true;
    this.bankingService.createAccount().subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Compte créé avec succès !';
        this.loadAccounts();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Erreur lors de la création du compte.';
      }
    });
  }

  onTransfer(): void {
    if (this.transferForm.invalid || !this.selectedAccount) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const payload = {
      sourceAccountNumber: this.selectedAccount.accountNumber,
      ...this.transferForm.value
    };

    this.bankingService.transfer(payload).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Virement effectué avec succès !';
        this.transferForm.reset();
        this.loadAccounts(); // Recharger pour maj le solde et les transactions
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Échec du virement. Vérifiez le solde ou le compte destinataire.';
      }
    });
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
