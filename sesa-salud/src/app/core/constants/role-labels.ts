/**
 * Etiquetas legibles para roles RBAC (alineado con backend RoleConstants).
 * Autor: Ing. J Sebastian Vargas S
 */

const LABELS: Record<string, string> = {
  SUPERADMINISTRADOR: 'Super administrador',
  ADMIN: 'Administrador',
  MEDICO: 'Médico',
  ODONTOLOGO: 'Odontólogo',
  BACTERIOLOGO: 'Bacteriólogo / laboratorio',
  ENFERMERO: 'Enfermero',
  ENFERMERA: 'Enfermera',
  JEFE_ENFERMERIA: 'Jefe de enfermería',
  AUXILIAR_ENFERMERIA: 'Auxiliar de enfermería',
  PSICOLOGO: 'Psicólogo',
  REGENTE_FARMACIA: 'Regente de farmacia',
  RECEPCIONISTA: 'Recepción',
  FACTURACION: 'Facturación',
  COORDINADOR_MEDICO: 'Coordinador médico',
  EBS: 'Equipo básico de salud',
  COORDINADOR_TERRITORIAL: 'Coordinador territorial',
  SUPERVISOR_APS: 'Supervisor APS',
  PACIENTE: 'Paciente',
};

/** Prioridad de visualización (roles más operativos primero). */
const ORDER: string[] = [
  'SUPERADMINISTRADOR',
  'ADMIN',
  'MEDICO',
  'ODONTOLOGO',
  'BACTERIOLOGO',
  'COORDINADOR_MEDICO',
  'ENFERMERO',
  'ENFERMERA',
  'JEFE_ENFERMERIA',
  'AUXILIAR_ENFERMERIA',
  'PSICOLOGO',
  'REGENTE_FARMACIA',
  'FACTURACION',
  'RECEPCIONISTA',
  'EBS',
  'COORDINADOR_TERRITORIAL',
  'SUPERVISOR_APS',
  'PACIENTE',
];

export function labelForRole(code: string): string {
  const c = code?.toUpperCase().replace(/^ROLE_/, '') ?? '';
  if (LABELS[c]) return LABELS[c];
  return c
    .split('_')
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');
}

export function sortRolesForPicker(roles: string[]): string[] {
  const norm = (r: string) => r.toUpperCase().replace(/^ROLE_/, '');
  return [...roles].sort((a, b) => {
    const na = norm(a);
    const nb = norm(b);
    const ia = ORDER.indexOf(na);
    const ib = ORDER.indexOf(nb);
    if (ia >= 0 && ib >= 0) return ia - ib;
    if (ia >= 0) return -1;
    if (ib >= 0) return 1;
    return na.localeCompare(nb);
  });
}
