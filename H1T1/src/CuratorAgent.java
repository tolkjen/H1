import jade.core.Agent;
import jade.lang.acl.*;
import java.util.*;
import jade.proto.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;

/**
 * Curator Agent class. Uses AchieveREResponder behaviors. 
 * 
 * @author tolkjen
 *
 */
@SuppressWarnings("serial")
public class CuratorAgent extends Agent {
	/**
	 * Adds three behaviors - one listening for tour requests from Tour Guide agent,
	 * second waiting for details requests from Profiler agent.
	 */
	protected void setup() {
		
		/*
		 * Figure out an acceptable price for this curator. Either from arguments
		 * or as a random number.
		 */
		long price;
		Object[] args = getArguments();
		if (args != null) {
			price = Long.parseLong((String) args[0]);
		} else {
			Random r = new Random();
			price = (long) (r.nextDouble() * 1000L);
		}
		System.out.println(getLocalName() + ": begin operation");
		
		/*
		 * Add a behavior which does a job of responding to dutch auction
		 * initiator. Use strategy object, which given new offers by initiator,
		 * updates acceptable price for curator.
		 */
		MessageTemplate template = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION),
				MessageTemplate.MatchPerformative(ACLMessage.CFP) );
		addBehaviour(new DutchResponder(this, template, new CuratorStrategy(price)));

		/*
		 * Implementing responding behavior for Tour Guide agent requests.
		 */
		MessageTemplate mt = MessageTemplate.and(AchieveREResponder.createMessageTemplate(
				FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchContent("request-tour-guide")); 
		addBehaviour( new AchieveREResponder(this, mt) { 
			protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
				System.out.println(myAgent.getAID().getLocalName() + ": request from "+request.getSender().getName()+". Action is "+request.getContent());
				ACLMessage agree = request.createReply();
				agree.setPerformative(ACLMessage.AGREE);
				return agree;
			}

			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) { 
				System.out.println(myAgent.getAID().getLocalName() + ": sending data to "+request.getSender().getName());
				ACLMessage informDone = request.createReply(); 
				informDone.setPerformative(ACLMessage.INFORM); 
				informDone.setContent("tour-data"); 
				return informDone; 
			} 
		});

		/*
		 * Implementing responding behavior for Profiler agent requests.
		 */
		MessageTemplate mt2 = MessageTemplate.and(AchieveREResponder.createMessageTemplate(
				FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchContent("request-tour-details")); 
		addBehaviour( new AchieveREResponder(this, mt2) { 
			protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
				System.out.println(myAgent.getAID().getLocalName() + ": request from "+request.getSender().getName()+". Action is "+request.getContent());
				ACLMessage agree = request.createReply();
				agree.setPerformative(ACLMessage.AGREE);
				return agree;
			}

			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) { 
				System.out.println(myAgent.getAID().getLocalName() + ": sending data to "+request.getSender().getName());
				ACLMessage informDone = request.createReply(); 
				informDone.setPerformative(ACLMessage.INFORM); 
				informDone.setContent("tour-details"); 
				return informDone; 
			} 
		});

		// Register the Curator service in the yellow pages.
		DFAgentDescription dfd = new DFAgentDescription(); 
		dfd.setName(getAID()); 
		ServiceDescription sd = new ServiceDescription(); 
		sd.setType("curator"); 
		sd.setName("Curator"); 
		dfd.addServices(sd);
		try { 
			DFService.register(this, dfd);
			System.out.println(this.getAID().getLocalName() + ": registered");
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	protected void takeDown() { 
		System.out.println(getAID().getLocalName() + ": terminating...");
	}
	
	/**
	 * This class handles responding to dutch auction messages.
	 */
	private class DutchResponder extends ContractNetResponder {
		
		private CuratorStrategy strategy;

		public DutchResponder(Agent a, MessageTemplate mt, CuratorStrategy s) {
			super(a, mt);
			strategy = s;
		}
		
		@Override
		protected ACLMessage handleCfp(ACLMessage cfp) {
			//System.out.println(getLocalName()+": CFP received from "+cfp.getSender().getLocalName());
			
			long proposal = Long.parseLong(cfp.getContent());
			strategy.addOffer(proposal);
			
			System.out.println(myAgent.getLocalName()+": strategy-based price = "+strategy.getAcceptablePrice());
			
			if (proposal <= strategy.getAcceptablePrice()) {
				// We provide a proposal
				System.out.println(getLocalName()+": proposing "+proposal);
				ACLMessage propose = cfp.createReply();
				propose.setPerformative(ACLMessage.PROPOSE);
				return propose;
			}
			else {
				// We refuse to provide a proposal
				System.out.println(getLocalName()+": refusing");
				ACLMessage refuse = cfp.createReply();
				refuse.setPerformative(ACLMessage.REFUSE);
				return refuse;
			}
		}
		
		@Override
		protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,ACLMessage accept) {
			System.out.println(getLocalName()+": proposal accepted");
			ACLMessage inform = accept.createReply();
			inform.setPerformative(ACLMessage.INFORM);
			return inform;	
		}

		protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
			System.out.println(getLocalName()+": proposal rejected");
		}
	}
	
	/**
	 * This class gathers information about previous offers given by initiator
	 * and calculates the acceptable price for curator. Formula of new price
	 * is to be implemented in addOffer() method.
	 */
	private class CuratorStrategy {
		//private long startingPrice;
		private long currentPrice;
		private Vector<Long> offers;
		
		public CuratorStrategy(long price) {
			//startingPrice = price;
			currentPrice = price;
			offers = new Vector<Long>();
		}
		
		public long getAcceptablePrice() {
			return currentPrice;
		}
		
		public void addOffer(long price) {
			offers.add(price);
			
			/*
			 * New price. In this case, regardless of offer history.
			 */
			currentPrice += 100;
		}
	}
}