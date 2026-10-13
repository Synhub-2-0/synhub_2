export interface BaseResource {
  id: number;
}

// Marcador de las respuestas envoltorio ({ users: [...] }); cada feature declara sus campos.
// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export interface BaseResponse {}
