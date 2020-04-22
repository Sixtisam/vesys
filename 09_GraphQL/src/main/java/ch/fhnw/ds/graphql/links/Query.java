package ch.fhnw.ds.graphql.links;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import graphql.kickstart.tools.GraphQLQueryResolver;

@Component
public class Query implements GraphQLQueryResolver {

	@Autowired
	private LinkRepository linkRepository;

	public Collection<Link> allLinks() {
		return linkRepository.getAllLinks();
	}

}
