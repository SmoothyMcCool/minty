package tom.api.services.ewm;

public class ResourceRef {
	public String uri; // rdf:resource
	public String title; // dcterms:title or foaf:name

	public ResourceRef() {
	}

	public ResourceRef(String uri, String title) {
		this.uri = uri;
		this.title = title;
	}

	public ResourceRef(ResourceRef ref) {
		this.uri = ref.uri;
		this.title = ref.title;
	}

	@Override
	public String toString() {
		return (title != null ? title : uri);
	}
}
