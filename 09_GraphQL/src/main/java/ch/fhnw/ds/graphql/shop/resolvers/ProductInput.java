package ch.fhnw.ds.graphql.shop.resolvers;

public class ProductInput {
	private String title;
	private String description;
	private String imageUrl;
	
	public ProductInput() {
	}

	public ProductInput(String title, String description, String imageUrl) {
		super();
		this.title = title;
		this.description = description;
		this.imageUrl = imageUrl;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
}
