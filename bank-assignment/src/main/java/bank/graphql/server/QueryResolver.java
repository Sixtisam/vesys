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
    
//  XXX Anstelle der Klasse AccountDTO könnte man hier auch die Klasse Account direkt verwenden (aber nicht beides, d.h. 
//      Die GraphQL Implementierung stellt sicher, dass pro GraphQL Datentyp nur eine Java-KLasse verwendet wird.
//      Da aber die Klasse Account die nötigen Methoden enthält kann ohne Probleme diese direkt zurückgegeben werden.
//      Die Methoden sehen dann wie folgt aus:
//
//    public List<? extends Account> accounts() {
//        return new ArrayList<>(bank.getAccounts());
//    }
//
//    public Account account(String number) {
//        try {
//            return bank.getAccount(number);
//        } catch (Exception e) {
//            throw new ApplicationException(e);
//        }
//    }
}
