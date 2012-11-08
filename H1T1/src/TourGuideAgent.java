import java.util.Iterator;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.proto.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;

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

	/**
	 * Prints hello message, awaits for requests and prints text message every 5 s.
	 * CyclicBehaviour waits for tour requests and then contacts Curator agent using
	 * CuratorInitiator class.
	 */
	protected void setup() {
		/*
		 * Hello message
		 */
		System.out.println(this.getAID().getLocalName() + ": begin operation");

		// Register the tour guide service in the yellow pages.
		DFAgentDescription dfd = new DFAgentDescription(); 
		dfd.setName(getAID()); 
		ServiceDescription sd = new ServiceDescription(); 
		sd.setType("tour-guide"); 
		sd.setName("Tour Guide"); 
		dfd.addServices(sd);
		try { 
			DFService.register(this, dfd);
			System.out.println(this.getAID().getLocalName() + " Registered");
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Build the description used as template for the subscription
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription templateSd = new ServiceDescription();
		templateSd.setType("curator");
		SearchConstraints sc = new SearchConstraints();
		// We want to receive 10 results at most
		sc.setMaxResults(new Long(10));		

		addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc)) {
			protected void handleInform(ACLMessage inform) {
				try {
					DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
					if (results.length > 0) {
						for (int i = 0; i < results.length; ++i) {
							DFAgentDescription dfd = results[i];
							AID provider = dfd.getName();
							Iterator<?> it = dfd.getAllServices();
			  				while (it.hasNext()) {
			  					ServiceDescription sd = (ServiceDescription) it.next();
			  					if (sd.getType().equals("curator")) {
			  					System.out.println(myAgent.getLocalName() + ": notified about " + provider.getName());
			  					}
			  				}
						}
					}	
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
			}
		} );

		/*
		 * CyclicBehaviour implemented as inner class. Uses block() in order
		 * to save CPU.
		 */
		addBehaviour(new CyclicBehaviour(this) {
			public void action() {
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) {
					System.out.println(myAgent.getAID().getLocalName() + ": request from "+msg.getSender().getName());

					ACLMessage msgCurator = new ACLMessage(ACLMessage.REQUEST);
					msgCurator.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					msgCurator.setContent("request-tour-guide");

					/*
					 * The Tour Guide looks for the registered Curators.
					 */
					DFAgentDescription template = new DFAgentDescription(); 
					ServiceDescription sd = new ServiceDescription(); 
					sd.setType("curator"); 
					template.addServices(sd);
					try { 
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						for (int i = 0; i < result.length; ++i) {
							msgCurator.addReceiver(result[i].getName()); 
						}
					}   catch (FIPAException fe) {
						fe.printStackTrace();
					}

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
				System.out.println(myAgent.getAID().getLocalName() + ": active");
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
			System.out.println(myAgent.getAID().getLocalName() + ": curator failure ("+failure.getContent()+")");
			ACLMessage msg = profilerMessage.createReply();
			msg.setPerformative(ACLMessage.FAILURE);
			msg.setContent("curator-connection-failure");
			myAgent.send(msg);
		}

		protected void handleInform(ACLMessage inform) {
			System.out.println(myAgent.getAID().getLocalName() + ": response from Curator ("+inform.getContent()+")");
			ACLMessage msg = profilerMessage.createReply();
			msg.setPerformative(ACLMessage.INFORM);
			msg.setContent(inform.getContent());
			myAgent.send(msg);
		}
	}
}