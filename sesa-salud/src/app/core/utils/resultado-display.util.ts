/**
 * Utilidad para presentar resultados de laboratorio por ítem (etiqueta + valor).
 * Autor: Ing. J Sebastian Vargas S
 */

export interface ResultadoItem {
  etiqueta: string;
  valor: string;
}

/**
 * Parsea un texto de resultado con líneas "Etiqueta: valor" en ítems para mostrar
 * (etiqueta en negrita + valor). Si no hay ":", la línea entera es la etiqueta.
 */
export function parseResultadoToItems(resultado: string | null | undefined): ResultadoItem[] {
  if (!resultado?.trim()) return [];
  return resultado
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .map((line) => {
      const colon = line.indexOf(': ');
      if (colon > 0) {
        return { etiqueta: line.slice(0, colon).trim(), valor: line.slice(colon + 2).trim() };
      }
      return { etiqueta: line, valor: '' };
    });
}
