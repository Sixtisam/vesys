package org.openapitools.client;

import org.openapitools.client.api.DefaultApi;
import org.openapitools.client.model.Users;

public class Main {
    public static void main(String[] args) throws Exception {
        DefaultApi api = new DefaultApi();
        Users users = api.getAllUsers();
        users.getUsers().forEach(u -> {
            System.out.printf("User(%d, %s, %s)%n",
                    u.getId(), u.getFirstName(), u.getLastName());
        });
    }
}