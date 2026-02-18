import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { AuthService } from '../../core/services/auth.service';
import { EmpresaCurrentService } from '../../core/services/empresa-current.service';
import { EmpresaService } from '../../core/services/empresa.service';

@Component({
  standalone: true,
  selector: 'sesa-mi-empresa-page',
  imports: [CommonModule, SesaCardComponent],
  templateUrl: './mi-empresa.page.html',
  styleUrl: './mi-empresa.page.scss',
})
export class MiEmpresaPageComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly empresaService = inject(EmpresaService);
  readonly empresaCurrent = inject(EmpresaCurrentService);

  loading = signal(false);
  error = signal<string | null>(null);
  success = signal(false);
  selectedFile: File | null = null;

  get canUploadLogo(): boolean {
    const role = this.auth.currentUser()?.role ?? '';
    return role === 'ADMIN' || role === 'SUPERADMINISTRADOR';
  }

  ngOnInit(): void {
    this.empresaCurrent.load();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    this.selectedFile = file ?? null;
    this.error.set(null);
    this.success.set(false);
  }

  uploadLogo(): void {
    if (!this.selectedFile) {
      this.error.set('Selecciona una imagen (PNG, JPG, WebP o SVG).');
      return;
    }
    this.error.set(null);
    this.success.set(false);
    this.loading.set(true);
    this.empresaService.uploadLogo(this.selectedFile).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        this.selectedFile = null;
        this.empresaCurrent.refresh();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || err.message || 'Error al subir el logo.');
      },
    });
  }
}
