import { defineConfig } from '@playwright/test';
import * as path from 'path';

export default defineConfig({
	testDir: path.join(__dirname, './e2e/tests'),
	testMatch: '**/*.spec.ts',
	testIgnore: '**/setup/**',
	timeout: 30 * 1000,
	retries: 1,
	use: {
		baseURL: 'http://localhost:4200',
		browserName: 'chromium',
		headless: true,
	},
	webServer: {
		command: 'npm run start',
		port: 4200,
		reuseExistingServer: true,
	},
});