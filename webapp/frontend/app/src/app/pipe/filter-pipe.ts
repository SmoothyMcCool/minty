import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
    name: 'filter'
})
export class FilterPipe implements PipeTransform {
    transform(items: any[], filter: Object) {
        if (!items || !filter) {
            return items;
        }
        return items.filter(item => item.shared === filter);
    }
    
}