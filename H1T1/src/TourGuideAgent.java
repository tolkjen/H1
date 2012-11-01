import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.proto.*;
import jade.domain.*;

/**
 * Tour Guide agent class. Uses CyclicBehaviour, TickerBehaviour, AcheiveREInitiator.
 * 
 * @author tolkjen
 *
 */
@SuppressWarnings("serial")
public class TourGuideAgent extends Agent {
	/*
	 * Curator agent identifier.
	 */
	private AID aidCurator = new AID("Curator", AID.ISLOCALNAME);
		
	/**
	 * Prints hello message, awaits for requests and prints text message every 5 s.
	 * CyclicBehaviour waits for tour requests and then contacts Curator agent using
	 * CuratorInitiator class.
	 */
	protected void setup() {
		/*
		 * Hello message
		 */
		System.out.println("Tour Guide: begin operation");
		
		/*
		 * CyclicBehaviour implemented as inner class. Uses block() in order
		 * to save CPU.
		 */
		addBehaviour(new CyclicBehaviour(this) {
			public void action() {
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) {
					System.out.println("Tour Guide: request from "+msg.getSender().getName());
					
					ACLMessage msgCurator = new ACLMessage(ACLMessage.REQUEST);
					msgCurator.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					msgCurator.setContent("request-tour-guide");
					msgCurator.addReceiver(aidCurator);
					
					addBehaviour(new CuratorInitiator(myAgent, msgCurator, msg));
				} else {
					block();
				}
			}
		});
		
		/*
		 * Repetitive behavior which prints text every 5 s.
		 */
		addBehaviour(new TickerBehaviour(this, 5000) {
			protected void onTick() {
				System.out.println("Tour Guide: active");
			}
		});
	}
	
	/**
	 * Works as a talker between Tour Guide agent and Curator Agent. It sends
	 * information back to Profiler agent about success or failure in communication
	 * with Curator agent.
	 * 
	 * @author tolkjen
	 *
	 */
	private class CuratorInitiator extends AchieveREInitiator {
		private ACLMessage profilerMessage;
		
		public CuratorInitiator(Agent a, ACLMessage msg, ACLMessage msgp) {
			super(a, msg);
			profilerMessage = msgp;
		}
		
		protected void handleFailure(ACLMessage failure) {
			System.out.println("Tour Guide: curator failure ("+failure.getContent()+")");
			ACLMessage msg = profilerMessage.createReply();
			msg.setPerformative(ACLMessage.FAILURE);
			msg.setContent("curator-connection-failure");
			myAgent.send(msg);
		}
		
		protected void handleInform(ACLMessage inform) {
			System.out.println("Tour Guide: response from Curator ("+inform.getContent()+")");
			ACLMessage msg = profilerMessage.createReply();
			msg.setPerformative(ACLMessage.INFORM);
			msg.setContent(inform.getContent());
			myAgent.send(msg);
		}
	}
}
