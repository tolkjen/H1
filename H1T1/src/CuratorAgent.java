import jade.core.AID;

import jade.core.Agent;
import jade.lang.acl.*;

import java.util.*;
import jade.proto.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.mobility.*;
import jade.domain.FIPANames;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.domain.JADEAgentManagement.WhereIsAgentAction;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import jade.lang.acl.*;
import jade.content.*;
import jade.content.onto.basic.*;
import jade.content.lang.*;
import jade.content.lang.sl.*;
import jade.core.*;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.mobility.*;
import jade.domain.JADEAgentManagement.*;
import jade.gui.*;

/**
 * Curator Agent class. 
 * 
 * Curator is an agent who tries to buy something on an auction. Because of race
 * conditions related to launching main container and museum containers at the 
 * same time, it subscribes itself for registration of any museum container.
 * 
 * Once a museum container is up and running, it uses GetContainerInitiator to
 * find out locations of museum containers. Upon finding them, it clones itself
 * into them. After successful clone action, Curator-clone registers itself in
 * DF and runs DutchResponder behaviour. As ArtistManager of that museum finds out
 * about registered Curato-clone, it starts auction. 
 * 
 * @author tolkjen
 *
 */
@SuppressWarnings("serial")
public class CuratorAgent extends Agent {
	/**
	 * Setup routine. Sets up some content options and subscribes for container
	 * registration.
	 */
	protected void setup() {
		System.out.println(getLocalName() + ": begin operation");
			
		getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);
		getContentManager().registerOntology(MobilityOntology.getInstance());
		
		subscribeForContainers();
	}
	
	/**
	 * Subscribes for container registration. When containers come up, it 
	 * launches findCountainers() to clone itself into them.
	 */
	private void subscribeForContainers() {
		System.out.println(getLocalName() + ": waiting for containers...");
		
		DFAgentDescription template = new DFAgentDescription(); 
        ServiceDescription sd = new ServiceDescription(); 
        sd.setType("container"); 
        template.addServices(sd);
        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults(new Long(2));
		
		addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc)) {
			protected void handleInform(ACLMessage inform) {
				System.out.println(getLocalName() + ": container subscription noticed...");
				try {
					DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
					if (results.length > 0) {
						findContainers();
					}	
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
			}
		} );
	}
	
	/**
	 * Adds behaviour which will find out information regarding container 
	 * locations and clone Curator into them.
	 */
	private void findContainers() {
		System.out.println(getLocalName() + ": fetching container information...");
		
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.addReceiver(getAMS());
		request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
		request.setOntology(MobilityOntology.NAME);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

		Action action = new Action();
		action.setActor(getAMS());
		action.setAction(new QueryPlatformLocationsAction());
		try {
			getContentManager().fillContent(request, action);
		} catch (Exception e) {
			e.printStackTrace();
		} 
			
		addBehaviour(new GetContainerInitiator(this, request));
	}
	
	/**
	 * Registers Curator in DF. Used after a successful clone action to tell
	 * ArtistManager to begin auction. Before registration, ArtistManager waits
	 * for Curator-clone.
	 */
	private void registerAgent() {	
		// Register the tour guide service in the yellow pages.
		DFAgentDescription dfd = new DFAgentDescription(); 
		dfd.setName(getAID()); 
		ServiceDescription sd = new ServiceDescription(); 
		sd.setType("curator"); 
		sd.setName(getLocalName()); 
		dfd.addServices(sd);
		try { 
			DFService.register(this, dfd);
			System.out.println(getLocalName() + ": registered");
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	/**
	 * After a cloning operation was performed, Curator adds DutchResponder 
	 * behaviour and registers itself in DF.
	 */
	protected void afterClone() {
		System.out.println("Agent has cloned! ");
		
		// Participate in the dutch auction
		Random r = new Random();
		Long price = (long) r.nextDouble() * 1000L;
	
		MessageTemplate template = MessageTemplate.and(
			MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION),
			MessageTemplate.MatchPerformative(ACLMessage.CFP) );
		addBehaviour(new DutchResponder(this, template, new CuratorStrategy(price)));
		
		// Register itself so that auction manager can start auction
		registerAgent();
	}
	
	protected void takeDown() { 
		System.out.println(getLocalName() + ": terminating...");
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
			
			/*
			 * Accepting proposal, time to go back to Main Container!
			 */
			
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
	 * This class is used to find locations of certain containers. Once it finds
	 * a location, it clones itself there.
	 * 
	 * @author tolkjen
	 *
	 */
	private class GetContainerInitiator extends AchieveREInitiator {
		public GetContainerInitiator(Agent a, ACLMessage msg) {
			super(a, msg);
		}

		protected void handleInform(ACLMessage inform) {
			System.out.println(getLocalName() + ": cloning agent...");
			try {
				Result results = (Result) myAgent.getContentManager().extractContent(inform);
				for (int i=0; i<results.getItems().size(); i++) {
					Location l = (Location) results.getItems().get(i);
					if (l.getName().equals("HMContainer")) {
						myAgent.doClone(l, "HMCurator");
					}
					if (l.getName().equals("GalileoContainer")) {
						myAgent.doClone(l, "GalileoCurator");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
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
			currentPrice += 200;
		}
	}
}