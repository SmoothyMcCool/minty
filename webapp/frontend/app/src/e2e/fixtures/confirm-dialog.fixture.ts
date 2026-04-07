import { Page, expect } from '@playwright/test';

/**
 * Confirms a minty-confirmation-dialog by matching its modal title,
 * then clicking the primary (checkmark) confirm button.
 *
 * Buttons are icon-only (bi-check-lg / bi-x-lg) so we locate by
 * btn-primary in the footer rather than by role name.
 */
export async function confirmDialog(page: Page, title: string): Promise<void> {
	const dialog = page.locator('.modal.show').filter({
		has: page.locator('.modal-title', { hasText: title })
	});
	await expect(dialog).toBeVisible({ timeout: 5_000 });
	await dialog.locator('.modal-footer .btn-primary').click();
	await expect(dialog).not.toBeVisible({ timeout: 5_000 });
}