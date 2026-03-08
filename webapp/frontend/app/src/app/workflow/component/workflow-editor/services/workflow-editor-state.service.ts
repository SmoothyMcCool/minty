import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class WorkflowEditorStateService {

	canvasMouse = { x: 0, y: 0 };
	spawnPosition = { x: 0, y: 0 };

	updateMouse(pos: { x: number, y: number }) {
		this.canvasMouse = pos;
	}

	captureSpawnPosition() {
		this.spawnPosition = { ...this.canvasMouse };
	}
}
