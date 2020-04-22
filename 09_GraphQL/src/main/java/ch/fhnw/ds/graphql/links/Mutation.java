package ch.fhnw.ds.graphql.links;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import graphql.kickstart.tools.GraphQLMutationResolver;

@Component
public class Mutation implements GraphQLMutationResolver {

	@Autowired
	private LinkRepository linkRepository;

	public Link createLink(String url, String description) {
		Link link = new Link(url, description); 
		linkRepository.addLink(link);
		return link;
	}

}
