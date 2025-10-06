import { request, FullConfig } from '@playwright/test';

async function globalSetup(config: FullConfig) {
	const baseURL = config.projects[0].use?.baseURL ?? 'http://localhost:4200';

	// Create API context for login
	const apiContext = await request.newContext({
		baseURL,
		extraHTTPHeaders: {
			Authorization: 'Basic ' + Buffer.from('tom:Lcbo!234').toString('base64'),
		},
	});

	const response = await apiContext.post('/api/login');
	if (!response.ok()) throw new Error('Login failed');

	let authToken = response.headers()['x-auth-token'];
	if (!authToken) {
		const json = await response.json();
		authToken = json['x-auth-token'];
	}

	// Create a browser context and inject token into localStorage
	const { chromium } = require('@playwright/test');
	const browser = await chromium.launch();
	const context = await browser.newContext();
	const page = await context.newPage();

	await page.addInitScript(token => {
		sessionStorage.setItem('x-auth-token', token);
	}, authToken);

	// Visit a page so Angular initializes localStorage for storageState
	await page.goto(baseURL);
	await context.storageState({ path: 'storageState.json' });

	await browser.close();
	await apiContext.dispose();
}

export default globalSetup;