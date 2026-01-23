// data-editor.component.ts
import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter, forwardRef, AfterViewInit } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Packet } from 'src/app/model/workflow/packet';

@Component({
	selector: 'minty-packet-editor',
	templateUrl: './packet-editor.component.html',
	imports: [CommonModule, FormsModule],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => PacketEditorComponent),
			multi: true
		}
	]
})
export class PacketEditorComponent implements ControlValueAccessor {
	packets: Packet[] = [];

	onChange = (_: any) => { };
	onTouched: any = () => {};

	textMode = false;
	valid = true;

	emitChange() {
		this.onTouched();
		this.onChange(JSON.stringify(this.packets));
	}

	addPacket() {
		this.packets.push({
			id: '',
			text: [],
			data: [],
		});
		this.emitChange();
	}

	removePacket(index: number) {
		if (index != -1) {
			this.packets.splice(index, 1);
		}
		this.emitChange();
	}

	addText(packet: Packet) {
		packet.text.push('');
		this.emitChange();
	}

	removeText(packet: Packet, index: number) {
		packet.text.splice(index, 1);
		this.emitChange();
	}

	trackByIndex(index: number, item: any) {
		return index;
	}

	addData(packet: Packet) {
		packet.data.push({ value: 0 });
		this.emitChange();
	}

	removeData(packet: Packet, index: number) {
		packet.data.splice(index, 1);
		this.emitChange();
	}

	onTextChange(packet: Packet, index: number, value: string) {
		packet.text[index] = value;
		this.emitChange();
	}

	onValueChange(packet: Packet, index: number, value: any) {
		try {
			const parsed = JSON.parse(value);
			packet.data[index] = parsed;
			this.valid = true;
		} catch(error) {
			this.valid = false;
		}

		this.emitChange();
	}

	onIdChange(packet: Packet, id: string) {
		packet.id = id;
		this.emitChange();
	}

	packetsAsString(): string {
		return JSON.stringify(this.packets, undefined, 2);
	}

	packetAsString(packet: Packet) {
		return JSON.stringify(packet, undefined, 2);
	}

	packetTextChanged($event) {
		try {
			this.packets = this.packets = JSON.parse($event) as Packet[];
			this.valid = true;
			this.emitChange();
		} catch(error) {
			this.valid = false;
		}
	}

	writeValue(obj: any): void {
		this.packets = [];
		if (!obj) {
			return;
		}

		if (typeof obj ===  'string') {
			obj = JSON.parse(obj);
			if (!Array.isArray(obj)) {
				obj = [obj];
			}
			this.packets = obj;
		} else if (Array.isArray(obj)) {
			this.packets = obj;
		} else {
			this.packets = [obj];
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
}
