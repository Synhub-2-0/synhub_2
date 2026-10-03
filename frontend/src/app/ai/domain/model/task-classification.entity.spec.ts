import { parseReport } from './task-classification.entity';

describe('parseReport', () => {
  it('separa títulos, ítems y párrafos y marca las negritas', () => {
    const blocks = parseReport('# Informe\n\n## Riesgos\n- **Ana**: sobrecarga\n1. Reasignar\nTexto libre');
    expect(blocks.map((b) => b.type)).toEqual(['heading', 'heading', 'item', 'item', 'paragraph']);
    expect(blocks[2].segments).toEqual([{ text: 'Ana', bold: true }, { text: ': sobrecarga', bold: false }]);
    expect(blocks[3].segments).toEqual([{ text: 'Reasignar', bold: false }]);
  });

  it('devuelve vacío para texto vacío', () => {
    expect(parseReport('')).toEqual([]);
  });
});
