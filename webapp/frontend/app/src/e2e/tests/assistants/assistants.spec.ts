import { test, expect } from '../../fixtures/auth.fixture';
import { confirmDialog } from '../../fixtures/confirm-dialog.fixture';

const uuidRegex = '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}';

test.describe.configure({ mode: 'serial'});

test.describe('Assistants flow', () => {

	const ASSISTANT_NAME = `PlayWright Bot`;
	const UPDATED_NAME = `PlayWright Bot v2`;
	const MODEL_NAME = 'gemma3:4b';
	const TEST_PROMPT = 'You are an expert in the Playwright testing framework. Respond to questions on Playwright as if you were Shakespeare - in iambic pentameter.';
	const TEST_MESSAGE = 'Why should I use PlayWright instead of Cypress?';

	test('should have assistants and no conversations', async ({ page }) => {
		await page.goto('http://localhost:4200/assistants');
		await expect(page).toHaveURL(/assistants/);

		const conversationHeading = page.locator('xpath=//h4[text()=\'Active Conversations\']');
		const ul = conversationHeading.locator('xpath=following-sibling::*[1]');
		await expect(ul.locator('li')).toHaveCount(0);

		const assistantsHeading = page.locator('xpath=//h4[text()=\'Assistants\']');
		let assistantList = assistantsHeading.locator('xpath=following-sibling::*[1]');
		await expect(assistantList).toHaveCount(1);
	});

	test('create an assistant', async ({ page }) => {
		await page.goto('http://localhost:4200/assistants');
		await expect(page).toHaveURL(/assistants/);

		await page.getByText('New Assistant').click();
		await expect(page).toHaveURL(/assistants\/new/);

		await page.fill('input[name="assistantName"]', ASSISTANT_NAME);
		await page.check('input[name="hasMemory"]');
		await expect(page.locator('button[id="submitButton"]')).toBeDisabled();
		await page.selectOption('select[name="model"]', MODEL_NAME);
		await expect(page.locator('button[id="submitButton"]')).toBeEnabled();
		await page.fill('input[name="assistantTemperature"]', '0.7');
		await page.fill('textarea[name="assistantPrompt"]', TEST_PROMPT);

		await page.click('button[id="submitButton"]');

		await expect(page).toHaveURL(/assistants/);
		const assistantHeading = page.getByText(ASSISTANT_NAME);
		await expect(assistantHeading).toBeVisible();
		const modelBadge = page.locator('li').filter({ hasText: ASSISTANT_NAME }).locator('.badge');
		await expect(modelBadge).toBeVisible();
		await expect(modelBadge).toHaveText(MODEL_NAME);
	});

	test('can create a new chat with an assistant', async ({ page }) => {
		await page.goto('http://localhost:4200/assistants');
		await expect(page).toHaveURL(/assistants/);

		const assistantHeading = page.locator('.clickable-text').filter({ hasText: ASSISTANT_NAME });
		await assistantHeading.click();

		await expect(page).toHaveURL(new RegExp(`conversation/${uuidRegex}`));
		const submitButton = page.locator('i.bi-arrow-down');
		await expect(submitButton).toBeDisabled();

		await page.fill('textarea[name="aiQuery"]', TEST_MESSAGE);
		await expect(submitButton).toBeEnabled();
		await submitButton.click();

		await expect(page.locator('minty-spinner')).toBeVisible();
		await expect(page.locator('.spinner-grow')).toBeHidden();

		const messageContent = page.locator('div.well-bot > markdown').first();
		await expect(messageContent).toBeVisible();
		await expect(messageContent).not.toBeEmpty();

		// Need to ensure there is enough time to consume the entire llm response.
		let text: string;
		let newText: string;
		do {
			text = await messageContent.innerText();
			await page.waitForTimeout(1000);
			newText = await messageContent.innerText();
		} while (text === newText);
	});

	test('can edit an assistant', async ({ page }) => {
		await page.goto('/assistants');
		const listItemsSelector = 'h4:has-text("Assistants") + ul > li';

		const assistantNameLocator = (name: string) =>
			page.locator('span.clickable-text').getByText(name, { exact: true });

		const firstAssistantSelector = page
			.locator(listItemsSelector)
			.filter({ has: assistantNameLocator(ASSISTANT_NAME) });

		const updatedFirstAssistantSelector = page
			.locator(listItemsSelector)
			.filter({ has: assistantNameLocator(UPDATED_NAME) });

		const assistantRow = firstAssistantSelector;
		await expect(assistantRow).toBeVisible();

		// Click the pencil/edit button on the assistant row
		await assistantRow.locator('button:has(.bi-pencil-square)').click();
		await expect(page).toHaveURL(/\/assistants\/edit\//);
		await expect(page.locator('h5')).toContainText("Proud of you");

		// Change the name
		const nameInput = page.locator('#assistantName');
		await nameInput.clear();
		await nameInput.fill(UPDATED_NAME);
		await expect(nameInput).toHaveValue(UPDATED_NAME);

		// Confirm model is still correct
		await expect(page.locator('select[name="model"]')).toHaveValue(MODEL_NAME);

		// Save the edit
		await page.locator('#submitButton').click();
		await expect(page).toHaveURL(/\/assistants$/);

		// Updated name should now appear in the list
		const updatedRow = updatedFirstAssistantSelector;
		await expect(updatedRow).toBeVisible();
		// Old name should be gone
		await expect(firstAssistantSelector).not.toBeVisible();
	});

	test('can delete a chat', async ({ page }) => {
		await page.goto('http://localhost:4200/assistants');
		await expect(page).toHaveURL(/assistants/);
		const deleteButton = page.locator('h4:has-text("Active Conversations") ~ ul > li > div > button > i.bi-trash3');
		await deleteButton.click();
		await confirmDialog(page, 'Delete Conversation');

		const assistantHeading = page.locator('h4:has-text("Active Conversations") ~ ul');
		await expect(assistantHeading.locator('li')).toHaveCount(0);
	});

	test('delete assistant', async({ page }) => {
		await page.goto('http://localhost:4200/assistants');
		await expect(page).toHaveURL(/assistants/);
		const deleteButton = page.locator('h4:has-text("Assistants") + ul > li > div > button > i.bi-trash3');
		await deleteButton.click();
		await confirmDialog(page, 'Delete Assistant');

		const assistantHeading = page.locator('h4:has-text("Assistants") + ul');
		await expect(assistantHeading.locator('li')).toHaveCount(1);
	});

});