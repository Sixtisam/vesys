package ch.fhnw.ds.graphql.shop.resolvers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.fhnw.ds.graphql.shop.models.Customer;
import ch.fhnw.ds.graphql.shop.models.Rating;
import ch.fhnw.ds.graphql.shop.repositories.ShopRepository;
import graphql.kickstart.tools.GraphQLResolver;

@Component
public class CustomerResolver implements GraphQLResolver<Customer> {

	@Autowired
	private ShopRepository shopRepository;

	// retuns the ratings of this customer
	public List<Rating> ratings(Customer c) {
		return shopRepository.getRatingsForCustomer(c);
	}
}
