import { test, expect } from '../../fixtures/auth.fixture';
import { confirmDialog } from '../../fixtures/confirm-dialog.fixture';

test.describe('Workflow: playwright test', () => {

	const WORKFLOW_NAME = 'playwright test';

	// Replace this with your known-good output once you have a reference run.
	// Use a distinctive substring rather than the full output if LLM responses
	// have any variation between runs.
	const KNOWN_GOOD_OUTPUT = `{
  "startTime" : 1775303968.130548000,
  "endTime" : 1775303973.143110600,
  "runtime" : "0h 0m 5s",
  "logFile" : "playwright test - 2026-04-04 07_59_28 EDT.log",
  "name" : "playwright test - 2026-04-04 07:59:28 EDT",
  "results" : {
    "Query LLM" : [ {
      "id" : "",
      "text" : [ "🦎\\n" ],
      "data" : [ ]
    } ],
    "Emit Text" : [ ]
  },
  "errors" : {
    "Query LLM" : [ ],
    "Emit Text" : [ ]
  }
}`;

	test('find workflow, run it, confirm in-progress, wait for completion, verify results', async ({ page, context }) => {

		// ── 1. Navigate to Workflows and open "playwright test" ───────────────────
		await page.goto('/workflow');
		await expect(page.locator('h2', { hasText: 'Runnable Workflows' })).toBeVisible();

		const workflowCard = page.locator('.card', { hasText: WORKFLOW_NAME }).first();
		await expect(workflowCard).toBeVisible({ timeout: 10_000 });
		await workflowCard.locator('.clickable-text').click();

		await expect(page).toHaveURL(/\/workflow\/edit\//);
		await expect(page.locator('h2', { hasText: 'Edit or Run Workflow' })).toBeVisible();

		// ── 2. Run the workflow ───────────────────────────────────────────────────
		const runButton = page.locator('button.btn-success:has(.bi-play)');
		await expect(runButton).toBeVisible();
		await runButton.click();

		// Navigate back to the list — the run is async, it continues server-side
		await page.goto('/workflow');
		await expect(page.locator('h2', { hasText: 'Runnable Workflows' })).toBeVisible();

		// ── 3. Confirm the workflow appears as in-progress ────────────────────────
		// In-progress items have no result.id, so they show "Result Not Ready" badge
		// and a cancel (stop) button instead of download/log buttons.
		const resultsList = page.locator('ul.list-group').last(); // results list is in the lower section

		const inProgressItem = resultsList.locator('.list-group-item').filter({
			hasText: WORKFLOW_NAME
		}).filter({
			has: page.locator('.badge', { hasText: 'Result Not Ready' })
		});

		await expect(inProgressItem).toBeVisible({ timeout: 15_000 });

		// The cancel (stop) button should be visible while in progress
		await expect(inProgressItem.locator('.bi-sign-stop-fill')).toBeVisible();

		// ── 4. Wait for the workflow to complete ──────────────────────────────────
		// The result list polls every 5 seconds. We wait for the "Result Not Ready"
		// badge to disappear, meaning result.id is now populated.
		// Workflow timeout is generous — tune once you know typical run time.
		const completedItem = resultsList.locator('.list-group-item').filter({
			hasText: WORKFLOW_NAME
		}).filter({
			hasNot: page.locator('.badge', { hasText: 'Result Not Ready' })
		});

		await expect(completedItem).toBeVisible({ timeout: 5 * 60_000 });

		// Confirm it didn't fail — no red exclamation diamond
		await expect(completedItem.locator('.bi-exclamation-diamond')).not.toBeVisible();

		// Download and log buttons should now be present (only visible when result.id exists)
		await expect(completedItem.locator('.bi-download')).toBeVisible();
		await expect(completedItem.locator('.bi-file-earmark-text')).toBeVisible();

		// ── 5. Intercept the result API call and verify output ────────────────────
		// displayResultsFor() calls GET api/result?workflowId=... then opens a blob
		// in a new tab — we can't assert on that tab's content directly.
		// Instead we intercept the API response before the click.
		let capturedOutput: string | null = null;

		await page.route('**/api/result?**', async (route, request) => {
			// Only intercept GET, not DELETE
			if (request.method() === 'GET') {
				const response = await route.fetch();
				const body = await response.json();
				// ApiResult wraps the payload in .data
				const workflowResult = body.data;
				capturedOutput = workflowResult?.output ?? null;
				await route.fulfill({ response });
			} else {
				await route.continue();
			}
		});

		// Click the result name to trigger displayResultsFor()
		await completedItem.locator('.clickable-text').click();

		// Wait for the intercepted request to complete
		await page.waitForResponse(resp =>
			resp.url().includes('api/result') &&
			resp.request().method() === 'GET' &&
			resp.status() === 200,
			{ timeout: 15_000 }
		);

		// Assert on the captured output
		expect(capturedOutput).not.toBeNull();
		const parsed = JSON.parse(capturedOutput!);
		expect(parsed.results['Query LLM'][0].text[0].trim()).toEqual('🦎');

		// ── 6. Clean up — delete the result ──────────────────────────────────────
		// Remove the route intercept before we do anything else
		await page.unrouteAll();

		const deleteResultBtn = completedItem.locator('button:has(.bi-trash3)');
		await deleteResultBtn.click();
		await confirmDialog(page, 'Delete Result');

		await expect(completedItem).not.toBeVisible({ timeout: 5_000 });
	});

});

//todo: create workflow from file. save. re-open. add a task. save and run.
/*
const TEST_DATA = [
	{ a: 1, b: 2, c: 3, id: 1 },
	{ a: 4, b: 5, c: 6, id: 2 },
	{ a: 7, b: 8, c: 9, id: 3 },
	{ a: 10, b: 11, c: 12, id: 4 },
];
await page.locator('#docFile').setInputFiles({
	name: 'test-data.json',
	mimeType: 'application/json',
	buffer: Buffer.from(JSON.stringify(TEST_DATA))
});

const csv = ['a,b,c,id', '1,2,3,1', '4,5,6,2', '7,8,9,3', '10,11,12,4'].join('\n');
await page.locator('#docFile').setInputFiles({
	name: 'test-data.csv',
	mimeType: 'text/csv',
	buffer: Buffer.from(csv)
});
*/