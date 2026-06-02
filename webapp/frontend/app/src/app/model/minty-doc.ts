export interface MintyDoc {
	id: string;
	title: string;
	state: string;
	ownerId: string;
	projectId: string;
	segments: string[];
	vectorized: boolean;
}
