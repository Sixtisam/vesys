package bank.graphql.server;

import java.util.Collection;
import java.util.stream.Collectors;

import bank.local.Bank;

public class GraphQLBank extends Bank {

    public Collection<Account> getAccounts() {
        return accounts.values().stream()
                .filter(Account::isActive)
                .collect(Collectors.toList());
    }
}
