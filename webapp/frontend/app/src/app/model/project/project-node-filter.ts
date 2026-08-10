import { ProjectNode } from './project-node';

export function getNodeFileName(path: string): string {
	if (!path) return '';

	// Normalize Windows backslashes to forward slashes
	const normalized = path.replace(/\\/g, '/');

	// Remove trailing slash if present
	const trimmed = normalized.endsWith('/') ? normalized.slice(0, -1) : normalized;

	const lastSlash = trimmed.lastIndexOf('/');

	const final = lastSlash >= 0
		? trimmed.substring(lastSlash + 1)
		: trimmed;

	return final ? final : '/';
}

/**
 * Determines whether a node should be visible under a given filter text.
 * Leaf nodes (File / Conversation) match on their own name. Folders match
 * only if at least one leaf descendant, at any depth, matches — so an
 * empty (post-filter) folder is correctly hidden rather than shown blank.
 *
 * `allNodes` should be the full flat list of nodes for the project (not
 * just the current subtree), since a folder needs to search its entire
 * descendant tree regardless of how deep the current component sits.
 */
export function nodeMatchesFilter(node: ProjectNode, allNodes: ProjectNode[], filterText: string): boolean {
	const text = (filterText ?? '').toString().trim().toLowerCase();
	if (!text) {
		return true;
	}

	if (node.type !== 'Folder') {
		return getNodeFileName(node.path).toLowerCase().includes(text);
	}

	const prefix = node.path.endsWith('/') ? node.path : node.path + '/';
	return allNodes.some(n =>
		n.type !== 'Folder' &&
		n.path.startsWith(prefix) &&
		getNodeFileName(n.path).toLowerCase().includes(text)
	);
}
