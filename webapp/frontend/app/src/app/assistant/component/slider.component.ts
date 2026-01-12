import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FilterPipe } from '../../pipe/filter-pipe';

@Component({
	selector: 'minty-slider',
	imports: [CommonModule, FormsModule, RouterModule, FilterPipe],
	templateUrl: 'slider.component.html',
	styleUrl: 'slider.component.css',
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => SliderComponent),
			multi: true
		}
	]
})
export class SliderComponent implements ControlValueAccessor {

	@Input() min: number;
	@Input() max: number;
	@Input() label: string;
	value: number;

	onChange = (_: any) => { };
	onTouched: any = () => { };

	valueChanged(value: number) {
		this.value = value;
		this.onChange(value);
		this.onTouched();
	}

	writeValue(obj: any): void {
		this.value = obj;
	}
	registerOnChange(fn: any): void {
		this.onChange = fn;
	}
	registerOnTouched(fn: any): void {
		this.onTouched = fn;
	}
	setDisabledState(isDisabled: boolean): void {
		// Nah.
	}
}
