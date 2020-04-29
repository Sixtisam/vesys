package bank.graphql.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import bank.BankDriver;
import bank.InactiveException;
import bank.OverdrawException;

public class GraphQLDriver implements BankDriver {
    private HttpClient client;
    private HttpRequest.Builder requestBuilder;
    private Bank bank;

    public GraphQLDriver() {
    }

    @Override
    public void connect(String[] args) throws IOException {
        bank = new Bank();
        client = HttpClient.newBuilder().build();
        requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/graphql"))
                .header("Accept", "application/json");
    }

    @Override
    public void disconnect() throws IOException {
        bank = null;
        requestBuilder = null;
        client = null;
    }

    @Override
    public Bank getBank() {
        return bank;
    }

    public static class GraphQLRequest {
        public String query;
        public Map<String, Object> variables;

        public GraphQLRequest(String query, Map<String, Object> variables) {
            super();
            this.query = query;
            this.variables = variables;
        }
    }

    protected JsonNode sendRequest(String query) throws Exception {
        return sendRequest(query, null);
    }

    protected JsonNode sendRequest(String query, Map<String, Object> variables) throws Exception {
        ObjectMapper om = new ObjectMapper();
        String requestBody = om.writeValueAsString(new GraphQLRequest(query, variables));

        HttpRequest request = requestBuilder
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(BodyPublishers.ofString(requestBody, Charset.forName("UTF-8")))
                .build();

        HttpResponse<InputStream> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        JsonNode response = parseResponse(httpResponse.body());

        JsonNode errorNode = response.get("errors");
        if (errorNode == null || errorNode instanceof NullNode) {
            // always returns first attribute in data node (bank application never returns
            // more than one query result)
            return response.get("data").iterator().next();
        } else {
            JsonNode exceptionField = response.findValue("exception");
            if (exceptionField != null) {
                try {
                    Exception ex = (Exception) Class.forName(exceptionField.asText()).getConstructor().newInstance();
                    throw ex;
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IOException("error in response:  " + errorNode.toPrettyString());
            }
        }
    }

    protected JsonNode parseResponse(InputStream jsonStream) throws IOException {
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.readTree(jsonStream);
        return node;
    }

    public class Bank implements bank.Bank {

        @Override
        public String createAccount(String owner) throws IOException {
            try {
                return sendRequest("mutation CreateAccount($owner: String!){ createAccount(owner: $owner)}", Map.of("owner", owner)).asText();
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            try {
                return sendRequest("mutation CloseAccount($number: ID!){ closeAccount(number: $number)}", Map.of("number", number)).asBoolean();
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            try {
                JsonNode resp = sendRequest("{ accounts { number } }");
                Set<String> numbers = new HashSet<>(resp.size());
                Iterator<JsonNode> iter = resp.elements();
                while (iter.hasNext()) {
                    numbers.add(iter.next().get("number").asText());
                }
                return numbers;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public bank.Account getAccount(String number) throws IOException {
            try {
                JsonNode data = sendRequest("query LoadAccount($number: ID!) { account(number: $number) { number owner } }",
                        Map.of("number", number));
                if (data.isNull()) {
                    return null;
                } else {
                    return new Account(data.get("number").asText(), data.get("owner").asText());
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void transfer(bank.Account a, bank.Account b, double amount)
                throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
            try {
                sendRequest("mutation Transfer($from: ID!, $to: ID!, $amount: Float!) { transfer(from: $from, to: $to, amount: $amount) }",
                        Map.of("from", a.getNumber(), "to", b.getNumber(), "amount", amount));
            } catch (InactiveException | OverdrawException | IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }

        }

        public class Account implements bank.Account {
            private final String number;
            private final String owner;

            public Account(String number, String owner) {
                this.number = number;
                this.owner = owner;
            }

            @Override
            public String getNumber() throws IOException {
                return number;
            }

            @Override
            public String getOwner() throws IOException {
                return owner;
            }

            @Override
            public boolean isActive() throws IOException {
                try {
                    return sendRequest("query LoadAccount($number: ID!) { account(number: $number) { active } }", Map.of("number", getNumber()))
                            .get("active")
                            .asBoolean();
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }

            @Override
            public double getBalance() throws IOException {
                try {
                    return sendRequest("query LoadAccount($number: ID!) { account(number: $number) { balance } }", Map.of("number", getNumber()))
                            .get("balance")
                            .asDouble();
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }

            @Override
            public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {
                try {
                    sendRequest("mutation Deposit($number: ID!, $amount: Float!) { deposit(number: $number, amount: $amount) }",
                            Map.of("number", this.getNumber(), "amount", amount));
                } catch (InactiveException | IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }

            @Override
            public void withdraw(double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
                try {
                    sendRequest("mutation Withdraw($number: ID!, $amount: Float!) { withdraw(number: $number, amount: $amount) }",
                            Map.of("number", this.getNumber(), "amount", amount));
                } catch (InactiveException | OverdrawException | IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }

        }
    }

}
