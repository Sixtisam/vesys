package ch.fhnw.ds.akka.sample.remote;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;

public class PrintClient {

	@SuppressWarnings("serial")
	public static void main(String[] args) throws Exception {
		Config config = ConfigFactory.parseString("akka.remote.artery.canonical.port=0")
				.withFallback(ConfigFactory.load().getConfig("PrintConfig"));
	      
		ActorSystem as = ActorSystem.create("PrintApplication", config);
		ActorSelection actor = as.actorSelection("akka://PrintApplication@127.0.0.1:25520/user/PrintServer");
		System.out.println(actor);
		
//		Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
//		Future<ActorRef> res = actor.resolveOne(timeout);
//		ActorRef ref = Await.result(res, timeout.duration());
//		System.out.println(ref);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line = in.readLine();
		while (line != null && !"".equals(line)) {
			if (line.equals("obj")) {
				actor.tell(new Serializable() {}, ActorRef.noSender());
			} else {
				actor.tell(line, ActorRef.noSender());
			}
			line = in.readLine();
		}
		as.terminate();
	}

}
