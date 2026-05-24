
import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MarkdownModule } from 'ngx-markdown';
import { MermaidClipboardDirective } from '../../conversation/component/mermaid-clipboard.directive';
import { ProjectNode } from '../../model/project/project-node';

@Component({
	selector: 'minty-project-node-viewer',
	imports: [FormsModule, MarkdownModule, MermaidClipboardDirective],
	templateUrl: 'project-node-viewer.component.html',
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => NodeViewerComponent),
			multi: true
		}
	]
})
export class NodeViewerComponent implements ControlValueAccessor {

	@Input() edit: boolean = false;

	node: ProjectNode | undefined = undefined;
	displayText: string | undefined

	onChange = (_: any) => { };
	onTouched: () => void = () => { };

	private debounceTimer: ReturnType<typeof setTimeout> | undefined = undefined;

	writeValue(value: ProjectNode | undefined): void {
		this.node = undefined;
		if (!value) {
			return;
		}
		this.node = JSON.parse(JSON.stringify(value));
		let lang = '';
		if (this.node?.path) {
			lang = this.getMarkdownLang(this.node?.path);
		}
		if (this.node?.fileType === 'code' || this.node?.fileType === 'json' || this.node?.fileType === 'markdown') {
			this.displayText = lang + '\n' + this.node.content + '\n```';
		} else {
			this.displayText = this.node?.content;
		}
	}
	registerOnChange(fn: any): void {
		this.onChange = fn;
	}
	registerOnTouched(fn: any): void {
		this.onTouched = fn;
	}
	setDisabledState(_isDisabled: boolean): void {
		// Nah.
	}

	onTextChange(text: string) {
		if (this.node) {
			this.node.content = text;
			this.onTouched();

			if (this.debounceTimer) {
				clearTimeout(this.debounceTimer);
			}

			this.debounceTimer = setTimeout(() => {
				this.onChange(this.node);
			}, 100);
		}
	}

	static markdownLangLookup: Record<string, string> = {
		// Web Development
		'ts': '```typescript\n',
		'tsx': '```typescript\n',
		'js': '```javascript\n',
		'jsx': '```javascript\n',
		'html': '```html\n',
		'htm': '```html\n',
		'css': '```css\n',
		'scss': '```scss\n',
		'sass': '```sass\n',

		// Data & Configuration
		'json': '```json\n',
		'yaml': '```yaml\n',
		'yml': '```yaml\n',
		'xml': '```xml\n',
		'csv': '```csv\n',
		'toml': '```toml\n',
		'ini': '```ini\n',

		// Backend & Systems
		'py': '```python\n',
		'rb': '```ruby\n',
		'php': '```php\n',
		'go': '```go\n',
		'rs': '```rust\n',
		'java': '```java\n',
		'kt': '```kotlin\n',
		'cs': '```csharp\n',
		'cpp': '```cpp\n',
		'c': '```c\n',

		// Shell & Documentation
		'sh': '```bash\n',
		'bash': '```bash\n',
		'ps1': '```powershell\n',
		'sql': '```sql\n',
		'md': '```markdown\n',
		'dockerfile': '```dockerfile\n'
	};

	getMarkdownLang(fileNameOrExtension: string): string {
		const cleanExt = fileNameOrExtension
			.split('.')
			.pop()!
			.toLowerCase()
			.trim();

		return NodeViewerComponent.markdownLangLookup[cleanExt] || 'code';
	}
}