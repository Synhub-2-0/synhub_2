export const environment = {
  production: true,
  // CloudFront sirve el front y reenvía /api/* al backend: mismo dominio, sin CORS.
  platformProviderApiBaseUrl: '/api/v1',
  platformProviderSignInEndpointPath: '/authentication/sign-in',
  platformProviderSignUpEndpointPath: '/authentication/sign-up',
  platformProviderUserEndpointPath: '/users',
  platformProviderProfileEndpointPath: '/users',
  platformProviderGroupsEndpointPath: '/groups',
  platformProviderTasksEndpointPath: '/tasks',
  platformProviderInvitationsEndpointPath: '/invitations',
  platformProviderRequestsEndpointPath: '/requests',
  platformProviderAiEndpointPath: '/ai',
  logoProviderApiBaseUrl: 'https://img.logo.dev'
};
