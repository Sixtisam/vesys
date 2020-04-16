package bank.http.rest.server;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import bank.Account;
import bank.InactiveException;
import bank.OverdrawException;
import bank.http.rest.AccountDTO;
import bank.http.rest.BalanceDTO;
import bank.http.rest.TransferDTO;
import bank.local.Bank;

@Singleton
@Path("/bank")
public class BankResource {

    private final Bank bank = new Bank();

    @Context
    UriInfo resourceUriInfo;

    protected String buildETag(Bank.Account acc) {
        return "\"" + acc.hashCode() + "\"";
    }

    /**
     * Returns all links to all accounts numbers in the header and in the body a
     * list of all account numbers.
     * 
     * @return
     * @throws IOException
     */
    @GET
    @Path("/accounts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccountNumbers() throws IOException {
        UriBuilder builder = resourceUriInfo.getAbsolutePathBuilder().path("/{accountNumber}");
        ResponseBuilder resBuilder = Response.ok();

        List<String> numbers = new LinkedList<>();
        bank.getAccountNumbers().forEach(acc -> {
            numbers.add(acc);
            resBuilder.link(builder.build(acc).toString(), "self");
        });
        return resBuilder.entity(numbers).build();
    }

    @POST
    @Path("/accounts")
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response createAccount(AccountDTO account) throws IOException {
    	// XXX hier haben sie sich sicher überlegt, ob man ein separates DTO definieren soll mit dem man nur den Namen übergeben kann.
    	//     Jetzt werden einfach die restlichen Felder, falls sie vorhanden sind, ignoriert. Variante wäre ein weiteres DTO analog zum BalanceDTO.
        String number = bank.createAccount(account.owner);

        URI uri = resourceUriInfo.getAbsolutePathBuilder().path("/{accountNumber}").build(number);
        return Response.created(uri).link(uri.toString(), "self").build();
    }

    @GET
    @Path("/accounts/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccount(@PathParam("accountNumber") String accountNumber, @Context Request request)
            throws IOException {
        Bank.Account acc = bank.getAccount(accountNumber);
        if (acc == null) {
            throw new NotFoundException();
        }
        ResponseBuilder builder = request.evaluatePreconditions(new EntityTag(buildETag(acc)));
        if (builder != null) {
            return builder.build();
        } else {
            return Response.ok(acc).tag(buildETag(acc)).build();
        }
    }

    /**
     * not used but still implemented
     * // XXX wenn Sie es nicht impelemntieren wäre es trotzdem verfügbar, d.h. es wird ann einfach der Header des GET Requests zurückgeschickt.
     * //     In Ihrem Fall sind diese beiden Header jedoch nicht identisch, d.h. das GET liefert die Information GONE nicht aus.
     */
    @HEAD
    @Path("/accounts/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response accountExists(@PathParam("accountNumber") String accountNumber) throws IOException {
        Account acc = bank.getAccount(accountNumber);
        if (acc == null) {
            throw new NotFoundException();
        } else if (acc.isActive()) {
            return Response.ok().build();
        } else {
            return Response.status(Status.GONE).build();
        }
    }

    @PUT
    @Path("/accounts/{accountNumber}")
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response updateBalance(@PathParam("accountNumber") String accountNumber, @Context Request request,
            BalanceDTO balanceDto)
            throws IOException, IllegalArgumentException, InactiveException, OverdrawException {
        Bank.Account serverAcc = bank.getAccount(accountNumber);
        if (serverAcc == null) {
            throw new NotFoundException();
        }
        ResponseBuilder builder = request.evaluatePreconditions(new EntityTag(buildETag(serverAcc)));
        if (builder == null) {
            double diff = balanceDto.getBalance() - serverAcc.getBalance();
            if (diff < 0) {
                serverAcc.withdraw(-diff);
            } else {
                serverAcc.deposit(diff);
            }
            return Response.ok().build();
        } else {
            return builder.build();
        }
    }

    @DELETE
    @Path("/accounts/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response closeAccount(@PathParam("accountNumber") String accountNumber) throws IOException {
        Account acc = bank.getAccount(accountNumber);
        if (acc == null) {
            throw new NotFoundException();
        }
        if (bank.closeAccount(accountNumber)) {
            return Response.ok().build();
        } else {
            return Response.notModified().build();
        }
    }

    @POST
    @Path("/transfers")
    @Consumes(MediaType.APPLICATION_JSON)
    public void transferTo(TransferDTO transfer) throws IOException, InactiveException, OverdrawException {
        Account from = bank.getAccount(transfer.getFrom());
        Account to = bank.getAccount(transfer.getTo());
        bank.transfer(from, to, transfer.getAmount());
    }
}
