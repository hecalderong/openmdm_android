import { describe, expect, it } from 'vitest';
import router from '../src/router';

describe('router', () => {
  it('registers core application routes', () => {
    const routeNames = router.getRoutes().map((route) => route.name);

    expect(routeNames).toContain('login');
    expect(routeNames).toContain('dashboard');
    expect(routeNames).toContain('devices');
    expect(routeNames).toContain('apps');
    expect(routeNames).toContain('users');
    expect(routeNames).toContain('settings');
  });
});
