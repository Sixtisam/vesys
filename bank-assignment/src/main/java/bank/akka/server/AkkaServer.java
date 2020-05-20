package bank.akka.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import bank.BankDriver2.UpdateHandler;
import bank.commands.AccountChangedAnswer;
import bank.commands.BankAnswer;
import bank.commands.BankAnswer.BankExceptionAnswer;
import bank.commands.BankCommand;
import bank.local.Bank;

public class AkkaServer implements UpdateHandler {
    private ActorSystem system;
    private Bank bank;
    private ActorRef serverActor;
    private static List<ActorRef> clients = new ArrayList<>();

    public static void main(String[] args) throws IOException, TimeoutException {
        new AkkaServer().start();
        System.in.read();
    }

    public AkkaServer() {
    }

    public void start() {
        bank = new Bank();
        bank.registerUpdateHandler(this);

        Config config = ConfigFactory.load().getConfig("BankServer");
        system = ActorSystem.create("Bank", config);

        serverActor = system.actorOf(Props.create(ServerActor.class, bank), "BankServer");
        System.out.println("Akka system started");
    }

    public void accountChanged(String id) throws IOException {
        AccountChangedAnswer answer = new AccountChangedAnswer(id);
        clients.forEach(c -> c.tell(answer, serverActor));
    }

    public static class ServerActor extends AbstractActor {

        private Bank bank;

        public ServerActor(Bank bank) {
            this.bank = bank;
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(ActorRef.class, this::registerBankClient)
                    .match(BankCommand.class, this::handleCommand)
                    .matchAny(msg -> {
                        System.out.println("Unhandled Message Received");
                        unhandled(msg);
                    })
                    .build();
        }

        private void registerBankClient(ActorRef ref) {
            clients.add(ref);
        }

        private void handleCommand(BankCommand<?> command) {
            System.out.println("Received bank command " + command.getClass().getName());
            BankAnswer<?> answer = processCommand(command);
            getSender().tell(answer, getSelf());
        }

        public BankAnswer<?> processCommand(BankCommand<?> command) {
            try {
                return command.execute(bank);
            } catch (Exception e) {
                return new BankExceptionAnswer(e);
            }
        }
    }
}
