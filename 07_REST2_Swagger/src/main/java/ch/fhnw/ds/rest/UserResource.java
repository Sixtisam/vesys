package ch.fhnw.ds.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.servers.Server;

@Singleton
@Path("/users")
@OpenAPIDefinition(
		info = @Info(
			title="User Management",
			description = "Service to manage users",
			version = "2020.03"
		),
		servers = @Server(url = "http://localhost:8080")
)
public class UserResource {
    private static Map<Integer, User> DB = new HashMap<>(); 

    @GET
    @Produces({"application/json", "application/xml"})
    @Operation(
    		summary = "Get all users",
    		description = "Returns a list of all users",
    		responses = {
    				@ApiResponse(responseCode = "200", 
    					description = "Successful operation", 
    					content = @Content(schema = @Schema(implementation = Users.class))
    				)
    		}
    )
    public Users getAllUsers() {
        Users users = new Users();
        users.setUsers(DB.values().stream().filter(u -> u != null).collect(Collectors.toList()));
        return users;
    }

    @POST
    @Consumes({"application/json", "application/xml"})
    @Operation(
    		summary = "Creates a new user",
    		description = "Creates a new user",
    		requestBody = @RequestBody(
    	    				description = "Data of the user to be created. All fields must be provided. The ID field is ignored.",
    	    				content = @Content(schema = @Schema(implementation = User.class))
    	    		),
    		responses = {
    				@ApiResponse(responseCode = "201", description = "Successful creation of a new user.", 
    						headers = @Header(name = "Location", description = "The location header contains the url of the created resource")
    				),
    				@ApiResponse(responseCode = "400", description = "Not all mandatory fields have been provided.")
    		}
    )	
    public Response createUser(@Context UriInfo uriInfo, User user) throws URISyntaxException 
    {
        if(user.getFirstName() == null || user.getLastName() == null) {
            return Response.status(400).entity("Please provide all mandatory fields in the provided document.").build();
        }
		user.setId(DB.values().size() + 1);
        DB.put(user.getId(), user);

		URI location = uriInfo.getAbsolutePathBuilder().path("" + user.getId()).build();
		return Response.created(location).build();
    }
 
    @GET
    @Path("{id}")
    @Produces({"application/json", "application/xml"})
    @Operation(
    		summary = "Find user by ID",
    		description = "Returns a single user",
    		responses = {
    				@ApiResponse(responseCode = "200", description = "Successful operation", content =
       						@Content(schema = @Schema(implementation = User.class))
    				),
    				@ApiResponse(responseCode = "404", description = "User not found"),
    				@ApiResponse(responseCode = "410", description = "User has been deleted")
    		}
    )	
    public Response getUserById(@PathParam("id") int id) throws URISyntaxException {
        User user = DB.get(id);
        if(user == null) {
            return Response.status(DB.containsKey(id) ? 410 : 404).build();
        }
        return Response
                .status(200)
                .entity(user)
                .contentLocation(new URI("api/user/"+id)).build();
    }
 
    @PUT
    @Path("/{id}")
    @Consumes({"application/json", "application/xml"})
    @Produces({"application/json", "application/xml"})
    @Operation(
    		requestBody = @RequestBody(
    				description = "The new version of the user. Only fields which are available are overwritten, i.e. missing fields are not changed.",
    				content = @Content(schema = @Schema(implementation = User.class))
    		),
    		summary = "Update the user using its ID",
    		description = "Updates a single user",
    		responses = {
    				@ApiResponse(responseCode = "200", description = "Successful operation", content =
       						@Content(schema = @Schema(implementation = User.class))
    				),
    				@ApiResponse(responseCode = "404", description = "User not found"),
    				@ApiResponse(responseCode = "410", description = "User has been deleted"),
    				@ApiResponse(responseCode = "412", description = "ID in path and ID in the provided resource do not match")
    		}
    )	
    public Response updateUser(@PathParam("id") int id, User user) throws URISyntaxException 
    {
        User temp = DB.get(id);
        if(user == null) {
            return Response.status(DB.containsKey(id) ? 410 : 404).build();
        }
        if(user.getId() != 0 && user.getId() != id) {
        	return Response.status(412, "id does not meet path id").build();
        }
        
        if(user.getFirstName() != null) temp.setFirstName(user.getFirstName());
        if(user.getLastName() != null) temp.setLastName(user.getLastName());
        DB.put(temp.getId(), temp);
        return Response.status(200).entity(temp).build();
    }
 
    @DELETE
    @Path("/{id}")
    @Operation(
    		summary = "Deletes the user using its ID",
    		description = "Deletes a single user",
    		responses = {
    				@ApiResponse(responseCode = "200", description = "Successful operation"),
    				@ApiResponse(responseCode = "404", description = "User not found"),
    				@ApiResponse(responseCode = "410", description = "User has already been deleted")
    		}
    )	
    public Response deleteUser(@PathParam("id") int id) throws URISyntaxException {
        User user = DB.get(id);
        if(user != null) {
            DB.put(user.getId(), null);
            return Response.status(200).build();
        } else if(DB.containsKey(id)) {
        	return Response.status(410).build();
        } else {
        	return Response.status(404).build();
        }
    }
     
    static {
		User user1 = new User(1, "Peter", "Müller");
		User user2 = new User(2, "Dominik", "Gruntz");
		DB.put(user1.getId(), user1);
		DB.put(user2.getId(), user2);
	}
}