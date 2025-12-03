import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MarkdownModule } from 'ngx-markdown';


@Component({
	selector: 'minty-workflows-pug-help',
	imports: [CommonModule, RouterLink, RouterLinkActive, MarkdownModule],
	templateUrl: 'workflows-pug-help.component.html',
	styleUrl: 'workflows-pug-help.component.css'
})
export class WorkflowsPugHelpComponent {

	packetExampleCode = `
\`\`\`pug
.card
	.card-header.bg-primary.text-white
		h5.mb-0 Id: #{Id}
	.card-body
		//- Text array
		if Text && Text.length
		h6.mt-0 text
		ul.list-group
			each txt in Text
				li.list-group-item.list-group-item-action !{txt}

		//- Data array
		if Data && Data.length
			h6.mt-4 data
			each d in Data
				.card.mb-3
					.card-body
						each val, key in d
							.row
								.col-4.fw-bold #{key}:
								.col-8 #{Helpers.jsonToString(val)}
		else
			p.text-muted No data available.
`;

	outputExampleCode = `
\`\`\`pug
doctype html
html(lang="en")
	head
		meta(charset="UTF-8")
		meta(name="viewport" content="width=device-width, initial-scale=1")
		title Result
		//- Bootstrap 5 CSS (fallback to local copy if CDN fails)
		link(rel="stylesheet"
				 href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
				 onerror="this.onerror=null;this.href='Minty/bootstrap.min.css';")

	body
		.container.my-5
			//- The TOC will be inserted here
			h2#toc-title.mb-4 Table of Contents
			ul#toc.list-group.mb-4

			//- The sections will be appended after the TOC
			#sections

		script#snippets-data(type="application/json") !{Helpers.jsonToString(results["HTML Formatter"])}

		//- ------------------------------------------------------------------
		//- Client-side logic to build a nice page
		//- ------------------------------------------------------------------
		script.
			//--- Slug helper ----------------------------------------------------
			function slugify(text) {
				return text
					.toString()
					.normalize('NFKD')
					.replace(/[\u0300-\u036f]/g, '')
					.toLowerCase()
					.trim()
					.replace(/[^\w\s-]/g, '')
					.replace(/\s+/g, '-')
					.replace(/-+/g, '-');
			}

			//--- Build TOC & sections --------------------------------------------
			document.addEventListener('DOMContentLoaded', () => {
				const data = JSON.parse(document.getElementById('snippets-data').textContent);
				const toc = document.getElementById('toc');
				const sections = document.getElementById('sections');

				data.forEach((snippet) => {
					const slug = slugify(snippet.id);

					// ----- TOC item -------------------------------------------------
					const li = document.createElement('li');
					li.className = 'list-group-item';
					const a = document.createElement('a');
					a.href = \`#\${slug}\`;
					a.textContent = snippet.id;
					li.appendChild(a);
					toc.appendChild(li);

					// ----- Section ---------------------------------------------------
					const section = document.createElement('section');
					section.id = slug;
					section.className = 'mb-5';

					const h2 = document.createElement('h2');
					h2.className = 'mb-3';
					h2.textContent = snippet.id;
					section.appendChild(h2);

					// Render the first (and only) HTML string
					const div = document.createElement('div');
					div.innerHTML = snippet.text[0];
					section.appendChild(div);

					// Optional prettified JSON
					if (snippet.data && snippet.data[0]) {
						const pre = document.createElement('pre');
						pre.className = 'mt-3 text-muted';
						const code = document.createElement('code');
						code.textContent = JSON.stringify(snippet.data[0], null, 2);
						pre.appendChild(code);
						section.appendChild(pre);
					}

					sections.appendChild(section);
				});
			});

		//- Bootstrap 5 JS (fallback to local copy if CDN fails)
		script(src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js"
					 onerror="this.onerror=null;this.src='Minty/bootstrap.bundle.min.js';")

`;

}
