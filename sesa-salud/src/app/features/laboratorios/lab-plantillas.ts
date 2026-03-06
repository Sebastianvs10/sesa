/**
 * Plantillas de resultados por tipo de prueba de laboratorio.
 * Campos con unidades y rangos de referencia según práctica clínica.
 * Autor: Ing. J Sebastian Vargas S
 */

export interface CampoPlantillaLab {
  id: string;
  label: string;
  unidad?: string;
  ref?: string;
  tipo?: 'text' | 'number';
  placeholder?: string;
}

export interface PlantillaLab {
  id: string;
  nombre: string;
  /** Palabras clave para detectar este tipo (en minúsculas). Se usa la primera que coincida. */
  keywords: string[];
  campos: CampoPlantillaLab[];
}

/** Plantillas por tipo de prueba (Hemograma, Perfil lipídico, etc.). */
export const LAB_PLANTILLAS: PlantillaLab[] = [
  {
    id: 'HEMOGRAMA',
    nombre: 'Hemograma completo',
    keywords: ['hemograma', 'cbc', 'biometría hemática', 'cuadro hemático'],
    campos: [
      { id: 'hemoglobina', label: 'Hemoglobina', unidad: 'g/dL', ref: 'H: 13-17, M: 12-16', tipo: 'number', placeholder: 'ej. 14.5' },
      { id: 'hematocrito', label: 'Hematocrito', unidad: '%', ref: 'H: 40-52, M: 36-48', tipo: 'number', placeholder: 'ej. 42' },
      { id: 'leucocitos', label: 'Leucocitos', unidad: 'x10³/µL', ref: '4.5-11', tipo: 'number', placeholder: 'ej. 7.2' },
      { id: 'plaquetas', label: 'Plaquetas', unidad: 'x10³/µL', ref: '150-400', tipo: 'number', placeholder: 'ej. 250' },
      { id: 'vcm', label: 'VCM', unidad: 'fL', ref: '80-100', tipo: 'number', placeholder: 'ej. 88' },
      { id: 'hcm', label: 'HCM', unidad: 'pg', ref: '27-33', tipo: 'number', placeholder: 'ej. 30' },
      { id: 'chcm', label: 'CHCM', unidad: 'g/dL', ref: '32-36', tipo: 'number', placeholder: 'ej. 34' },
      { id: 'neutrofilos', label: 'Neutrófilos', unidad: '%', ref: '40-70', tipo: 'text', placeholder: 'ej. 58' },
      { id: 'linfocitos', label: 'Linfocitos', unidad: '%', ref: '20-40', tipo: 'text', placeholder: 'ej. 32' },
      { id: 'monocitos', label: 'Monocitos', unidad: '%', ref: '2-8', tipo: 'text', placeholder: 'ej. 5' },
      { id: 'eosinofilos', label: 'Eosinófilos', unidad: '%', ref: '1-4', tipo: 'text', placeholder: 'ej. 2' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Resumen clínico opcional' },
    ],
  },
  {
    id: 'PERFIL_LIPIDICO',
    nombre: 'Perfil lipídico',
    keywords: ['perfil lipídico', 'lipidico', 'colesterol', 'triglicéridos', 'ldl', 'hdl'],
    campos: [
      { id: 'colesterol_total', label: 'Colesterol total', unidad: 'mg/dL', ref: '<200', tipo: 'number', placeholder: 'ej. 185' },
      { id: 'ldl', label: 'LDL', unidad: 'mg/dL', ref: '<100 óptimo', tipo: 'number', placeholder: 'ej. 110' },
      { id: 'hdl', label: 'HDL', unidad: 'mg/dL', ref: 'H >40, M >50', tipo: 'number', placeholder: 'ej. 55' },
      { id: 'trigliceridos', label: 'Triglicéridos', unidad: 'mg/dL', ref: '<150', tipo: 'number', placeholder: 'ej. 120' },
      { id: 'vldl', label: 'VLDL', unidad: 'mg/dL', ref: '5-40', tipo: 'text', placeholder: 'opcional' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Riesgo cardiovascular, recomendaciones' },
    ],
  },
  {
    id: 'GLICEMIA',
    nombre: 'Glicemia',
    keywords: ['glicemia', 'glucosa', 'glucemia', 'glucosa en ayunas'],
    campos: [
      { id: 'glucosa_ayunas', label: 'Glucosa en ayunas', unidad: 'mg/dL', ref: '70-100', tipo: 'number', placeholder: 'ej. 92' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Normoglicemia / alteración / diabetes' },
    ],
  },
  {
    id: 'CREATININA',
    nombre: 'Creatinina sérica',
    keywords: ['creatinina', 'creatinina sérica'],
    campos: [
      { id: 'creatinina', label: 'Creatinina', unidad: 'mg/dL', ref: 'H: 0.7-1.3, M: 0.6-1.1', tipo: 'number', placeholder: 'ej. 0.9' },
      { id: 'tfg', label: 'TFG (estimada)', unidad: 'mL/min/1.73 m²', ref: '>90', tipo: 'text', placeholder: 'opcional' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Función renal' },
    ],
  },
  {
    id: 'UROANALISIS',
    nombre: 'Uroanálisis',
    keywords: ['uroanálisis', 'uroanalisis', 'orina', 'examen de orina'],
    campos: [
      { id: 'aspecto', label: 'Aspecto', tipo: 'text', placeholder: 'Límpido, turbio' },
      { id: 'color', label: 'Color', tipo: 'text', placeholder: 'Amarillo claro' },
      { id: 'densidad', label: 'Densidad', ref: '1.010-1.030', tipo: 'text', placeholder: 'ej. 1.018' },
      { id: 'ph', label: 'pH', ref: '4.5-8', tipo: 'text', placeholder: 'ej. 6' },
      { id: 'proteinas', label: 'Proteínas', tipo: 'text', placeholder: 'Negativo / trazas / positivo' },
      { id: 'glucosa', label: 'Glucosa', tipo: 'text', placeholder: 'Negativo / positivo' },
      { id: 'leucocitos', label: 'Leucocitos', tipo: 'text', placeholder: 'Por campo' },
      { id: 'hematies', label: 'Hematíes', tipo: 'text', placeholder: 'Por campo' },
      { id: 'bacterias', label: 'Bacterias', tipo: 'text', placeholder: 'Ausentes / presentes' },
      { id: 'observaciones', label: 'Observaciones', tipo: 'text', placeholder: 'Cilindros, cristales, etc.' },
    ],
  },
  {
    id: 'TSH_T4',
    nombre: 'TSH / T4 libre',
    keywords: ['tsh', 't4', 'tiroides', 'hormona tiroidea'],
    campos: [
      { id: 'tsh', label: 'TSH', unidad: 'µUI/mL', ref: '0.4-4', tipo: 'number', placeholder: 'ej. 2.1' },
      { id: 't4_libre', label: 'T4 libre', unidad: 'ng/dL', ref: '0.8-1.8', tipo: 'number', placeholder: 'ej. 1.2' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Eutiroidismo / hipo / hipertiroidismo' },
    ],
  },
  {
    id: 'TRANSAMINASAS',
    nombre: 'Transaminasas (AST/ALT)',
    keywords: ['transaminasas', 'ast', 'alt', 'got', 'gpt', 'hepatograma', 'pruebas hepáticas'],
    campos: [
      { id: 'ast', label: 'AST (GOT)', unidad: 'U/L', ref: 'H <40, M <32', tipo: 'number', placeholder: 'ej. 28' },
      { id: 'alt', label: 'ALT (GPT)', unidad: 'U/L', ref: 'H <41, M <33', tipo: 'number', placeholder: 'ej. 25' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Función hepática' },
    ],
  },
  {
    id: 'HBA1C',
    nombre: 'Hemoglobina glicosilada',
    keywords: ['hemoglobina glicosilada', 'hba1c', 'hb a1c', 'glicada'],
    campos: [
      { id: 'hba1c', label: 'HbA1c', unidad: '%', ref: '<5.7 normal, 5.7-6.4 prediabetes, ≥6.5 diabetes', tipo: 'number', placeholder: 'ej. 5.4' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Control glucémico' },
    ],
  },
  {
    id: 'PCR_VSG',
    nombre: 'PCR / VSG',
    keywords: ['pcr', 'vsg', 'proteína c reactiva', 'velocidad de sedimentación'],
    campos: [
      { id: 'pcr', label: 'PCR', unidad: 'mg/L', ref: '<5', tipo: 'number', placeholder: 'ej. 3' },
      { id: 'vsg', label: 'VSG', unidad: 'mm/h', ref: 'H <15, M <20', tipo: 'number', placeholder: 'ej. 8' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Inflamación / infección' },
    ],
  },
  {
    id: 'EMBARAZO',
    nombre: 'Prueba de embarazo (bhCG)',
    keywords: ['embarazo', 'bhcg', 'beta hcg', 'hcg', 'gestación'],
    campos: [
      { id: 'resultado', label: 'Resultado', tipo: 'text', placeholder: 'Positivo / Negativo' },
      { id: 'bhcg_cuantitativa', label: 'bhCG cuantitativa (si aplica)', unidad: 'mUI/mL', tipo: 'text', placeholder: 'opcional' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Comentario clínico' },
    ],
  },
  {
    id: 'UROCULTIVO',
    nombre: 'Urocultivo',
    keywords: ['urocultivo', 'cultivo de orina'],
    campos: [
      { id: 'desarrollo', label: 'Desarrollo', tipo: 'text', placeholder: 'Si / No' },
      { id: 'germen', label: 'Germen aislado', tipo: 'text', placeholder: 'ej. E. coli' },
      { id: 'recuento', label: 'Recuento', tipo: 'text', placeholder: 'UFC/mL' },
      { id: 'antibiograma', label: 'Antibiograma', tipo: 'text', placeholder: 'Sensible / Resistente a...' },
      { id: 'observaciones', label: 'Observaciones', tipo: 'text', placeholder: 'Comentarios' },
    ],
  },
  {
    id: 'COPROCULTIVO',
    nombre: 'Coprocultivo',
    keywords: ['coprocultivo', 'cultivo de heces'],
    campos: [
      { id: 'desarrollo', label: 'Desarrollo', tipo: 'text', placeholder: 'Si / No' },
      { id: 'germen', label: 'Germen aislado', tipo: 'text', placeholder: 'Salmonella, Shigella, etc.' },
      { id: 'antibiograma', label: 'Antibiograma', tipo: 'text', placeholder: 'Si aplica' },
      { id: 'observaciones', label: 'Observaciones', tipo: 'text', placeholder: 'Parásitos, leucocitos' },
    ],
  },
  {
    id: 'BUN',
    nombre: 'Nitrógeno ureico (BUN)',
    keywords: ['nitrógeno ureico', 'bun', 'urea'],
    campos: [
      { id: 'bun', label: 'BUN', unidad: 'mg/dL', ref: '7-20', tipo: 'number', placeholder: 'ej. 14' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Función renal / hidratación' },
    ],
  },
  {
    id: 'BILIRRUBINAS',
    nombre: 'Bilirrubinas',
    keywords: ['bilirrubina', 'bilirrubinas'],
    campos: [
      { id: 'total', label: 'Bilirrubina total', unidad: 'mg/dL', ref: '0.1-1.2', tipo: 'number', placeholder: 'ej. 0.8' },
      { id: 'directa', label: 'Bilirrubina directa', unidad: 'mg/dL', ref: '0-0.3', tipo: 'number', placeholder: 'ej. 0.2' },
      { id: 'indirecta', label: 'Bilirrubina indirecta', unidad: 'mg/dL', tipo: 'text', placeholder: 'calculada' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Hepatobiliar' },
    ],
  },
  {
    id: 'INR',
    nombre: 'INR / Tiempo de protrombina',
    keywords: ['inr', 'protrombina', 'tiempo de protrombina', 'tp', 'coagulación'],
    campos: [
      { id: 'inr', label: 'INR', ref: '0.8-1.2 (anticoagulados 2-3)', tipo: 'number', placeholder: 'ej. 1.0' },
      { id: 'tp', label: 'TP', unidad: 'seg', tipo: 'text', placeholder: 'opcional' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Coagulación' },
    ],
  },
  {
    id: 'PSA',
    nombre: 'Antígeno prostático (PSA)',
    keywords: ['psa', 'antígeno prostático', 'prostático'],
    campos: [
      { id: 'psa_total', label: 'PSA total', unidad: 'ng/mL', ref: '<4', tipo: 'number', placeholder: 'ej. 2.1' },
      { id: 'psa_libre', label: 'PSA libre (si aplica)', unidad: 'ng/mL', tipo: 'text', placeholder: 'opcional' },
      { id: 'interpretacion', label: 'Interpretación', tipo: 'text', placeholder: 'Comentario' },
    ],
  },
];

/**
 * Detecta la plantilla a usar según el texto de la prueba solicitada (detalle o tipoPrueba).
 */
export function getPlantillaParaPrueba(tipoPruebaODetalle: string): PlantillaLab | null {
  if (!tipoPruebaODetalle?.trim()) return null;
  const texto = tipoPruebaODetalle.toLowerCase();
  return LAB_PLANTILLAS.find((p) => p.keywords.some((k) => texto.includes(k))) ?? null;
}

/**
 * Serializa los valores de una plantilla a texto para guardar en resultado.
 */
export function serializarResultadoPlantilla(
  plantilla: PlantillaLab,
  valores: Record<string, string>
): string {
  const lineas: string[] = [];
  for (const campo of plantilla.campos) {
    const v = (valores[campo.id] ?? '').trim();
    if (!v) continue;
    const parte = campo.unidad ? `${campo.label}: ${v} ${campo.unidad}` : `${campo.label}: ${v}`;
    if (campo.ref) lineas.push(`${parte} (Ref: ${campo.ref})`);
    else lineas.push(parte);
  }
  return lineas.join('\n');
}
