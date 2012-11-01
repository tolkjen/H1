import jade.core.Agent;
import jade.lang.acl.*;
import jade.proto.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;

@SuppressWarnings("serial")
public class CuratorAgent extends Agent {
	protected void setup() {
		System.out.println("Curator: begin operation");
		
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
