import { environment } from './environment';
import { environment as development } from './environment.development';

describe('Entornos', () => {
  it('desarrollo apunta al backend local', () => {
    expect(development.production).toBe(false);
    expect(development.platformProviderApiBaseUrl).toBe('http://localhost:8080/api/v1');
  });

  it('el entorno activo define los mismos paths que desarrollo', () => {
    expect(Object.keys(environment).sort()).toEqual(Object.keys(development).sort());
    for (const key of Object.keys(development).filter((k) => k.endsWith('EndpointPath'))) {
      expect(environment[key as keyof typeof environment], key).toBe(development[key as keyof typeof development]);
    }
  });

  it('en producción la API es relativa (mismo origen detrás de nginx o CloudFront)', () => {
    // Las pruebas pueden compilarse con el reemplazo de desarrollo: solo se verifica si el entorno activo es el de producción.
    if (environment.production) expect(environment.platformProviderApiBaseUrl).toBe('/api/v1');
    else expect(environment.platformProviderApiBaseUrl).toBe(development.platformProviderApiBaseUrl);
  });
});
