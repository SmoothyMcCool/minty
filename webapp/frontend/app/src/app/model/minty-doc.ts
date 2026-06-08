export interface DocumentSection {
	id: string;
	documentId: string;
	sequenceOrder: number;
	parentIndex: number;
	level: number;
	title: string;
	content: string;
	created: Date;
}

export interface MintyDoc {
	id: string;
	title: string;
	ownerId: string;
	projectId: string;
	vectorized: boolean;
	created: Date;
	updated: Date;
	summary: string;
	sections: DocumentSection[];
