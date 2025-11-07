import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
	name: 'filter'
})
export class FilterPipe implements PipeTransform {
	transform(items: any[], filter: any) {
		if (!items || !filter) {
			return items;
		}
		const keys = Object.keys(filter);
		const result = items.filter(item => item[keys[0]] === filter[keys[0]]);
		return result;
	}

}