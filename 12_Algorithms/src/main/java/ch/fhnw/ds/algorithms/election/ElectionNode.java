package ch.fhnw.ds.algorithms.election;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import ch.fhnw.ds.algorithms.election.ElectionTest.Reset;
import ch.fhnw.ds.algorithms.election.ElectionTest.Start;
import ch.fhnw.ds.algorithms.election.ElectionTest.Token;

public class ElectionNode extends AbstractActor {
	private final int id;

	private ActorRef next;
	private ActorRef initiator;
	private int master = Integer.MIN_VALUE;
	public static int COUNTER = 0;
	public ElectionNode(int id) { this.id = id; }

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(ActorRef.class, actor -> {
				next = actor;
			})
			.match(Start.class, value -> {
			    Thread.sleep(100l);
				if(master >= 0) {
					System.out.println("start is no longer possible " + getSelf());
				} else {
					initiator = getSender();
					master = id;
					next.tell(new Token(master), getSelf());
				}
			})
			.match(Token.class, token -> {
			    System.out.println("Node " + this.id + " received token " + getSender().toString());
			    Thread.sleep(100 * (this.id + 1));
			    synchronized(ElectionNode.class) {
			        COUNTER++;			        
			    }
				if(token.value > master) {
					master = token.value;
					next.tell(new Token(master), getSelf());
				} else if(token.value == master) {
					System.out.println("hurray, I got elected " + getSelf());
					next.tell(new Reset(id), getSelf());
				}
			})
			.match(Reset.class, token -> {
				master = Integer.MIN_VALUE;
				if(token.value == id) {
					initiator.tell("its me " + id, getSelf());
				} else {
				    if(initiator != null) {
				        initiator.tell("its him" + id, getSelf());				        
				    }
					next.tell(token, getSelf());
				}
			})
			.matchAny(msg -> {
				System.out.println("UnHandled Message Received");
				unhandled(msg);
			})
			.build();
	}

}