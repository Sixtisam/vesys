package ch.fhnw.ds.graphql.shop.resolvers;

import java.util.List;
import java.util.OptionalDouble;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.fhnw.ds.graphql.shop.models.Product;
import ch.fhnw.ds.graphql.shop.models.Rating;
import ch.fhnw.ds.graphql.shop.repositories.ShopRepository;
import graphql.kickstart.tools.GraphQLResolver;

@Component
public class ProductResolver implements GraphQLResolver<Product> {

	@Autowired
	private ShopRepository shopRepository;

	public List<Rating> ratings(Product p) {
		return shopRepository.getRatingsForProduct(p);
	}

	public OptionalDouble averageRatingScore(Product p) {
		return shopRepository.getRatingsForProduct(p).stream().mapToInt(r -> r.getScore()).average();
	}
}
