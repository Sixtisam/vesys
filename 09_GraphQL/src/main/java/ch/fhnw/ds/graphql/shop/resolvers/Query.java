package ch.fhnw.ds.graphql.shop.resolvers;

import java.util.Collection;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.fhnw.ds.graphql.shop.models.Product;
import ch.fhnw.ds.graphql.shop.repositories.ShopRepository;
import graphql.kickstart.tools.GraphQLQueryResolver;

@Component
public class Query implements GraphQLQueryResolver {

	@Autowired
	private ShopRepository shopRepository;

	public Collection<Product> products() {
		return shopRepository.getAllProducts();
	}

	public Optional<Product> product(String id) {
		return shopRepository.getProductById(id);
	}
}
