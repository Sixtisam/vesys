package bank.graphql.server;

import java.util.List;
import java.util.stream.Collectors;

import graphql.kickstart.tools.GraphQLQueryResolver;

public class QueryResolver implements GraphQLQueryResolver {

    private final GraphQLBank bank;

    public QueryResolver(GraphQLBank bank) {
        this.bank = bank;
    }

    public List<AccountDTO> accounts() {
        return bank.getAccounts().stream()
                .map(AccountDTO::from)
                .collect(Collectors.toList());
    }

    public AccountDTO account(String number) {
        try {
            return AccountDTO.from(bank.getAccount(number));
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
    }
}
