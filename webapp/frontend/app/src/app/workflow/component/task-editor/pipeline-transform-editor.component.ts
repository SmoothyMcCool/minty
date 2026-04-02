import { CdkDragDrop, moveItemInArray } from "@angular/cdk/drag-drop";
import { CommonModule } from "@angular/common";
import { Component, forwardRef } from "@angular/core";
import { FormsModule, NG_VALUE_ACCESSOR, ControlValueAccessor } from "@angular/forms";
import { PipelineDefinition, PipelineOperation } from "src/app/model/workflow/pipeline-transform";

@Component({
	selector: 'minty-pipeline-transform-editor',
	templateUrl: 'pipeline-transform-editor.component.html',
	imports: [CommonModule, FormsModule],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => PipelineTransformEditorComponent),
			multi: true
		}
	]
})
export class PipelineTransformEditorComponent implements ControlValueAccessor {

	onChange = (_: any) => { };
	onTouched: any = () => { };

	pipeline: PipelineDefinition | null = null;

	showDescriptions: boolean[] = [];
	jsonErrors: boolean[] = [];

	get operations(): { op: string, configType: string, description: string }[] {
		return PipelineTransformEditorComponent.Operations;
	}

	requiresConfig(op: PipelineOperation): boolean {
		const meta = this.getOperation(op.name);
		if (!meta) {
			return false;
		}
		return meta.configType !== 'none';
	}

	addOperation(index?: number) {

		if (!this.pipeline) {
			this.pipeline = { operations: [] };
		}

		const op: PipelineOperation = {
			name: '',
			configuration: null
		};

		if (index === undefined) {
			this.pipeline.operations.push(op);
		} else {
			this.pipeline.operations.splice(index, 0, op);
		}

		this.onTouched();
		this.onChange(JSON.stringify(this.pipeline));
	}

	removeOperation(index: number) {
		this.pipeline?.operations.splice(index, 1);
		this.onTouched();
		this.onChange(JSON.stringify(this.pipeline));
	}

	moveUp(index: number) {
		if (!this.pipeline || index === 0) {
			return;
		}
		const ops = this.pipeline.operations;
		[ops[index - 1], ops[index]] = [ops[index], ops[index - 1]];
		this.onTouched();
		this.onChange(JSON.stringify(this.pipeline));
	}

	moveDown(index: number) {
		if (!this.pipeline) {
			return;
		}

		const ops = this.pipeline.operations;
		if (index >= ops.length - 1) {
			return;
		}

		[ops[index + 1], ops[index]] = [ops[index], ops[index + 1]];

		this.onTouched();
		this.onChange(JSON.stringify(this.pipeline));
	}

	drop(event: CdkDragDrop<any>) {
		if (!this.pipeline) {
			return;
		}

		moveItemInArray(
			this.pipeline.operations,
			event.previousIndex,
			event.currentIndex
		);

		this.onTouched();
		this.onChange(JSON.stringify(this.pipeline));
	}

	toggleDescription(index: number) {
		this.showDescriptions[index] = !this.showDescriptions[index];
	}

	getOperation(name: string) {
		return this.operations.find(o => o.op === name);
	}

	getConfigJson(op: PipelineOperation): string {
		if (!op.configuration) {
			return '';
		}
		if (Array.isArray(op.configuration)) {
			return JSON.stringify(op.configuration, null, 2);
		}
		else if (typeof op.configuration === 'string') {
			return op.configuration;
		}
		return JSON.stringify(op.configuration, null, 2);
	}



	updateConfig(op: PipelineOperation, value: string, index: number) {
		if (!this.requiresConfig(op)) {
			this.jsonErrors[index] = false;
			return;
		}

		try {
			const configType = this.operations.find(operation => operation.op.localeCompare(op.name) === 0).configType
			if (configType === 'text') {
				op.configuration = value;
			} else {
				const parsed = JSON.parse(value);
				if (typeof parsed !== 'string') {
					op.configuration = parsed;
					this.jsonErrors[index] = false;
				}
				else {
					op.configuration = value;
					this.jsonErrors[index] = true;
				}
			}
		}
		catch {
			this.jsonErrors[index] = true;
		}

		this.onTouched();
		this.onChange(JSON.stringify(this.pipeline));

	}

	onOperationChange(op: PipelineOperation) {
		op.configuration = null;
		this.onTouched();
		this.onChange(JSON.stringify(this.pipeline));
	}

	writeValue(_obj: string): void {
		this.pipeline = JSON.parse(_obj);
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

	public static readonly Operations = [
	{
	op: 'SpEL Expression',
	configType: 'text',
	description:
`
Applies a Spring Expression Language (SpEL) expression to the packet.

The expression is evaluated with the packet as the root object, allowing
direct access to the packet's text and data fields.

For convenience, expressions like:

  data.foo
  text.bar

are automatically rewritten to:

  data[0].foo
  text[0].bar

This allows easy access to fields in the first data row or text entry.

Expressions may either:
• read values
• modify fields
• assign new values

If the expression contains an assignment, the packet will be modified.

Examples

Set a field:
  data.status = 'processed'

Copy a value:
  data.fullName = data.firstName + ' ' + data.lastName

Modify text:
  text = text[0].toUpperCase()

Access the first data row explicitly:
  data[0].score = data[0].score * 2

Configuration
<SpEL expression>
`
	},
	{
		op: 'Flatten Lists',
		configType: 'map',
		description:
`
Flattens the packet so that text and data contain only one entry.

Text entries are joined using the configured separator.
Data rows are merged into a single object. If multiple rows contain the
same field, the precedence ('first' or 'last') determines which value wins.

e.g.
{ text: ['Apples', 'Bananas'], data: [{ a: 1, b: 2 }, { b: 3, c: 4 }] }

separator: '\\n'
precedence: 'last'

result:
{ text: ['Apples\\nBananas'], data: [{ a: 1, b: 3, c: 4 }] }
`
	},
	{
		op: 'Keep Fields',
		configType: 'list',
		description:
`
Keeps only the specified fields in each data row. All other fields are removed.

Configuration: list of field names to keep.

e.g.
data: [{ a: 1, b: 2, c: 3 }]
config: ['a', 'c']

result:
data: [{ a: 1, c: 3 }]
`
	},
	{
		op: 'Remove Empty Records',
		configType: 'none',
		description:
`
Removes empty values from the packet.

Text entries are removed if they are null, empty, or whitespace.

Data fields are removed if their value is:
• null
• an empty or whitespace-only string

e.g.
{ text: ['Hello', '', 'World'], data: [{ a: 1, b: '' }, { c: null, d: 4 }] }

result:
{ text: ['Hello', 'World'], data: [{ a: 1 }, { d: 4 }] }
`
	},
	{
		op: 'Remove Fields',
		configType: 'list',
		description:
`
Removes the specified fields from every data row.

Configuration: list of field names to remove.

e.g.
data: [{ a: 1, b: 2, c: 3 }]
config: ['b']

result:
data: [{ a: 1, c: 3 }]
`
	},
	{
		op: 'Remove Null Fields',
		configType: 'none',
		description:
`
Removes fields whose value is null.

Text entries that are null or blank are also removed.

e.g.
{ text: ['Hello', null, ''], data: [{ a: 1, b: null }, { c: null, d: 4 }] }

result:
{ text: ['Hello'], data: [{ a: 1 }, { d: 4 }] }
`
	},
	{
		op: 'Rename Fields',
		configType: 'map',
		description:
`
Renames fields in each data row using a mapping.

Configuration: map of { oldFieldName : newFieldName }.

e.g.
data: [{ first_name: 'Tom', last_name: 'Smith' }]
config: { first_name: 'firstName', last_name: 'lastName' }

result:
data: [{ firstName: 'Tom', lastName: 'Smith' }]
`
	},
	{
		op: 'Set Field',
		configType: 'map',
		description:
`
Sets a field to the same value in every data row.

Configuration:
{
  field: string
  value: any
}

Existing values will be overwritten.

e.g.
data: [{ a: 1 }, { a: 2 }]
config: { field: 'status', value: 'processed' }

result:
data: [{ a: 1, status: 'processed' }, { a: 2, status: 'processed' }]
`
	}
].sort((a, b) => a.op.localeCompare(b.op));

}