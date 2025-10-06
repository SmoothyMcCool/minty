import { test, expect } from '../../fixtures/auth.fixture';

const uuidRegex = '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}';

test.describe.configure({ mode: 'serial'});

test.describe('Assistants flow', () => {

	test('should have no assistants, no shared assistants, no conversations', async ({ page }) => {
		await page.goto('http://localhost:4200/assistants');
		await expect(page).toHaveURL(/assistants/);

		const conversationHeading = page.locator('xpath=//h4[text()=\'Active Conversations\']');
		const ul = conversationHeading.locator('xpath=following-sibling::*[1]');
		await expect(ul.locator('li')).toHaveCount(0);

		const assistantsHeading = page.locator('xpath=//h4[text()=\'My Assistants\']');
		let assistantList = assistantsHeading.locator('xpath=following-sibling::*[1]');
		await expect(assistantList).toHaveJSProperty('tagName', 'P');

		const sharedAssistantsHeading = page.locator('xpath=//h4[text()=\'Shared Assistants\']');
		assistantList = assistantsHeading.locator('xpath=following-sibling::*[1]');
		await expect(assistantList).toHaveJSProperty('tagName', 'P');
	});

	test('create an assistant', async ({ page }) => {
		await page.goto('http://localhost:4200/assistants');
		await expect(page).toHaveURL(/assistants/);

		await page.getByText('New Assistant').click();
		await expect(page).toHaveURL(/assistants\/new/);

		await page.fill('input[name="assistantName"]', 'PlayWright Bot');
		await page.check('input[name="hasMemory"]');
		await expect(page.locator('button[id="submitButton"]')).toBeDisabled();
		await page.selectOption('select[name="model"]', 'gemma3:12b');
		await expect(page.locator('button[id="submitButton"]')).toBeEnabled();
		await page.fill('input[name="assistantTemperature"]', '0.7');
		await page.fill('textarea[name="assistantPrompt"]', 'You are an expert in the Playwright testing framework. Respond to questions on Playwright as if you were Shakespeare - in iambic pentameter.');

		await page.click('button[id="submitButton"]');

		await expect(page).toHaveURL(/assistants/);
		const assistantHeading = page.locator('xpath=//h5[text()=\'PlayWright Bot\']');
		await expect(assistantHeading).toBeVisible();
		const modelBadge = assistantHeading.locator('xpath=../following-sibling::*[1]/span');
		await expect(modelBadge).toBeVisible();
		await expect(modelBadge).toHaveText('gemma3:12b');
	});

	test('can create a new chat with an assistant', async ({ page }) => {
		await page.goto('http://localhost:4200/assistants');
		await expect(page).toHaveURL(/assistants/);

		const assistantHeading = page.locator('xpath=//h5[text()=\'PlayWright Bot\']');
		const chatButton = assistantHeading.locator('xpath=../../following-sibling::*[1]')
		await chatButton.click();

		await expect(page).toHaveURL(new RegExp(`conversation/${uuidRegex}`));
		const okeyDokeyButton = page.locator('text=Okey Dokey');
		await expect(okeyDokeyButton).toBeDisabled();

		await page.fill('textarea[name="aiQuery"]', 'Why should I use PlayWright instead of Cypress?');
		await expect(okeyDokeyButton).toBeEnabled();
		await okeyDokeyButton.click();

		await expect(page.locator('.spinner-grow')).toBeVisible();
		await expect(page.locator('markdown')).toBeVisible();

		// Need to ensure there is enough time to consume the entire llm response.
		let text: string;
		let newText: string;
		do {
			text = await page.locator('markdown').innerText();
			await page.waitForTimeout(1000);
			newText = await page.locator('markdown').innerText();
		} while (text === newText);
	});

	test('chat gets a name and shows the correct model', async ({ page }) => {
	});

	test('can resume a chat with an assistant', async ({ page }) => {
	});

	test('can delete a chat', async ({ page }) => {
	});

	test('delete assistant', async({ page }) => {
		await page.goto('http://localhost:4200/assistants');
		await expect(page).toHaveURL(/assistants/);
		const assistantHeading = page.locator('xpath=//h5[text()=\'PlayWright Bot\']');
		const deleteButton = assistantHeading.locator('xpath=../../../preceding-sibling::*[1]/*[2]');
		await deleteButton.click();

		await expect(page.getByText('Delete Assistant')).toBeVisible();

		const confirmButton = page.getByRole('button', { name: 'Confirm' });
		await expect(confirmButton).toBeVisible();
		await confirmButton.click();

		const assistantsHeading = page.locator('xpath=//h4[text()=\'My Assistants\']');
		let assistantList = assistantsHeading.locator('xpath=following-sibling::*[1]');
		await expect(assistantList).toHaveJSProperty('tagName', 'P');
	});
});