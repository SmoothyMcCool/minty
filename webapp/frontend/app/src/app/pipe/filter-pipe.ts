import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
	name: 'filter'
})
export class FilterPipe implements PipeTransform {
	transform(items: any[], filter: any) {
		if (!items || !filter) {
			return items;
		}
		const result = items.filter(item => item.shared === filter.shared);
		return result;
	}

}