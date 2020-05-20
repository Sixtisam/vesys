package ch.fhnw.ds.akka.hierarchy;

import akka.actor.AbstractActor;

public class B extends AbstractActor {

	@Override
	public Receive createReceive() {
		return receiveBuilder().matchAny(msg -> System.out.printf("%s received msg `%s` from %s%n", getSelf(), msg, getSender())).build();
	}

}