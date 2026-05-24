import { AfterViewInit, Directive, ElementRef, OnDestroy } from '@angular/core';

@Directive({
	selector: '[mermaidClipboard]',
	standalone: true
})
export class MermaidClipboardDirective
	implements AfterViewInit, OnDestroy {

	private observer!: MutationObserver;

	constructor(private el: ElementRef<HTMLElement>) { }

	ngAfterViewInit() {
		this.observer = new MutationObserver(() => this.process());
		this.observer.observe(this.el.nativeElement, {
			childList: true,
			subtree: true
		});

		this.process();
	}

	ngOnDestroy() {
		this.observer?.disconnect();
	}

	private process() {
		this.observer?.disconnect();

		try {
			const containers =
				this.el.nativeElement.querySelectorAll<HTMLElement>('.mermaid');

			containers.forEach(container => {
				const svg = container.querySelector('svg');
				if (!svg) {
					return;
				}

				// Avoid duplicates
				if (container.querySelector('.clipboard-btn')) {
					return;
				}

				const pngButton = this.createButton('bi bi-filetype-png', () => {
					this.onDownloadClick(container, 'png');
				});
				pngButton.style.right = '4px';

				const svgButton = this.createButton('bi bi-filetype-svg', () => {
					this.onDownloadClick(container, 'svg');
				});
				svgButton.style.right = '40px';

				container.style.position = 'relative';
				container.appendChild(pngButton);
				container.appendChild(svgButton);;
			});
		} finally {
			this.observer.observe(this.el.nativeElement, {
				childList: true,
				subtree: true
			});
		}
	}

	private createButton(iconClass: string, clickHandler: () => void): HTMLButtonElement {
		const button = document.createElement('button');
		button.className = 'btn btn-sm btn-light clipboard-button border border-success rounded p-1';

		const icon = document.createElement('i');
		icon.className = iconClass;

		button.appendChild(icon);
		button.addEventListener('click', clickHandler);

		button.style.position = 'absolute';
		button.style.top = '4px';

		return button;
	}

	private onDownloadClick(container: HTMLElement, type: string) {
		const svg = container.querySelector('svg') as SVGSVGElement | null;
		if (!svg) {
			return;
		}

		if (type === 'svg') {
			const serializer = new XMLSerializer();
			let svgString = serializer.serializeToString(svg);

			if (!svgString.includes('xmlns="http://www.w3.org/2000/svg"')) {
				svgString = svgString.replace('<svg', '<svg xmlns="http://www.w3.org/2000/svg"');
			}

			const blob = new Blob([svgString], { type: 'image/svg+xml' });
			const url = URL.createObjectURL(blob);

			const a = document.createElement('a');
			a.href = url;
			a.download = 'diagram.svg';
			a.click();
			URL.revokeObjectURL(url);

		} else if (type === 'png') {
			const serializer = new XMLSerializer();
			let svgString = serializer.serializeToString(svg);

			// Ensure xmlns is present (required for Firefox)
			if (!svgString.includes('xmlns="http://www.w3.org/2000/svg"')) {
				svgString = svgString.replace(
					'<svg',
					'<svg xmlns="http://www.w3.org/2000/svg"'
				);
			}

			// Encode safely (handles UTF-8)
			const encoded = encodeURIComponent(svgString);
			const dataUrl = 'data:image/svg+xml;charset=utf-8,' + encoded;

			const img = new Image();

			// Important for Firefox / cross-origin safety
			img.crossOrigin = 'anonymous';

			img.onload = () => {
				const canvas = document.createElement('canvas');

				// Prefer viewBox for correct sizing
				const viewBox = svg.viewBox.baseVal;
				const width = viewBox?.width || svg.clientWidth || 300;
				const height = viewBox?.height || svg.clientHeight || 150;

				// Apply scale factor
				canvas.width = width * 2;
				canvas.height = height * 2;

				const ctx = canvas.getContext('2d');
				if (!ctx) return;

				ctx.drawImage(img, 0, 0, canvas.width, canvas.height);

				canvas.toBlob(blob => {
					if (!blob) return;

					const url = URL.createObjectURL(blob);
					const a = document.createElement('a');
					a.href = url;
					a.download = 'diagram.png';
					document.body.appendChild(a);
					a.click();
					document.body.removeChild(a);
					URL.revokeObjectURL(url);
				}, 'image/png');
			};

			img.onerror = err => {
				console.error('Failed to render SVG to image', err);
			};

			img.src = dataUrl;
		}
	}

}
