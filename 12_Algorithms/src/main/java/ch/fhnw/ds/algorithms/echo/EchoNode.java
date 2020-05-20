package ch.fhnw.ds.algorithms.echo;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import ch.fhnw.ds.algorithms.echo.EchoTest.Start;
import ch.fhnw.ds.algorithms.echo.EchoTest.Token;

public class EchoNode extends AbstractActor {
    private final Set<ActorRef> neighbours = new HashSet<>();
    private ActorRef parent;
    private int counter = 0; // number of received tokens
    private int nodeCounter = 0;
    private Deque<String> spanningTree = new LinkedList<>();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ActorRef.class, actor -> {
                    neighbours.add(actor);
                })
                .match(Start.class, value -> {
                    parent = getSender();
                    neighbours.forEach(a -> a.tell(new Token(0), getSelf()));
                })
                .match(Token.class, msg -> {
                    counter++;
                    if (parent == null) {
                        parent = getSender();
                        System.out.printf("Actor %s got informed by %s%n", getSelf(), getSender());
                        neighbours.stream().filter(a -> a != parent).forEach(a -> a.tell(msg, getSelf()));
                    } else {
                        // müsste nicht unbedingt im else sein, da wenn parent == null => msg.counter == 0 ist
                        nodeCounter += msg.counter;
                        spanningTree.addAll(msg.spanningTree);
                    }

                    if (counter == neighbours.size()) {
                        spanningTree.addFirst(getSelf().toString());
                        parent.tell(new Token(nodeCounter + 1, spanningTree), getSelf());
                    }
                })
                .matchAny(msg -> {
                    System.out.printf("%s UnHandled Message Received: %s%n", getSelf(), msg);
                    unhandled(msg);
                })
                .build();
    }

}