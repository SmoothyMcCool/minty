import { test as base, expect, request } from '@playwright/test';

// Create a worker-scoped "authToken" fixture
type WorkerFixtures = {
	authToken: string;
};

// Define the worker-scoped fixture
const test = base.extend<{}, WorkerFixtures>({
	authToken: [
		async ({ }, use) => {
			const apiContext = await request.newContext({
				baseURL: 'http://localhost:4200',
				extraHTTPHeaders: {
					Authorization: 'Basic ' + Buffer.from('test:asdfqwer').toString('base64'),
				},
			});

			const response = await apiContext.post('/api/login');
			if (!response.ok()) throw new Error('Login failed');

			let authToken = response.headers()['x-auth-token'];
			if (!authToken) {
				const json = await response.json();
				authToken = json['x-auth-token'];
			}

			await use(authToken);
			await apiContext.dispose();
		},
		{ scope: 'worker' },
	],
});

// Extend "page" to inject sessionStorage before every page load
const authenticated = test.extend({
	page: async ({ page, authToken }, use) => {
		await page.addInitScript((token: string) => {
			sessionStorage.setItem('x-auth-token', token);
		}, authToken);
		await use(page);
	},
});

export { authenticated as test, expect };