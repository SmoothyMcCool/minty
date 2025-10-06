import { test, expect } from '../../fixtures/auth.fixture';

//let authToken: string;
//let context: BrowserContext;
//let page: Page;

test.describe('Logout flow', () => {

	test('should log out and redirect to login page', async ({ page }) => {
		// Go to the main app page while logged in
		await page.goto('http://localhost:4200/assistants');
		await expect(page).toHaveURL(/assistants/);

		// Trigger logout (adjust selector as needed)
		let button = page.getByText('Logout');
		await button.click();

		// Wait for redirect to login
		await page.waitForURL(/login/);
		await expect(page).toHaveURL(/login/);
		await expect(page.locator('button[id="loginButton"]')).toBeVisible();
	});
});