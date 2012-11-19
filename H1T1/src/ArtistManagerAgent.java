import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.proto.SubscriptionInitiator;
import jade.util.leap.Iterator;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;

import java.util.Date;
import java.util.Vector;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;

/**
 * ArtistManager class.
 * 
 * ArtistManager runs an auction in a museum. It gets a command line parameter
 * telling what is the name of a Curator which is going to participate in the
 * auction (for simplicity). 
 * 
 * ArtistManager is run in a museum container, which is non-main container. First
 * it registers itself in DF so that main Curator knows about container operation.
 * Then it subscribes for a Curator-Clone registration. As it occures, 
 * ArtistManager begins a dutch auction.
 * 
 * @author tolkjen
 *
 */
@SuppressWarnings("serial")
public class ArtistManagerAgent extends Agent {
	protected void setup() {
		Object[] args = getArguments();
		if (args == null || args.length != 1) {
			System.out.println(getLocalName() + ": no curator specified. Terminate!");
			doDelete();
			return;
		} 
		
		System.out.println(getLocalName() + ": begin operation");
		
		registerAgent();
		subscribeForCurator((String) args[0]);
	}
	
	private void registerAgent() {
		// Register the tour guide service in the yellow pages.
		DFAgentDescription dfd = new DFAgentDescription(); 
		dfd.setName(getAID()); 
		ServiceDescription sd = new ServiceDescription(); 
		sd.setType("container"); 
		sd.setName(getLocalName()); 
		dfd.addServices(sd);
		try { 
			DFService.register(this, dfd);
			System.out.println(getLocalName() + ": registered");
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	private void subscribeForCurator(final String curatorName) {
		DFAgentDescription template = new DFAgentDescription(); 
		template.setName(new AID(curatorName, AID.ISLOCALNAME));
        ServiceDescription sd = new ServiceDescription(); 
        sd.setType("curator"); 
        template.addServices(sd);
        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults(new Long(2));
		
		addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc)) {
			protected void handleInform(ACLMessage inform) {
				System.out.println(getLocalName() + ": curator subscription noticed...");
				try {
					DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
					if (results.length > 0) {
						addDutchBehaviour(curatorName);
					}	
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
			}
		} );
	}
	
	private void addDutchBehaviour(String curatorName) {
		System.out.println("Starting auction");
		
		// Call For Proposal message
		ACLMessage msgCFP = new ACLMessage(ACLMessage.CFP);
		msgCFP.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
		msgCFP.setReplyByDate(new Date(System.currentTimeMillis() + 3000));
		msgCFP.addReceiver(new AID(curatorName, AID.ISLOCALNAME));
				
		// Launch behavior
		addBehaviour(new DutchInitiator(this, msgCFP, new ManagerStrategy()));
	}
	
	protected void takeDown() { 
		System.out.println(getAID().getLocalName() + ": terminating...");
	}
	
	/**
	 * Runs dutch auction.
	 * 
	 * @author tolkjen
	 *
	 */
	private class DutchInitiator extends ContractNetInitiator {
		
		private int roundNumber;
		private ManagerStrategy strategy;
		private Vector<AID> responders;
		
		public DutchInitiator(Agent a, ACLMessage cfp, ManagerStrategy s) {
			super(a, cfp);
			
			strategy = s;
			responders = new Vector<AID>();
			
			Iterator it = cfp.getAllReceiver();
			while (it.hasNext()) {
				responders.add((AID) it.next());
			}
			
			startAuction();
		}
		
		private void startAuction() {
			strategy.reset();
			roundNumber = 1;
			//System.out.println(getLocalName()+": starting price = "+strategy.getPrice());
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
				//System.out.println(getLocalName()+": auction terminates (after "+roundNumber+" rounds)");
				//myAgent.doDelete();
				reset();
				startAuction();
			} else {
				strategy.nextRound();
				roundNumber++;
				//System.out.println(getLocalName()+": new price = "+strategy.getPrice());
				newIteration(generateCFPs());
			}
		}
	}
	
	/**
	 * This class is used to get a price of a product in each auction round.
	 * 
	 * @author tolkjen
	 *
	 */
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
			currentPrice = Math.round(currentPrice * 0.75);
			if (currentPrice <= lowestPrice) {
				currentPrice = lowestPrice;
			}
		}
		
		public boolean isFinalRound() {
			return (currentPrice == lowestPrice);
		}
		
		public void reset() {
			currentPrice = highestPrice;
		}
	}
}
