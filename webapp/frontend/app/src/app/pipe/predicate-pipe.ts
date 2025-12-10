import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
	name: 'predicate',
	pure: false
})
export class PredicatePipe implements PipeTransform {
	transform(items: any[], predicate: (item: any) => boolean): any[] {
		return items?.filter(predicate) ?? [];
	}

}