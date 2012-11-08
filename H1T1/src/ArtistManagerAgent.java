import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.util.leap.Iterator;
import jade.domain.FIPANames;

import java.util.Date;
import java.util.Vector;

@SuppressWarnings("serial")
public class ArtistManagerAgent extends Agent {

	protected void setup() {
		System.out.println(getAID().getLocalName() + ": begin operation");
		
		// Exit if there are no arguments given
		Object[] args = getArguments();
		if (args == null || args.length == 0) {
			System.out.println(getAID().getLocalName() + ": no curators specified. Terminate!");
			doDelete();
		}
		
		// Call For Proposal message
		ACLMessage msgCFP = new ACLMessage(ACLMessage.CFP);
		msgCFP.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
		msgCFP.setReplyByDate(new Date(System.currentTimeMillis() + 3000));
		for (int i=0; i<args.length; i++) {
			msgCFP.addReceiver(new AID((String) args[i], AID.ISLOCALNAME));
		}
		
		// Launch behavior
		addBehaviour(new DutchInitiator(this, msgCFP, new ManagerStrategy()));
	}
	
	protected void takeDown() { 
		System.out.println(getAID().getLocalName() + ": terminating...");
	}
	
	private class DutchInitiator extends ContractNetInitiator {
		
		private int roundNumber;
		private ManagerStrategy strategy;
		private Vector<AID> responders;
		
		public DutchInitiator(Agent a, ACLMessage cfp, ManagerStrategy s) {
			super(a, cfp);
			
			strategy = s;
			responders = new Vector<AID>();
			roundNumber = 1;
			
			Iterator it = cfp.getAllReceiver();
			while (it.hasNext()) {
				responders.add((AID) it.next());
			}
			
			System.out.println(getLocalName()+": starting price = "+strategy.getPrice());
		}
		
		private Vector<ACLMessage> generateCFPs() {
			Vector<ACLMessage> result = new Vector<ACLMessage>();
			for (int i=0; i<responders.size(); i++) {
				ACLMessage msg = new ACLMessage(ACLMessage.CFP);
				msg.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
				msg.setReplyByDate(new Date(System.currentTimeMillis() + 3000));
				msg.setContent(Long.toString(strategy.getPrice()));
				msg.addReceiver(responders.get(i));
				result.add( msg );
			}
			return result;
		}

		@SuppressWarnings({ "rawtypes" })
		protected Vector prepareCfps(ACLMessage cfp) {
			return generateCFPs();
		}
		
		protected void handlePropose(ACLMessage propose, Vector v) {
			System.out.println(getAID().getLocalName() + ": agent "+propose.getSender().getLocalName()+" sent a proposal");
		}
		
		protected void handleRefuse(ACLMessage refuse) {
			System.out.println(getAID().getLocalName() + ": agent "+refuse.getSender().getLocalName()+" refused");
		}
		
		protected void handleAllResponses(Vector responses, Vector acceptances) {
			boolean proposalReceived = false;
			
			for (int i=0; i<responses.size(); i++) {
				ACLMessage msg = (ACLMessage) responses.elementAt(i);
				
				if (msg.getPerformative() == ACLMessage.PROPOSE) {
					ACLMessage reply = msg.createReply();
					if (!proposalReceived) {
						reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						acceptances.addElement(reply);
						proposalReceived = true;
					} else {
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						acceptances.addElement(reply);
					}
				} 
			}
			
			if (!proposalReceived) {
				handleNextRound();
			} else {
				System.out.println(getLocalName()+": auction fisnishes (after "+roundNumber+" rounds)");
			}
		}
		
		private void handleNextRound() {
			if (strategy.isFinalRound()) {
				System.out.println(getLocalName()+": auction terminates (after "+roundNumber+" rounds)");
				myAgent.doDelete();
			} else {
				strategy.nextRound();
				roundNumber++;
				System.out.println(getLocalName()+": new price = "+strategy.getPrice());
				newIteration(generateCFPs());
			}
		}
	}
	
	private class ManagerStrategy {
		private final long highestPrice = 1000;
		private final long lowestPrice = 100;
		
		private long currentPrice;
		
		public ManagerStrategy() {
			currentPrice = highestPrice;
		}
		
		public long getPrice() {
			return currentPrice;
		}
		
		public void nextRound() {
			currentPrice = Math.round(currentPrice * 0.5);
			if (currentPrice <= lowestPrice) {
				currentPrice = lowestPrice;
			}
		}
		
		public boolean isFinalRound() {
			return (currentPrice == lowestPrice);
		}
	}
}
