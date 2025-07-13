import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type AlertType = 'failure' | 'info' | 'success';

export class Alert {
    type: AlertType;
    message: string;

    constructor(alert?: Alert) {
        if (alert) {
            this.type = alert.type;
            this.message = alert.message;
        }
    }
}

@Injectable({
    providedIn: 'root'
})
export class AlertService {

    public alert: BehaviorSubject<Alert> = new BehaviorSubject<Alert>(undefined);

    private outstandingRequests = 0;
    private completedRequests = 0;
    private _syncProgress = 0;
    public syncProgress: BehaviorSubject<number> = new BehaviorSubject<number>(this._syncProgress);

    clean(): void {
        this.outstandingRequests = 0;
        this.completedRequests = 0;
        this._syncProgress = 0;
    }

    public postAlert(alert: Alert): void {
        this.alert.next(alert);
    }

    public postSuccess(message: string): void {
        const alert = new Alert({
            type: 'success',
            message: message
        });

        this.postAlert(alert);
    }

    public postFailure(message: string): void {
        const alert = new Alert({
            type: 'failure',
            message: message
        });

        this.postAlert(alert);
    }

    public postFailures(messages: string[]): void {
        const alert = new Alert({
            type: 'failure',
            message: messages.join('<br/>')
        });

        this.postAlert(alert);
    }

    public addOutstandingRequest(): void {
        this.outstandingRequests++;
        this._syncProgress = Math.round((this.completedRequests / this.outstandingRequests) * 100);
        // Make sure that if there are some requests, we report more than 0 progress. This ensures the status bar will show
        // when there are outstanding requests with no responses yet.
        if (this._syncProgress === 0) {
            this._syncProgress = 1;
        }
        this.syncProgress.next(this._syncProgress);
    }

    public removeOutstandingRequest(): void {
        this.completedRequests++;
        this._syncProgress = Math.round((this.completedRequests / this.outstandingRequests) * 100);
        if (this.outstandingRequests === this.completedRequests) {
            this.outstandingRequests = 0;
            this.completedRequests = 0;
            this._syncProgress = 0;
        }
        this.syncProgress.next(this._syncProgress);

    }
}
