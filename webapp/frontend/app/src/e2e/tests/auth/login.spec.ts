import test, { expect } from "@playwright/test";

test.describe('Login', () => {

	test.beforeEach(async ({ page }) => {
		await page.goto('/login');
	});

	test('unknown user fails login', async ({ page }) => {
		await page.fill('input[name="account"]', 'hello');
		await page.fill('input[name="password"]', 'there');
		await page.click('button[id="loginButton"]');
		await expect(page).toHaveURL(/login/);
		const div = page.locator('text=Try again.');
		await expect(div).toBeVisible();
	});

	test('bad password fails login', async ({ page }) => {
		await page.fill('input[name="account"]', 'tom');
		await page.fill('input[name="password"]', 'asdf');
		await page.click('button[id="loginButton"]');
		await expect(page).toHaveURL(/login/);
		const div = page.locator('text=Try again.');
		await expect(div).toBeVisible();
	});

	test('good credentials allow login', async ({ page }) => {
		await page.fill('input[name="account"]', 'tom');
		await page.fill('input[name="password"]', 'Lcbo!234');
		await page.click('button[id="loginButton"]');
		await expect(page).toHaveURL(/assistants/);
		await expect(page.locator('h1')).toContainText('Assistants');
	});
});