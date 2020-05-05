package bank.graphql.server;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import bank.local.Bank;

public class GraphQLBank extends Bank {

    public Collection<Account> getAccounts() {	// XXX ich würde wohl als Resultattyp Collection<Bank.Account> nehmen und nicht Collection<bank.local.Bank.Account>
        return accounts.values().stream()
                .filter(Account::isActive)
                .collect(Collectors.toList());
    }
    // XXX diese Methode könnte man auch mit Hilfe von getAccountNumbers realisieren, und das könnte man dann auch im QueryResolver machen.
    //     d.h es bräuchte dann diese Klasse GraphQLBank nicht. Vorteil wäre, dass man in der Basisklasse das Feld accounts nicht protected deklarieren muss.
    //     Die folgende Methode könnte so auch in der Klasse GraphQLQueryResolver stehen. Und weil im GraphQLQueryResolver mit der LocalBank gearbeitet wird
    //     erübrigt sich auch das Fangen der IOException.
	//	    
	//	    public List<Bank.Account> getAccounts() {
	//	        return getAccountNumbers().stream().map(id -> {
	//				try {
	//					return this.getAccount(id);
	//				} catch (IOException e) {
	//					throw new IllegalStateException(e);
	//				}
	//			}).collect(Collectors.toList());
	//	    }
	//	   
}
