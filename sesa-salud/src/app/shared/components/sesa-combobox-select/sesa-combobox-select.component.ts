/**
 * Select tipo combobox con búsqueda (mismo patrón visual que Hospitalización).
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  forwardRef,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormsModule,
  NG_VALUE_ACCESSOR,
} from '@angular/forms';

export interface SesaComboboxOption {
  value: string;
  label: string;
}

@Component({
  standalone: true,
  selector: 'sesa-combobox-select',
  imports: [CommonModule, FormsModule],
  templateUrl: './sesa-combobox-select.component.html',
  styleUrl: './sesa-combobox-select.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SesaComboboxSelectComponent),
      multi: true,
    },
  ],
  host: { class: 'sesa-combobox-host' },
})
export class SesaComboboxSelectComponent
  implements ControlValueAccessor, OnChanges
{
  @Input({ required: true }) options: SesaComboboxOption[] = [];
  /** Texto del campo cuando no hay selección */
  @Input() placeholder = 'Buscar o seleccionar…';
  /** Encabezado del listado cuando no hay filtro de texto */
  @Input() sectionLabel = 'Opciones';
  @Input() inputId = '';
  @Input() ariaLabel = '';
  /**
   * Si es true, emite `number` (o `null` si la opción tiene value '') — útil con FormControl numérico (ej. epsId).
   */
  @Input() numberValue = false;

  searchText = '';
  open = false;
  disabled = false;

  private value = '';
  private blurTimer: ReturnType<typeof setTimeout> | null = null;
  private onChange: (v: unknown) => void = () => {};
  private onTouched: () => void = () => {};

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['options']) {
      this.syncSearchFromValue();
    }
  }

  get dropdownSectionLabel(): string {
    const q = this.searchText.trim();
    if (q) {
      return `Coincidencias (${this.filtered.length})`;
    }
    return this.sectionLabel;
  }

  get filtered(): SesaComboboxOption[] {
    const q = this.searchText.trim().toLowerCase();
    if (!q) {
      return this.options;
    }
    return this.options.filter(
      (o) =>
        o.label.toLowerCase().includes(q) ||
        o.value.toLowerCase().includes(q),
    );
  }

  writeValue(v: unknown): void {
    if (v === null || v === undefined || v === '') {
      this.value = '';
    } else {
      this.value = String(v);
    }
    this.syncSearchFromValue();
  }

  registerOnChange(fn: (v: unknown) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  private syncSearchFromValue(): void {
    if (this.value === '') {
      // No copiar la etiqueta del placeholder al input: si no, el filtro la usa como
      // consulta y solo queda visible esa opción (p. ej. "Seleccione tipo de documento").
      this.searchText = '';
      return;
    }
    const o = this.options.find((x) => x.value === this.value);
    this.searchText = o?.label ?? String(this.value);
  }

  onFocusInput(): void {
    if (this.disabled) {
      return;
    }
    if (this.blurTimer) {
      clearTimeout(this.blurTimer);
      this.blurTimer = null;
    }
    this.open = true;
  }

  onBlurInput(): void {
    this.blurTimer = setTimeout(() => {
      this.open = false;
      this.syncSearchFromValue();
      this.onTouched();
      this.blurTimer = null;
    }, 180);
  }

  onSearchInput(): void {
    if (this.disabled) {
      return;
    }
    this.open = true;
  }

  toggleDrop(event: MouseEvent): void {
    event.preventDefault();
    if (this.disabled) {
      return;
    }
    this.open = !this.open;
    if (this.open) {
      this.onFocusInput();
    }
  }

  selectOption(opt: SesaComboboxOption, event: MouseEvent): void {
    event.preventDefault();
    if (this.blurTimer) {
      clearTimeout(this.blurTimer);
      this.blurTimer = null;
    }
    const raw = opt.value;
    this.value = raw;
    this.searchText = raw === '' ? '' : opt.label;
    let emitted: unknown = raw;
    if (raw === '') {
      emitted = this.numberValue ? null : '';
    } else if (this.numberValue) {
      const n = Number(raw);
      emitted = Number.isNaN(n) ? raw : n;
    }
    this.onChange(emitted);
    this.open = false;
  }

  isSelected(opt: SesaComboboxOption): boolean {
    return this.value === opt.value;
  }
}
