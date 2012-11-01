import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.proto.*;
import jade.domain.*;

@SuppressWarnings("serial")
public class TourGuideAgent extends Agent {
	// Agent IDs
	private AID aidCurator = new AID("Curator", AID.ISLOCALNAME);
		
	protected void setup() {
		System.out.println("Tour Guide: begin operation");
		
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
		
		addBehaviour(new TickerBehaviour(this, 5000) {
			protected void onTick() {
				System.out.println("Tour Guide: active");
			}
		});
	}
	
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
