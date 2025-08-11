import { Observable, Observer, Subject, Subscription } from "rxjs";

export class TrackableSubject<T> {

	private subject = new Subject<T>();
	private count = 0;

	subscribe(observerOrNext?: Partial<Observer<T>> | ((value: T) => void)): Subscription {
		const sub = this.subject.subscribe(observerOrNext as any);
		this.count++;

		// This avoids a double unsubscribe that can happen when unsubscribing from both the observable and the subscription.
		let unsubscribed = false;

		const internalUnsub = sub.unsubscribe.bind(sub);
		sub.unsubscribe = () => {
			if (!unsubscribed) {
				unsubscribed = true;
				this.count--;
			}
			internalUnsub();
		}
		return sub;
	}

	next(result: T) {
		this.subject.next(result);
	}

	asObservable(): Observable<T> {
		return new Observable<T>(observer => {
			const sub = this.subscribe(observer);
			return () => sub.unsubscribe();
		});
	}

	hasSubscribers(): boolean {
		return this.count > 0;
	}
}