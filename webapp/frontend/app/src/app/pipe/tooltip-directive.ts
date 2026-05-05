import { Directive, ElementRef, Input, OnDestroy, OnInit, HostListener } from '@angular/core';

declare var bootstrap: any;

@Directive({
	selector: '[mintyTooltip]',
	standalone: true
})
export class TooltipDirective implements OnInit, OnDestroy {

	@Input('mintyTooltip') tooltipText: string = '';
	@Input() tooltipPlacement: 'top' | 'bottom' | 'left' | 'right' = 'right';

	private instance: any = null;
	private clicking = false;

	constructor(private el: ElementRef) { }

	ngOnInit() {
		this.instance = new bootstrap.Tooltip(this.el.nativeElement, {
			title: this.tooltipText,
			placement: this.tooltipPlacement,
			trigger: 'manual'
		});
	}

	@HostListener('mouseenter')
	onMouseEnter() {
		if (this.instance && !this.clicking) {
			this.instance.show();
		}
	}

	@HostListener('mouseleave')
	onMouseLeave() {
		// Skip hide() if we're in the middle of a click -
		// hide() queues async callbacks that blow up after DOM removal
		if (this.instance && !this.clicking) {
			this.instance.hide();
		}
	}

	@HostListener('mousedown')
	onMouseDown() {
		// Set flag as early as possible in the click sequence,
		// before mouseleave can fire
		this.clicking = true;
		this.safeDispose();
	}

	@HostListener('click')
	onClick() {
		this.clicking = false;
	}

	ngOnDestroy() {
		this.safeDispose();
	}

	private safeDispose() {
		if (this.instance) {
			try {
				this.instance.dispose();
			} catch (e) { }
			this.instance = null;
		}
	}
}