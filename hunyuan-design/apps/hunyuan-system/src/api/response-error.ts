export function resolveResponseErrorMessage(responseData: unknown) {
  if (!responseData || typeof responseData !== 'object') {
    return '';
  }

  const data = responseData as Record<string, unknown>;
  for (const field of ['msg', 'error', 'message']) {
    const value = data[field];
    if (typeof value === 'string' && value.trim()) {
      return value;
    }
  }
  return '';
}
