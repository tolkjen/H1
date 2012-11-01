import jade.core.Agent;
import jade.lang.acl.*;
import jade.proto.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;

/**
 * Curator Agent class. Uses AchieveREResponder behaviors. 
 * 
 * @author tolkjen
 *
 */
@SuppressWarnings("serial")
public class CuratorAgent extends Agent {
	/**
	 * Adds two behaviors - one listening for tour requests from Tour Guide agent,
	 * second waiting for details requests from Profiler agent.
	 */
	protected void setup() {
		System.out.println("Curator: begin operation");
		
		/*
		 * Implementing responding behavior for Tour Guide agent requests.
		 */
		MessageTemplate mt = MessageTemplate.and(AchieveREResponder.createMessageTemplate(
				FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchContent("request-tour-guide")); 
		addBehaviour( new AchieveREResponder(this, mt) { 
			protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
				System.out.println("Curator: request from "+request.getSender().getName()+". Action is "+request.getContent());
				ACLMessage agree = request.createReply();
				agree.setPerformative(ACLMessage.AGREE);
				return agree;
			}
			
			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) { 
				System.out.println("Curator: sending data to "+request.getSender().getName());
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
				System.out.println("Curator: request from "+request.getSender().getName()+". Action is "+request.getContent());
				ACLMessage agree = request.createReply();
				agree.setPerformative(ACLMessage.AGREE);
				return agree;
			}
			
			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) { 
				System.out.println("Curator: sending data to "+request.getSender().getName());
				ACLMessage informDone = request.createReply(); 
				informDone.setPerformative(ACLMessage.INFORM); 
				informDone.setContent("tour-details"); 
				return informDone; 
			} 
		});
	}
}
